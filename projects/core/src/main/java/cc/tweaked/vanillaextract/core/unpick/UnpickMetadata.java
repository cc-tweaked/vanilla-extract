package cc.tweaked.vanillaextract.core.unpick;


import cc.tweaked.vanillaextract.core.MavenArtifact;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import com.google.gson.JsonObject;
import net.fabricmc.tinyremapper.FileSystemReference;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Metadata about the unpick definitions in a Yarn mappings file.
 */
public sealed interface UnpickMetadata {
    String METADATA_PATH = "extras/unpick.json";

    /**
     * Unpick V1 metadata
     *
     * @param unpickGroup   The maven group of the tool to use.
     * @param unpickVersion The maven version of the tool to use.
     */
    record V1(String unpickGroup, String unpickVersion) implements UnpickMetadata {
        /**
         * Get the Maven artifact for the {@code unpick-cli} tool.
         *
         * @return The maven artifact for the CLI tool.
         */
        public MavenArtifact getCliTool() {
            return new MavenArtifact(unpickGroup(), "unpick-cli", unpickVersion(), null, null);
        }
    }

    record V2(String namespace, @Nullable String constants) implements UnpickMetadata {
    }

    static UnpickMetadata fromJar(Path jar) throws IOException {
        JsonObject metadata;
        try (var fs = FileSystemReference.openJar(jar)) {
            metadata = MoreFiles.readJson(fs.getPath(METADATA_PATH), JsonObject.class);
        }

        var version = metadata.get("version").getAsInt();
        return switch (version) {
            case 1 -> new V1(metadata.get("unpickGroup").getAsString(), metadata.get("unpickVersion").getAsString());
            case 2 -> {
                var constants = metadata.get("constants");
                yield new V2(metadata.get("namespace").getAsString(), constants == null ? null : constants.getAsString());
            }
            default -> throw new IllegalStateException("Unsupported Unpick version " + version);
        };
    }
}
