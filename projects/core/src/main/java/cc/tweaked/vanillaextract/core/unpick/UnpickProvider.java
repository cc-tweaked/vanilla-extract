package cc.tweaked.vanillaextract.core.unpick;

import cc.tweaked.vanillaextract.core.mappings.MappingNamespaces;
import cc.tweaked.vanillaextract.core.mappings.MappingsFileProvider;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import daomephsta.unpick.api.ConstantUninliner;
import daomephsta.unpick.api.classresolvers.ClassResolvers;
import daomephsta.unpick.api.classresolvers.IClassResolver;
import daomephsta.unpick.api.constantgroupers.ConstantGroupers;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Writer;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Writer;
import daomephsta.unpick.impl.classresolvers.ChainClassResolver;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.FileSystemReference;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class UnpickProvider implements Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(UnpickProvider.class);
    private static final java.util.logging.Logger JAVA_LOG = java.util.logging.Logger.getLogger(LOG.getName());

    private final List<FileSystemReference> toClose;
    private final ConstantUninliner uninliner;

    public UnpickProvider(Path mappings, Path unpick, List<Path> classpath) throws IOException {
        toClose = new ArrayList<>(classpath.size() + 1);
        List<IClassResolver> classResolvers = new ArrayList<>(classpath.size() + 1);
        for (var classPathEntry : classpath) {
            if (Files.isDirectory(classPathEntry)) {
                classResolvers.add(ClassResolvers.fromDirectory(classPathEntry));
            } else {
                var jarFile = FileSystemReference.openJar(classPathEntry, false);
                toClose.add(jarFile);
                classResolvers.add(ClassResolvers.fromDirectory(jarFile.getPath("/")));
            }
        }
        classResolvers.add(ClassResolvers.classpath(ClassLoader.getSystemClassLoader()));

        var classResolver = new ChainClassResolver(classResolvers.toArray(new IClassResolver[0]));
        uninliner = ConstantUninliner.builder()
            .logger(JAVA_LOG)
            .classResolver(classResolver)
            .grouper(ConstantGroupers.dataDriven()
                .logger(JAVA_LOG)
                .lenient(true)
                .classResolver(classResolver)
                .mappingSource(new StringReader(remapUnpick(mappings, unpick)))
                .build()
            )
            .build();
    }

    public void unpick(Path inputPath, Path outputPath) throws IOException {
        try (var scratch = MoreFiles.scratchZip(outputPath)) {
            List<FileSystemReference> toClose = new ArrayList<>();
            try (
                var outputJar = new ZipArchiveOutputStream(scratch.path());
                var inputJar = ZipFile.builder().setPath(inputPath).get();
            ) {
                var entries = inputJar.getEntries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (entry.isDirectory() || !entry.getName().endsWith(".class")) {
                        outputJar.addRawArchiveEntry(entry, inputJar.getRawInputStream(entry));
                    } else {
                        // Read the old class
                        var node = new ClassNode();
                        try (var is = inputJar.getInputStream(entry)) {
                            new ClassReader(is).accept(node, 0);
                        }

                        // Transform it
                        uninliner.transform(node);

                        // And write the new one
                        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        node.accept(writer);

                        outputJar.putArchiveEntry(entry);
                        outputJar.write(writer.toByteArray());
                        outputJar.closeArchiveEntry();
                    }
                }
            }

            scratch.commit();
        }
    }

    @Override
    public void close() throws IOException {
        for (var ref : toClose) ref.close();
    }

    static String remapUnpick(Path mappings, Path unpick) throws IOException {
        try (var unpickJar = FileSystemReference.openJar(unpick)) {
            // Read official, intermediary and yarn from the unpick jar.
            var mappingTree = new MemoryMappingTree();
            MappingReader.read(unpickJar.getPath("mappings/mappings.tiny"), MappingFormat.TINY_2_FILE, mappingTree);

            // Then read in our current mappings, which includes official and the workspace.
            MappingsFileProvider.readMappings(mappings, new MappingSourceNsSwitch(mappingTree, "official"));

            var metadata = UnpickMetadata.fromJar(unpick);
            var sourceMappings = switch (metadata) {
                case UnpickMetadata.V1 v1 -> "named"; // "named" refers to the Yarn namespace.
                case UnpickMetadata.V2 v2 -> v2.namespace();
            };

            // This gives us a link of the source namespace to our workspace mappings, so we can then remap the unpick file.
            try (var reader = Files.newBufferedReader(unpickJar.getPath("extras/definitions.unpick"))) {
                reader.mark(16);
                var line = reader.readLine();
                reader.reset();

                return switch (line) {
                    case "v2" -> {
                        var writer = new UnpickV2Writer();
                        new UnpickV2Reader(reader).accept(new UnpickRemapperV2(mappingTree, sourceMappings, MappingNamespaces.WORKSPACE, writer));
                        yield writer.getOutput();
                    }
                    case "unpick v3" -> {
                        var writer = new UnpickV3Writer();
                        new UnpickV3Reader(reader).accept(new UnpickRemapperV3(mappingTree, sourceMappings, MappingNamespaces.WORKSPACE, writer));
                        yield writer.getOutput();
                    }

                    default -> throw new IllegalStateException("Unknown unpick format '" + line + "'");
                };
            }
        }
    }

}
