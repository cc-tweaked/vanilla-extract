package cc.tweaked.vanillaextract.core.support;

import cc.tweaked.vanillaextract.core.download.BasicFileDownloader;
import cc.tweaked.vanillaextract.core.download.DownloadException;
import cc.tweaked.vanillaextract.core.download.FileDownload;
import cc.tweaked.vanillaextract.core.download.FileDownloader;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link FileDownloader} which just copies files from a pre-defined list of files.
 */
public class MirrorDownloader implements FileDownloader {
    private static final Logger LOG = LoggerFactory.getLogger(MirrorDownloader.class);

    public static final Path ROOT = Path.of("src/test/resources/mirror");

    private final List<FileDownload> downloads = new ArrayList<>();
    private final @Nullable FileDownloader fallback;

    private MirrorDownloader(@Nullable FileDownloader fallback) {
        this.fallback = fallback;
    }

    /**
     * Create a downloader which just uses the mirror directory.
     *
     * @return The newly created downloader.
     */
    public static MirrorDownloader createOffline() {
        return new MirrorDownloader(null);
    }

    /**
     * Create a downloader which downloads missing files and saves them to the mirror directory.
     *
     * @return The newly created downloader.
     */
    public static MirrorDownloader createWithFallback() {
        return new MirrorDownloader(new BasicFileDownloader());
    }

    @Override
    public void download(FileDownload download) throws DownloadException {
        downloads.add(download);

        var path = ROOT.resolve(download.uri().getHost()).resolve(download.uri().getPath().substring(1));
        if (!MoreFiles.exists(path)) {
            if (fallback == null) {
                LOG.info("Skipping download of {} ({} does not exist)", download.uri(), path);
            } else {
                fallback.download(new FileDownload(download.uri(), path, download.sha1(), download.force()));
            }
        }

        try {
            Files.createDirectories(download.destination().getParent());
            Files.copy(path, download.destination(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Scope openScope() {
        return new ScopeImpl();
    }

    /**
     * Get all downloads since this function was last called.
     *
     * @return All called downloads.
     */
    public List<FileDownload> takeDownloads() {
        var downloads = List.copyOf(this.downloads);
        this.downloads.clear();
        return downloads;
    }

    private class ScopeImpl implements Scope {
        private final List<FileDownload> downloads = new ArrayList<>();

        @Override
        public void download(FileDownload download) {
            downloads.add(download);
        }

        @Override
        public void close() throws DownloadException {
            for (var download : downloads) MirrorDownloader.this.download(download);
        }
    }
}
