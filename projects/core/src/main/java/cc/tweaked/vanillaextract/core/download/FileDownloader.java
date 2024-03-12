package cc.tweaked.vanillaextract.core.download;

import java.io.IOException;

/**
 * Responsible for downloading files to a particular location.
 *
 * @see FileDownload
 * @see BasicFileDownloader
 */
public interface FileDownloader {
    /**
     * Download a file.
     *
     * @param download The download to perform.
     * @throws DownloadException If an error occurred downloading the file. This typically wraps the underlying
     *                           {@link IOException}, providing a little more information.
     */
    void download(FileDownload download) throws DownloadException;

    /**
     * Open a resource for running multiple downloads in parallel.
     *
     * @return The newly created scope.
     */
    Scope openScope();

    /**
     * A scope for scheduling several parallel downloads.
     */
    interface Scope extends AutoCloseable {
        /**
         * Enqueue a file to be downloaded in parallel.
         *
         * @param download The download to perform.
         */
        void download(FileDownload download);

        /**
         * Wait for all downloads to finish.
         *
         * @throws DownloadException If an error occurred downloading a file. This typically wraps the underlying
         *                           {@link IOException}, providing a little more information.
         */
        @Override
        void close() throws DownloadException;
    }
}

