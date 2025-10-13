package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.download.FileDownload;
import cc.tweaked.vanillaextract.core.download.FileDownloader;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersionManifest;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MojangUrls;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * Fetches metadata information about a specific Minecraft version.
 */
public final class MinecraftVersionProvider {
    private final Path cachePath;
    private final Path manifestPath;
    private final FileDownloader downloader;

    /**
     * Construct a new Minecraft version provider.
     *
     * @param cachePath  The path of the global cache.
     * @param downloader The file downloader to use.
     */
    public MinecraftVersionProvider(Path cachePath, FileDownloader downloader) {
        this.cachePath = cachePath;
        this.manifestPath = cachePath.resolve("manifest.json");
        this.downloader = downloader;
    }

    /**
     * Get or download the {@link MinecraftVersion} information.
     *
     * @param version The version to download.
     * @param refresh Always download a new version of the manifest, rather than using the cached one.
     * @return The downloaded version, or {@code null} if it could not be found.
     * @throws IOException If the file could not be downloaded.
     */
    public MinecraftVersion getVersion(String version, boolean refresh) throws IOException {
        var versionInfo = getManifestVersion(version, downloader, refresh);
        if (versionInfo == null) throw new IllegalArgumentException("Cannot find Minecraft version " + version);

        var versionPath = cachePath.resolve(version).resolve("version-" + versionInfo.sha1() + ".json");
        versionInfo.downloadTo(versionPath).download(downloader);

        return MoreFiles.readJson(versionPath, MinecraftVersion.class);
    }

    private @Nullable MinecraftVersionManifest.Version getManifestVersion(String version, FileDownloader downloader, boolean refresh) throws IOException {
        if (!refresh) {
            // Try to read from the cached file.
            try {
                var versionInfo = getVersionManifestFromFile(version);
                if (versionInfo != null) return versionInfo;
            } catch (NoSuchFileException ignored) {
            }
        }

        // Otherwise re-download the manifest and try again.
        FileDownload.builder(MojangUrls.VERSION_MANIFEST, manifestPath).force().download(downloader);
        return getVersionManifestFromFile(version);
    }

    private @Nullable MinecraftVersionManifest.Version getVersionManifestFromFile(String version) throws IOException {
        var manifest = MoreFiles.readJson(manifestPath, MinecraftVersionManifest.class);
        return manifest.versions().stream().filter(x -> x.id().equals(version)).findFirst().orElse(null);
    }
}
