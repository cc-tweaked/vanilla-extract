package cc.tweaked.vanillaextract.core.download;

import cc.tweaked.vanillaextract.core.util.MoreFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A concrete implementation of {@link FileDownloader} which uses {@link HttpClient}.
 */
public class BasicFileDownloader implements FileDownloader {
    private static final Logger LOG = LoggerFactory.getLogger(BasicFileDownloader.class);

    private static final int MAX_RETRIES = 3;
    private static final Duration TIMEOUT = Duration.ofMinutes(1);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .connectTimeout(TIMEOUT)
        .build();

    @Override
    public void download(FileDownload download) throws DownloadException {
        downloadImpl(download);
    }

    @Override
    public Scope openScope() {
        return new ScopeImpl();
    }

    private static boolean canSkipDownload(FileDownload download) {
        if (download.sha1() != null) {
            var actualSha = MoreFiles.tryGetSha1(download.destination());
            return Objects.equals(download.sha1(), actualSha);
        }

        if (download.force()) return false;

        // TODO: Check expiry time.
        return MoreFiles.exists(download.destination());
    }

    private static void downloadImpl(FileDownload download) throws DownloadException {
        if (canSkipDownload(download)) return;

        try {
            Files.createDirectories(download.destination().getParent());
        } catch (IOException e) {
            throw new DownloadException(download.uri(), "failed to create parent directory", e);
        }

        try (var tempFile = MoreFiles.scratch(download.destination())) {
            downloadAndReplace(download, tempFile);
        } catch (DownloadException e) {
            throw e;
        } catch (IOException e) {
            throw new DownloadException(download.uri(), "cannot create temporary file", e);
        }
    }

    private static void downloadAndReplace(FileDownload download, MoreFiles.ScratchFile scratch) throws DownloadException {
        LOG.info("Downloading {} to {}", download.uri(), scratch.destination());

        var request = HttpRequest.newBuilder(download.uri()).timeout(TIMEOUT);

        for (int i = 1; ; i++) {
            HttpResponse<?> response;
            try {
                response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofFile(scratch.path()));
            } catch (IOException e) {
                throw new DownloadException(download.uri(), "download failed", e);
            } catch (InterruptedException e) {
                throw new DownloadException(download.uri(), "download interrupted", e);
            }

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Ensure that the SHA matches.
                if (download.sha1() != null) {
                    String actualHash;
                    try {
                        actualHash = MoreFiles.computeSha1(scratch.path());
                    } catch (IOException e) {
                        throw new DownloadException(download.uri(), "Failed to compute hash", e);
                    }
                    if (!Objects.equals(download.sha1(), actualHash)) {
                        throw new DownloadException(download.uri(), "expected " + download.sha1() + ", got " + actualHash);
                    }
                }

                break;
            }

            // If we've got a 404, or have retried too many times then just abort.
            if (i >= MAX_RETRIES || response.statusCode() == 404) {
                throw new DownloadException(download.uri(), "got non-200 status code " + response);
            }

            // Double check if we can skip downloading (if the file has been downloaded on another thread),
            // otherwise try to download again.
            if (canSkipDownload(download)) return;

            LOG.info("Download of {} returned {}. Retrying.", download.uri(), response.statusCode());
        }

        try {
            scratch.commit();
        } catch (IOException e) {
            throw new DownloadException(download.uri(), "cannot replace existing file", e);
        }
    }

    private static class ScopeImpl implements Scope {
        private final List<FileDownload> downloads = new ArrayList<>();

        @Override
        public void download(FileDownload download) {
            downloads.add(download);
        }

        @Override
        public void close() throws DownloadException {
            // TODO: Do these in parallel.
            for (var download : downloads) downloadImpl(download);
        }
    }
}
