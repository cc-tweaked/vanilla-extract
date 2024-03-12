package cc.tweaked.vanillaextract.core.minecraft.manifest;

import cc.tweaked.vanillaextract.core.download.FileDownload;

import java.nio.file.Path;
import java.util.List;

/**
 * The contents of Minecraft's version manifest.
 *
 * @param latest   The latest snapshot and release versions.
 * @param versions The list of available versions.
 * @see MojangUrls#VERSION_MANIFEST
 * @see <a href="https://minecraft.wiki/w/Version_manifest.json">version_manifest.json on the wiki</a>
 */
public record MinecraftVersionManifest(
    Latest latest,
    List<Version> versions
) {
    /**
     * The latest snapshot and release versions.
     *
     * @param release  The latest snapshot version.
     * @param snapshot The latest release version.
     */
    public record Latest(String release, String snapshot) {
    }

    /**
     * A specific Minecraft version.
     *
     * @param id              The id of this version.
     * @param type            The type of this version, typically {@code release} or {@code snapshot}.
     * @param url             The URL to the {@linkplain MinecraftVersion full version information}.
     * @param time            A timestamp of when this version was last updated.
     * @param releaseTime     A timestamp of when this version was first published.
     * @param sha1            A hash of the version.
     * @param complianceLevel {@code 0} if player safety features are not supported, {@code 1} otherwise.
     */
    public record Version(
        String id,
        String type,
        String url,
        String time,
        String releaseTime,
        String sha1,
        int complianceLevel
    ) {
        public FileDownload.Builder downloadTo(Path destination) {
            return FileDownload.builder(url(), destination).expectSha1(sha1);
        }
    }
}
