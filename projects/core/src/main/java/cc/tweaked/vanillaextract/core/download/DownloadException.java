package cc.tweaked.vanillaextract.core.download;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;

/**
 * An exception that occurs when {@linkplain FileDownloader downloading a file}.
 */
public class DownloadException extends IOException {
    public DownloadException(URI uri, String message) {
        this(uri, message, null);
    }

    public DownloadException(URI uri, String message, @Nullable Throwable cause) {
        super("Failed to download " + uri + ": " + message, cause);
    }
}
