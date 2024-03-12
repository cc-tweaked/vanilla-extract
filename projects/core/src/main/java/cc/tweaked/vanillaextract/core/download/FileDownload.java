package cc.tweaked.vanillaextract.core.download;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A file to be downloaded.
 * <p>
 * This handles caching, retries and attempts to ensure atomicity of downloads.
 *
 * @param uri         The URI of the file to download.
 * @param destination The location the file should be downloaded to.
 * @param sha1        The expected hash of the file. This is used to avoid downloading the file again.
 * @param force       Whether to force the file to be updated, even if it was downloaded recently.
 * @see FileDownloader
 */
public record FileDownload(URI uri, Path destination, @Nullable String sha1, boolean force) {
    /**
     * Create a new builder for {@link FileDownload}s.
     *
     * @param url  The URL to download.
     * @param path The path to download to.
     * @return The builder.
     */
    public static Builder builder(String url, Path path) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot parse URL", e);
        }

        return new Builder(uri, path);
    }

    /**
     * Create a new builder for {@link FileDownload}s.
     *
     * @param uri  The URL to download.
     * @param path The path to download to.
     * @return The builder.
     */
    public static Builder builder(URI uri, Path path) {
        return new Builder(uri, path);
    }

    /**
     * A builder for {@link FileDownload}s.
     */
    public static final class Builder {
        private final URI uri;
        private final Path path;

        private @Nullable String sha1;
        private boolean force;

        private Builder(URI uri, Path path) {
            this.uri = uri;
            this.path = path;
        }

        /**
         * Expect the file to have a specific hash to avoid downloading it it does.
         *
         * @param sha1 The hash to check.
         * @return {@code this}, for chaining.
         * @see FileDownload#uri
         */
        public Builder expectSha1(String sha1) {
            this.sha1 = Objects.requireNonNull(sha1, "Hash cannot be null");
            return this;
        }

        /**
         * Always download this file, even if it already exists.
         *
         * @return {@code this}, for chaining.
         */
        public Builder force() {
            force = true;
            return this;
        }

        /**
         * Return the finished {@link FileDownload}.
         *
         * @return The resulting {@link FileDownload}.
         */
        public FileDownload build() {
            return new FileDownload(uri, path, sha1, force);
        }

        /**
         * Download the file to the given path.
         *
         * @param downloader The downloader to use.
         * @throws DownloadException If the download could not complete.
         */
        public void download(FileDownloader downloader) throws DownloadException {
            downloader.download(build());
        }

        /**
         * Download a file asynchronously. This is used for running multiple downloads in parallel.
         *
         * @param downloader The downloader to use.
         */
        public void download(FileDownloader.Scope downloader) {
            downloader.download(build());
        }
    }
}
