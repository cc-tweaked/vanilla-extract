package cc.tweaked.vanillaextract.core.mappings;

import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.inputs.HashingInputCollector;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.MappingWriter;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MappingTreeView;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Stores mappings in a derivation-addressed cache.
 * <p>
 * We don't need to store mappings - they're relatively cheap to compute, so we don't benefit much from caching.
 * However, when working with Gradle, it's useful to have the mappings available as a file rather than in-memory. For
 * instance, we need the mappings when decompiling Minecraft in order to provide Javadoc. As that runs in a separate
 * process, we need to be able to serialise the mappings - it's much easier to do that with a file
 */
public final class MappingsFileProvider {
    private final Path cache;

    /**
     * Construct a new mappings file provider.
     *
     * @param cache The global cache path.
     */
    public MappingsFileProvider(Path cache) {
        this.cache = cache;
    }

    /**
     * Save the resolved mappings to a file.
     *
     * @param version  The current Minecraft version.
     * @param mappings The mappings to save.
     * @return The location the mappings were saved to.
     * @throws IOException If we failed to save the mappings.
     */
    public FileFingerprint saveMappings(String version, ResolvedMappings mappings) throws IOException {
        var inputs = new HashingInputCollector("Mappings");
        inputs.addInput(mappings);
        var hash = inputs.getDigest();

        var path = cache.resolve(version).resolve("mappings").resolve(hash + ".tiny.gz");
        if (!MoreFiles.exists(path)) {
            var mappingTree = new MemoryMappingTree();
            mappings.accept(mappingTree);

            Files.createDirectories(path.getParent());
            try (var scratch = MoreFiles.scratch(path)) {
                writeMappings(scratch.path(), mappingTree);
                scratch.commit();
            }

            try (var scratch = MoreFiles.scratch(path.resolveSibling(hash + ".log"))) {
                Files.writeString(scratch.path(), inputs.toString());
                scratch.commit();
            }
        }

        return new FileFingerprint(path, hash);
    }

    /**
     * Write mappings to a file.
     * <p>
     * These are stored as a gzipped {@linkplain MappingFormat#TINY_2_FILE tiny v2} file.
     *
     * @param path     The path to write to.
     * @param mappings The mappings to write.
     * @throws IOException If we failed to save the mappings.
     */
    public static void writeMappings(Path path, MappingTreeView mappings) throws IOException {
        try (var writer = MappingWriter.create(
            new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path)), StandardCharsets.UTF_8)),
            MappingFormat.TINY_2_FILE)
        ) {
            mappings.accept(writer);
        }
    }

    /**
     * Read mappings from a file.
     * <p>
     * These are stored as a gzipped {@linkplain MappingFormat#TINY_2_FILE tiny v2} file.
     *
     * @param path    The path to read from.
     * @param visitor The visitor to read mappings into.
     * @throws IOException If we failed to read the mappings.
     */
    public static void readMappings(Path path, MappingVisitor visitor) throws IOException {
        try (var reader = new BufferedReader(
            new InputStreamReader(new GZIPInputStream(Files.newInputStream(path)), StandardCharsets.UTF_8))
        ) {
            MappingReader.read(reader, MappingFormat.TINY_2_FILE, visitor);
        }
    }
}
