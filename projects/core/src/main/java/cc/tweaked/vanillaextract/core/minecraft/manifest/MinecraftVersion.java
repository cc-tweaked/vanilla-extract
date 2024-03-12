package cc.tweaked.vanillaextract.core.minecraft.manifest;

import cc.tweaked.vanillaextract.core.download.FileDownload;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * Information about a specific Minecraft version.
 *
 * @param arguments              Arguments for launching the game.
 * @param assetIndex             The asset index for this version.
 * @param assets                 The assets version, same as {@link AssetIndex#id()}
 * @param complianceLevel        {@code 0} if player safety features are not supported, {@code 1} otherwise.
 * @param downloads              The main downloads for this version.
 * @param id                     This version's name.
 * @param javaVersion            The version of the Java runtime, unused by us.
 * @param libraries              Required libraries to launch this version.
 * @param logging                Logging configuration, unused by us.
 * @param mainClass              The class used to launch the game.
 * @param minimumLauncherVersion The minimum launcher version that can launch this game.
 * @param releaseTime            The time this version was released.
 * @param time                   Same as {@link #releaseTime()}.
 * @param type                   The type of this version, typically {@code release} or {@code snapshot}.
 * @see <a href="https://minecraft.wiki/w/Client.json">client.json on the Minecraft wiki</a>
 */
public record MinecraftVersion(
    Arguments arguments,
    AssetIndex assetIndex,
    String assets,
    int complianceLevel,
    Downloads downloads,
    String id,
    Object javaVersion,
    List<Library> libraries,
    Object logging,
    String mainClass,
    int minimumLauncherVersion,
    String releaseTime,
    String time,
    String type
) {
    /**
     * Arguments to be passed when launching the game. These are ignored by us, so this is somewhat incomplete.
     *
     * @param game Arguments to be passed to the game itself.
     * @param jvm  Arguments to be passed to the JVM.
     */
    record Arguments(List<Object> game, List<Object> jvm) {
    }

    /**
     * A reference to the asset indexed used by this version of the game.
     *
     * @param id        The asset version.
     * @param sha1      The hash of the asset file.
     * @param size      The size of the asset file.
     * @param totalSize The total size of all downloaded assets.
     * @param url       The URL of the asset file.
     */
    record AssetIndex(
        String id,
        String sha1,
        int size,
        int totalSize,
        String url
    ) {
    }

    /**
     * The main downloads for this version.
     *
     * @param client          The client jar.
     * @param client_mappings The mappings for the client jar.
     * @param server          The server jar.
     * @param server_mappings The mappings for the server jar.
     */
    public record Downloads(
        Download client,
        Download client_mappings,
        Download server,
        Download server_mappings
    ) {
    }

    /**
     * A single file to download.
     *
     * @param sha1 The hash of this file.
     * @param size The size of this file.
     * @param url  The URL where the file is hosted.
     */
    public record Download(String sha1, long size, String url) {
        public FileDownload.Builder downloadTo(Path destination) {
            return FileDownload.builder(url(), destination).expectSha1(sha1);
        }
    }

    /**
     * A library needed to run the game.
     *
     * @param downloads The download(s) for this library.
     * @param name      The name of this library, as a Maven coordinate.
     * @param rules     Predicates to determine whether this library is needed or not.
     */
    public record Library(
        LibraryDownloads downloads,
        String name,
        @Nullable List<Rule> rules
    ) {
    }

    /**
     * Download(s) for a librry.
     *
     * @param artifact The main artifact for this library.
     */
    public record LibraryDownloads(LibraryArtifact artifact) {
    }

    /**
     * A single file to download.
     *
     * @param path The path to download this file to.
     * @param sha1 The hash of this file.
     * @param size The size of this file.
     * @param url  The URL where the file is hosted.
     */
    public record LibraryArtifact(String path, String sha1, long size, String url) {
        public FileDownload.Builder downloadTo(Path destination) {
            return FileDownload.builder(url(), destination).expectSha1(sha1);
        }
    }

    /**
     * A rule which controls whether a download is needed or not.
     *
     * @param action Should always be {@code "action"}.
     * @param os     The operating system this rule should be applied on.
     */
    public record Rule(String action, OS os) {
    }

    /**
     * A specific operating system.
     *
     * @param name One of {@code windows}, {@code linux}, {@code osx}.
     */
    public record OS(String name) {
    }
}
