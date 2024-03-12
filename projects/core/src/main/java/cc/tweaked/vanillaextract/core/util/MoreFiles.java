package cc.tweaked.vanillaextract.core.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.UserDefinedFileAttributeView;
import java.util.zip.ZipOutputStream;

/**
 * More support for working with files, in the vein of {@link Files}.
 */
public final class MoreFiles {
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final Logger LOG = LoggerFactory.getLogger(MoreFiles.class);
    private static final String SHA1_ATTR = "VanillaExtractSha1";

    private MoreFiles() {
    }

    /**
     * Get the SHA1 hash of the given path.
     *
     * @param path The path to check.
     * @return The hash, or {@code null} if it could not be computed.
     */
    public static @Nullable String tryGetSha1(Path path) {
        try {
            return getSha1(path);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Get the SHA1 hash of the given path.
     *
     * @param path The path to check.
     * @return The hash, or {@code null} if it could not be computed.
     */
    public static String getSha1(Path path) throws IOException {
        var attr = getAttribute(path, SHA1_ATTR);
        if (attr != null) return attr;

        var sha = computeSha1(path);
        setSha1(path, sha);
        return sha;
    }

    /**
     * Compute the SHA1 hash of the given path.
     *
     * @param path The path of the file to read.
     * @return The hash.
     */
    public static String computeSha1(Path path) throws IOException {
        var digest = MoreDigests.createSha1();
        MoreDigests.digestFile(digest, path);
        return MoreDigests.toHexString(digest);
    }

    /**
     * Store SHA1 hash of the given path.
     *
     * @param path The file to update.
     * @param hash The SHA1 hash of this file.
     */
    public static void setSha1(Path path, String hash) {
        if (!setAttribute(path, SHA1_ATTR, hash)) LOG.warn("Cannot cache hash for {}.", path);
    }

    /**
     * Update the SHA hash of the given path.
     *
     * @param path The file to update.
     * @throws IOException If the sha could not be updated.
     */
    public static void updateSha(Path path) throws IOException {
        setSha1(path, computeSha1(path));
    }

    /**
     * Compute the MD5 hash of the given path.
     *
     * @param path The path of the file to read.
     * @return The hash.
     */
    public static String computeMd5(Path path) throws IOException {
        var digest = MoreDigests.createMd5();
        MoreDigests.digestFile(digest, path);
        return MoreDigests.toHexString(digest);
    }

    /**
     * Attempt to read a custom file attribute.
     *
     * @param path      The path to the file.
     * @param attribute The name of the attribute.
     * @return The attribute, or {@code null} if it doesn't exist or cannot be read.
     */
    private static @Nullable String getAttribute(Path path, String attribute) {
        var userAttributes = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        if (userAttributes == null) return null;
        try {
            var buffer = ByteBuffer.allocateDirect(userAttributes.size(attribute));
            userAttributes.read(attribute, buffer);
            buffer.flip();
            return StandardCharsets.UTF_8.decode(buffer).toString();
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Attempt to write a custom file attribute.
     *
     * @param path      The path to the file.
     * @param attribute The name of the attribute.
     * @param value     The value of the attribute.
     * @return If the attribute was successfully written.
     */
    private static boolean setAttribute(Path path, String attribute, String value) {
        var userAttributes = Files.getFileAttributeView(path, UserDefinedFileAttributeView.class);
        if (userAttributes == null) return false;
        try {
            var contents = StandardCharsets.UTF_8.encode(value);
            var length = contents.remaining();
            var written = userAttributes.write(attribute, contents);
            return length == written;
        } catch (IOException ignored) {
            return false;
        }
    }

    /**
     * Checks if a file exists, without resolving symlinks.
     *
     * @param path The path to check.
     * @return Whether the file exists.
     */
    public static boolean exists(Path path) {
        return path.getFileSystem() == FileSystems.getDefault() ? path.toFile().exists() : Files.exists(path);
    }

    /**
     * Attempt to atomically replace the file at {@code to} with that at {@code from}.
     *
     * @param from The file to replace with.
     * @param to   The file to be replaced.
     * @throws IOException If the file could not be replaced.
     */
    public static void replace(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Try to delete a {@link Path}, ignoring any errors.
     *
     * @param path The path to delete
     */
    public static void tryDelete(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Ignored
        }
    }

    public static Path addSuffix(Path path, String suffix) {
        var name = path.getFileName().toString();
        var extensionIdx = name.lastIndexOf('.');
        if (extensionIdx <= 0) throw new IllegalArgumentException(path + " has no extension");

        return path.resolveSibling(name.substring(0, extensionIdx) + suffix + name.substring(extensionIdx));
    }

    /**
     * Read a JSON file
     *
     * @param path  The path to the file.
     * @param klass The type of value to read.
     * @param <T>   The type of value to read.
     * @return The parsed value.
     * @throws IOException                        If the file could not be read.
     * @throws com.google.gson.JsonParseException If the JSON could not be parsed.
     */
    public static <T> T readJson(Path path, Class<T> klass) throws IOException {
        try (var stream = Files.newBufferedReader(path)) {
            return GSON.fromJson(stream, klass);
        }
    }

    /**
     * Create a temporary "scratch" file, which will replace {@code destination} when finished.
     *
     * @param destination The destination to write to.
     * @return The {@link ScratchFile}.
     * @throws IOException If the file cannot be created.
     */
    public static ScratchFile scratch(Path destination) throws IOException {
        var tempFile = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".scratch");
        return new ScratchFile(tempFile, destination);
    }

    /**
     * Create a temporary "scratch" file, which will replace {@code destination} when finished.
     *
     * @param destination The destination to write to.
     * @return The {@link ScratchFile}.
     * @throws IOException If the file cannot be created.
     */
    public static ScratchFile scratchZip(Path destination) throws IOException {
        var tempFile = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".scratch");

        // Nasty hack, but write the ZIP header.
        try (var stream = Files.newOutputStream(tempFile)) {
            new ZipOutputStream(stream).close();
        }

        return new ScratchFile(tempFile, destination);
    }

    /**
     * A temporary scratch file, marking some in-progress work which will eventually be written to {@link #destination()}.
     *
     * @param path        The path to the scratch file.
     * @param destination The eventual destination
     */
    public record ScratchFile(Path path, Path destination) implements Closeable {
        /**
         * Commit the scratch file, overwriting the original.
         *
         * @throws IOException If the file could not be committed.
         */
        public void commit() throws IOException {
            replace(path, destination);
        }

        @Override
        public void close() {
            tryDelete(path);
        }
    }
}
