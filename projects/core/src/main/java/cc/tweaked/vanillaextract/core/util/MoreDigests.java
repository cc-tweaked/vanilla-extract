package cc.tweaked.vanillaextract.core.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

/**
 * Additional functions for working with {@link MessageDigest}s.
 */
public class MoreDigests {
    /**
     * Create a new digest, rethrowing the {@link NoSuchAlgorithmException} as an unchecked exception.
     *
     * @param algorithm The name of the algorithm.
     * @return The new message digest.
     */
    private static MessageDigest getInstance(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to locate digest " + algorithm, e);
        }
    }

    /**
     * A factory for {@link MessageDigest}s. This tries to use {@link Object#clone()} on an existing object before
     * falling back to {@link #getInstance(String)} if not available.
     */
    private static final class MessageDigestFactory {
        private final String name;
        private @Nullable Supplier<MessageDigest> supplier;

        private MessageDigestFactory(String name) {
            this.name = name;
        }

        public MessageDigest get() {
            return (supplier == null ? supplier = getSupplier(name) : supplier).get();
        }

        private static Supplier<MessageDigest> getSupplier(String algorithm) {
            var digest = getInstance(algorithm);
            try {
                digest.clone();
                return () -> {
                    try {
                        return (MessageDigest) digest.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new IllegalStateException("Hash function should be cloneable", e);
                    }
                };
            } catch (CloneNotSupportedException e) {
                return () -> getInstance(algorithm);
            }
        }
    }

    private static final MessageDigestFactory MD5 = new MessageDigestFactory("MD5");
    private static final MessageDigestFactory SHA1 = new MessageDigestFactory("SHA-1");
    private static final MessageDigestFactory SHA256 = new MessageDigestFactory("SHA-256");

    public static MessageDigest createMd5() {
        return MD5.get();
    }

    public static MessageDigest createSha1() {
        return SHA1.get();
    }

    public static MessageDigest createSha256() {
        return SHA256.get();
    }

    /**
     * Digest a file.
     *
     * @param digest The digester.
     * @param path   The path of the file.
     * @throws IOException If the file could not be read.
     */
    public static void digestFile(MessageDigest digest, Path path) throws IOException {
        try (var stream = Files.newInputStream(path)) {
            digestStream(digest, stream);
        }
    }

    /**
     * Digest an {@link InputStream}.
     *
     * @param digest The digester.
     * @param stream The stream to digest.
     * @throws IOException If the stream could not be read.
     */
    public static void digestStream(MessageDigest digest, InputStream stream) throws IOException {
        byte[] block = new byte[8192];
        while (true) {
            int read = stream.read(block);
            if (read < 0) break;
            digest.update(block, 0, read);
        }
    }

    /**
     * Convert a digest to a hex string.
     *
     * @param digest The digest to encode.
     * @return The hexadecimal representation of this digest.
     */
    public static String toHexString(MessageDigest digest) {
        return toHexString(digest.digest());
    }

    /**
     * Convert a hash to a hex string.
     *
     * @param hash The hash to encode.
     * @return The hexadecimal representation of this digest.
     */
    public static String toHexString(byte[] hash) {
        var builder = new StringBuilder(hash.length * 2);
        for (var b : hash) {
            builder.append("0123456789abcdef".charAt((b >> 4) & 0xF));
            builder.append("0123456789abcdef".charAt(b & 0xF));
        }
        return builder.toString();
    }
}
