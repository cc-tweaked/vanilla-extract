package cc.tweaked.vanillaextract.core.mappings;

import cc.tweaked.vanillaextract.core.MavenArtifact;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.inputs.InputCollector;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.tinyremapper.FileSystemReference;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A mapping provider using Parchment.
 * <p>
 * This adds additional information to {@linkplain MojangMappings Mojang's mappings}, adding parameter names and
 * Javadoc.
 *
 * @param path The path to the Parchment data.
 */
public record ParchmentMappings(Path path) implements MappingProvider {
    /**
     * The Maven group parchment mappings are published under.
     */
    public static final String GROUP = "org.parchmentmc.data";

    @Override
    public ResolvedMappings resolve(Context context) throws IOException {
        var official = MojangMappings.get().resolve(context);
        return new Resolved(official, context.fingerprint().snapshot(path));
    }

    /**
     * Get a Maven artifact coordinate for a parchment data export.
     *
     * @param mcVersion The Minecraft version these mappings were created for.
     * @param version   The mappings version.
     * @return The maven artifact.
     */
    public static MavenArtifact getArtifact(String mcVersion, String version) {
        return new MavenArtifact(GROUP, "parchment-" + mcVersion, version, null, "zip");
    }

    private record Resolved(ResolvedMappings mojang, FileFingerprint parchmentData) implements ResolvedMappings {
        @Override
        public void addInputs(InputCollector collector) {
            collector.addInput(mojang);
            collector.addInput(parchmentData);
        }

        @Override
        public void accept(MappingVisitor visitor) throws IOException {
            mojang().accept(visitor);

            ParchmentData data;
            try (var fs = FileSystemReference.openJar(parchmentData().path())) {
                data = MoreFiles.readJson(fs.getPath("parchment.json"), ParchmentData.class);
            }

            data.visit(visitor, MappingNamespaces.WORKSPACE);
        }
    }
}
