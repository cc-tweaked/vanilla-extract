package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.TestData;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class MinecraftVersionProviderTest {
    private static final URI URL_1_20_4 = uri("https://piston-meta.mojang.com/v1/packages/e2faca08a8d4c3358af4269e8bcea1fbad586df4/1.20.4.json");
    private static final String SHA_1_20_4 = "e2faca08a8d4c3358af4269e8bcea1fbad586df4";
    private static final String PATH_1_20_4 = "1.20.4/version-" + SHA_1_20_4 + ".json";

    @TempDir
    private Path dir;

    @Test
    public void testDownload() throws IOException {
        var downloader = MirrorDownloader.createOffline();
        var provider = new MinecraftVersionProvider(dir, downloader);

        // First download the file, and check we tried to download both files.
        var versionInfo = provider.getVersion("1.20.4", false);
        assertIterableEquals(List.of(
            new FileDownload(uri(MojangUrls.VERSION_MANIFEST), dir.resolve("manifest.json"), null, true),
            new FileDownload(URL_1_20_4, dir.resolve(PATH_1_20_4), SHA_1_20_4, false)
        ), downloader.takeDownloads());

        assertEquals(TestData.MC_1_20_4, versionInfo.downloads());

        // Try again, and check we didn't try to re-download the manifest.
        var newVersionInfo = provider.getVersion("1.20.4", false);
        assertIterableEquals(List.of(
            new FileDownload(URL_1_20_4, dir.resolve(PATH_1_20_4), SHA_1_20_4, false)
        ), downloader.takeDownloads());

        assertEquals(versionInfo, newVersionInfo);

        // System.out.println(versionInfo.libraries());
        for (var lib : versionInfo.libraries()) {
            System.out.printf(
                "new Library(new LibraryDownloads(new LibraryArtifact(\"\", \"%s\", %d, \"%s\")), \"%s\", null),\n",
                lib.downloads().artifact().sha1(), lib.downloads().artifact().size(), lib.downloads().artifact().url(),
                lib.name()
            );
        }
    }

    public static URI uri(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
