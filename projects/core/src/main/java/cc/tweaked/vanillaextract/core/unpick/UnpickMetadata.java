package cc.tweaked.vanillaextract.core.unpick;


import cc.tweaked.vanillaextract.core.MavenArtifact;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import net.fabricmc.tinyremapper.FileSystemReference;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Metadata about the unpick definitions in a Yarn mappings file.
 *
 * @param version       The version of this file format, assumed to be {@code 0}.
 * @param unpickGroup   The maven group of the tool to use.
 * @param unpickVersion The maven version of the tool to use.
 */
public record UnpickMetadata(
    int version,
    String unpickGroup,
    String unpickVersion
) {
    public static final String METADATA_PATH = "extras/unpick.json";

    public static UnpickMetadata fromJar(Path jar) throws IOException {
        try (var fs = FileSystemReference.openJar(jar)) {
            return MoreFiles.readJson(fs.getPath(METADATA_PATH), UnpickMetadata.class);
        }
    }

    /**
     * Get the Maven artifact for the {@code unpick-cli} tool.
     *
     * @return The maven artifact for the CLI tool.
     */
    public MavenArtifact getCliTool() {
        return new MavenArtifact(unpickGroup(), "unpick-cli", unpickVersion(), null, null);
    }
}
