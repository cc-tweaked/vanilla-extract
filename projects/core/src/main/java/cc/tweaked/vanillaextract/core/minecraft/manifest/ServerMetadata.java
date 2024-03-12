package cc.tweaked.vanillaextract.core.minecraft.manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Metadata about the fat server jars.
 *
 * @param versions  The list of versions included in this jar.
 * @param libraries The list of libraries included in this jar.
 */
public record ServerMetadata(List<IncludedFile> versions, List<IncludedFile> libraries) {
    private static final Logger LOG = LoggerFactory.getLogger(ServerMetadata.class);

    /**
     * Attempt to parse server metadata from a folder.
     *
     * @param path The path to {@code META-INF}. This will typically be constructed from a zip file system.
     * @return The parsed server metadata.
     * @throws IOException If the version or library information could not be read.
     */
    public static ServerMetadata parse(Path path) throws IOException {
        return new ServerMetadata(
            parseFileList(path.resolve("versions.list")),
            parseFileList(path.resolve("libraries.list"))
        );
    }

    private static List<IncludedFile> parseFileList(Path path) throws IOException {
        List<IncludedFile> lines = new ArrayList<>();
        try (var reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                var parts = line.split("\t");
                if (parts.length != 3) LOG.warn("Cannot parse included file \"{} \" in {}.", line, path);

                lines.add(new IncludedFile(parts[0], parts[1], parts[2]));
            }
        }

        return List.copyOf(lines);
    }

    /**
     * A file included in this JAR.
     *
     * @param sha1 The hash of this file.
     * @param id   The ID of this file. For versions, this is the version name, for libraries this is the maven
     *             coordinate.
     * @param path The path to this file within the jar.
     */
    public record IncludedFile(String sha1, String id, String path) {
    }
}
