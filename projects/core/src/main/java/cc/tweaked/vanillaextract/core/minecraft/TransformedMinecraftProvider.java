package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.MavenArtifact;
import cc.tweaked.vanillaextract.core.MavenRelease;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.inputs.HashingInputCollector;
import cc.tweaked.vanillaextract.core.mappings.MappingNamespaces;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import cc.tweaked.vanillaextract.core.util.PomWriter;
import cc.tweaked.vanillaextract.core.util.Timing;
import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerClassVisitor;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.TinyUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides a transformed version of Minecraft,
 */
public final class TransformedMinecraftProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TransformedMinecraftProvider.class);

    public static final String GROUP = "net.minecraft";
    public static final String COMMON_MODULE = "minecraft-common";
    public static final String CLIENT_ONLY_MODULE = "minecraft-clientOnly";

    private static final Pattern INVALID_LOCAL_VARIABLE = Pattern.compile("\\$\\$\\d+");

    private final Path mavenPath;

    public TransformedMinecraftProvider(Path mavenPath) {
        this.mavenPath = mavenPath;
    }

    /**
     * A single transformed jar.
     *
     * @param path    The path to the resulting jar.
     * @param release The maven release of this jar.
     */
    public record TransformedJar(Path path, MavenRelease release) {
    }

    /**
     * The transformed jars, suitable to be used in a development environment.
     *
     * @param common     The common jar.
     * @param clientOnly The client-only jar.
     */
    public record TransformedJars(TransformedJar common, TransformedJar clientOnly) {
    }

    public TransformedJars provide(
        String version,
        MinecraftProvider.SplitArtifacts artifacts,
        FileFingerprint mappings,
        List<FileFingerprint> accessWideners,
        boolean refresh
    ) throws IOException {
        // Build up our list of inputs.
        var inputs = new HashingInputCollector("Minecraft " + version);
        // The Minecraft jars (obviously)
        inputs.addInput(artifacts.common().jar());
        inputs.addInput(artifacts.client().jar());
        // Our mappings
        inputs.addInput(mappings);
        // And our access wideners.
        for (var accessWidener : accessWideners) inputs.addInput(accessWidener);

        var hash = inputs.getDigest().substring(0, 16);
        var common = getMavenModule(version, COMMON_MODULE, hash);
        var clientOnly = getMavenModule(version, CLIENT_ONLY_MODULE, hash);

        var commonJar = common.getJarLocation(mavenPath);
        var clientOnlyJar = clientOnly.getJarLocation(mavenPath);

        // Write jars if needed.
        if (refresh || !MoreFiles.exists(commonJar) || !MoreFiles.exists(clientOnlyJar)) {
            transformJars(
                mappings.path(), accessWideners,
                List.of(artifacts.common().jar().path(), artifacts.client().jar().path()),
                List.of(commonJar, clientOnlyJar)
            );
        }

        // Write POMs if needed.
        if (!MoreFiles.exists(common.getPomLocation(mavenPath)) || !MoreFiles.exists(common.getPomLocation(mavenPath))) {
            writeMinecraftPoms(version, artifacts, mavenPath, common, clientOnly);
        }

        // Write a trace of the inputs, for easier debugging.
        var trace = common.getFileLocation(mavenPath, "inputs", "log");
        if (!MoreFiles.exists(trace)) {
            try (var scratch = MoreFiles.scratch(trace)) {
                Files.writeString(scratch.path(), inputs.toString());
                scratch.commit();
            }
        }

        return new TransformedJars(new TransformedJar(commonJar, common), new TransformedJar(clientOnlyJar, clientOnly));
    }

    private static MavenRelease getMavenModule(String version, String module, String hash) {
        return new MavenRelease(GROUP, module, version + "-" + hash);
    }

    /**
     * Deobfuscate and access-transform our jars.
     *
     * @param mappings       The mappings to use.
     * @param accessWideners The access wideners to apply.
     * @param inputJars      The input jars to use.
     * @param outputJars     The paths to write the output jars to. Must be the same length as {@code inputJars}.
     */
    private static void transformJars(
        Path mappings, List<FileFingerprint> accessWideners,
        List<Path> inputJars, List<Path> outputJars
    ) throws IOException {
        if (inputJars.size() != outputJars.size()) throw new IllegalArgumentException("Jars must be the same length");

        // Read all our access wideners, then compute the set of all files we'll need to transform.
        var accessWidener = new AccessWidener();
        {
            var accessWidenerReader = new AccessWidenerReader(accessWidener);
            for (var widener : accessWideners) {
                try (var reader = Files.newBufferedReader(widener.path())) {
                    accessWidenerReader.read(reader);
                }
            }
        }

        var accessWidenedFiles = accessWidener.getTargets().stream()
            .map(x -> x.replace('.', '/'))
            .collect(Collectors.toUnmodifiableSet());

        var remapper = TinyRemapper.newRemapper()
            .withMappings(TinyUtils.createTinyMappingProvider(mappings, MappingNamespaces.OFFICIAL, MappingNamespaces.WORKSPACE))
            .renameInvalidLocals(true)
            .rebuildSourceFilenames(true)
            .invalidLvNamePattern(INVALID_LOCAL_VARIABLE)
            .inferNameFromSameLvIndex(true)
            .build();

        try {
            var tags = new InputTag[inputJars.size()];

            for (int i = 0; i < inputJars.size(); i++) {
                var tag = tags[i] = remapper.createInputTag();
                remapper.readInputs(tag, inputJars.get(i));
            }

            for (int i = 0; i < inputJars.size(); i++) {
                long start = System.nanoTime();
                LOG.info("Remapping {} to {}", inputJars.get(i), outputJars.get(i));
                transformJar(remapper, accessWidener, accessWidenedFiles, tags[i], inputJars.get(i), outputJars.get(i));
                LOG.info("Remapping took {}.", Timing.formatSince(start));
            }
        } finally {
            remapper.finish();
        }
    }

    /**
     * Transform a single jar, deobfuscating and applying access transformers.
     *
     * @param remapper           The {@link TinyRemapper} instance.
     * @param accessWidener      The collection of access wideners.
     * @param accessWidenedFiles The set of files that should be transformed by the access widener.
     * @param inputTag           The input tag for this file.
     * @param input              The path of the input jar.
     * @param output             The path of the output jar.
     */
    private static void transformJar(
        TinyRemapper remapper, AccessWidener accessWidener, Set<String> accessWidenedFiles,
        InputTag inputTag, Path input, Path output
    ) throws IOException {
        Files.createDirectories(output.getParent());

        try (var scratch = MoreFiles.scratchZip(output)) {
            try (var jarWriter = new OutputConsumerPath.Builder(scratch.path()).build()) {
                jarWriter.addNonClassFiles(input);

                remapper.apply((path, bytes) -> {
                    if (accessWidenedFiles.contains(path)) bytes = transformClass(accessWidener, bytes);
                    jarWriter.accept(path, bytes);
                }, inputTag);
            }

            scratch.commit();
        }
    }

    private static byte[] transformClass(AccessWidener accessWidener, byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(AccessWidenerClassVisitor.createClassVisitor(Opcodes.ASM9, writer, accessWidener), 0);
        return writer.toByteArray();
    }

    private static void writeMinecraftPoms(
        String version, MinecraftProvider.SplitArtifacts artifacts, Path mavenPath, MavenRelease common, MavenRelease clientOnly
    ) throws IOException {
        writeMinecraftPom(
            mavenPath, common, "Minecraft " + version + " (common)",
            "Common files for Minecraft " + version + ".",
            artifacts.common().dependencies().stream().map(MavenArtifact::parse)
        );
        writeMinecraftPom(
            mavenPath, clientOnly, "Minecraft " + version + " (client only)",
            "Client-only files for Minecraft " + version + ".",
            Stream.concat(
                artifacts.client().dependencies().stream().map(MavenArtifact::parse),
                Stream.of(MavenArtifact.main(common))
            )
        );
    }

    private static void writeMinecraftPom(
        Path mavenPath, MavenRelease module, String displayName, String description, Stream<MavenArtifact> dependencies
    ) throws IOException {
        var pom = new PomWriter(module.group(), module.module(), module.version());
        pom.setName(displayName);
        pom.setDescription(description);
        pom.setUrl("https://www.minecraft.net/en-us");

        dependencies.forEach(d -> pom.addDependency(d, "compile", false));

        try (var scratch = MoreFiles.scratch(module.getPomLocation(mavenPath))) {
            try (var writer = Files.newBufferedWriter(scratch.path())) {
                pom.write(writer);
            } catch (XMLStreamException e) {
                throw new IOException("Failed to write XML", e);
            }

            scratch.commit();
        }
    }
}
