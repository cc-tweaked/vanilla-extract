package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.download.FileDownload;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MojangUrls;
import cc.tweaked.vanillaextract.core.support.MirrorDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class MinecraftVersionProviderTest {
    public static final URI URL_1_20_4 = uri("https://piston-meta.mojang.com/v1/packages/c98adde5094a3041f486b4d42d0386cf87310559/1.20.4.json");
    public static final String SHA_1_20_4 = "c98adde5094a3041f486b4d42d0386cf87310559";

    @TempDir
    private Path dir;

    @Test
    public void testDownload() throws IOException {
        var downloader = MirrorDownloader.createOffline();
        var provider = new MinecraftVersionProvider(dir, downloader);

        // First download the file, and check we tried to download both files.
        provider.getVersion("1.20.4", false);
        assertIterableEquals(List.of(
            new FileDownload(uri(MojangUrls.VERSION_MANIFEST), dir.resolve("manifest.json"), null, true),
            new FileDownload(URL_1_20_4, dir.resolve("1.20.4/version.json"), SHA_1_20_4, false)
        ), downloader.takeDownloads());

        // Try again, and check we didn't try to re-download the manifest.
        provider.getVersion("1.20.4", false);
        assertIterableEquals(List.of(
            new FileDownload(URL_1_20_4, dir.resolve("1.20.4/version.json"), SHA_1_20_4, false)
        ), downloader.takeDownloads());
    }

    public static URI uri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
