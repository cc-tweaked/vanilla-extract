package cc.tweaked.vanillaextract.core.inputs;

import cc.tweaked.vanillaextract.core.util.MoreDigests;
import cc.tweaked.vanillaextract.core.util.MoreFiles;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A path of a file, along with some hash/digest/checksum.
 * <p>
 * This is used to track inputs to various processing steps, allowing them to be regenerated when the inputs change.
 *
 * @param path   The path to this file.
 * @param digest The digest of this file. The algorithm used to compute this is unspecified, merely that it should be
 *               constant between runs.
 * @see InputCollector
 */
public record FileFingerprint(Path path, String digest) implements BuildInput {
    /**
     * Create a new {@link FileFingerprint} with the default digest algorithm.
     *
     * @param path The file's path.
     * @return The file fingerprint.
     * @throws IOException If the hash could not be computed.
     */
    public static FileFingerprint createDefault(Path path) throws IOException {
        var digest = MoreDigests.createMd5();
        MoreDigests.digestFile(digest, path);
        return new FileFingerprint(path, MoreDigests.toHexString(digest));
    }

    /**
     * Create a new {@link FileFingerprint} for immutable files. This reads from our cache of hashes instead.
     *
     * @param path The file's path.
     * @return The file fingerprint.
     * @throws IOException If the hash could not be computed.
     */
    public static FileFingerprint createImmutable(Path path) throws IOException {
        return new FileFingerprint(path, MoreFiles.getSha1(path));
    }

    @Override
    public void addInputs(InputCollector collector) {
        collector.addInputDigest(digest);
    }

    @Override
    public String toString() {
        return path + " (" + digest + ")";
    }

    /**
     * A function to capture a file fingerprint from a path.
     *
     * @see #createDefault(Path)
     * @see #createImmutable(Path)
     */
    public interface Provider {
        /**
         * Capture a fingerprint of a file.
         *
         * @param path The file's path.
         * @return The file fingerprint
         * @throws IOException If the fingerprint could not be computed.
         */
        FileFingerprint snapshot(Path path) throws IOException;
    }
}
