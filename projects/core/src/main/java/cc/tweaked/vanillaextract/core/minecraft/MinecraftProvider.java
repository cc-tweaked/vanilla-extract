package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.download.FileDownloader;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion;
import cc.tweaked.vanillaextract.core.minecraft.manifest.ServerMetadata;
import cc.tweaked.vanillaextract.core.util.JarContentsFilter;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import net.fabricmc.tinyremapper.FileSystemReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * Provides the base Minecraft jars {@code client.jar}, {@code server.jar}.
 */
public final class MinecraftProvider {
    private final FileDownloader downloader;

    /**
     * Construct a new Minecraft provider.
     *
     * @param downloader The file downloader to use.
     */
    public MinecraftProvider(FileDownloader downloader) {
        this.downloader = downloader;
    }

    /**
     * A single Minecraft jar, and information required to consume that jar.
     *
     * @param jar          The path to the jar file.
     * @param mappings     The mappings for this jar.
     * @param dependencies A list of dependencies for this artifact.
     */
    public record MinecraftJar(FileFingerprint jar, FileFingerprint mappings, List<String> dependencies) {
    }

    /**
     * The raw artifacts.
     *
     * @param server The server jar.
     * @param client The client jar.
     */
    public record RawArtifacts(MinecraftJar server, MinecraftJar client) {
    }

    /**
     * All Minecraft artifacts.
     *
     * @param common The common/server jar.
     * @param client The client-only jar.
     */
    public record SplitArtifacts(MinecraftJar common, MinecraftJar client) {
        /**
         * Get the list of mappings used to deobfuscate the jars.
         *
         * @return The list of mappings.
         */
        public List<FileFingerprint> mappings() {
            return List.of(common().mappings(), client().mappings());
        }
    }

    /**
     * Download the vanilla Minecraft jars and extract their dependencies.
     *
     * @param target          The directory to download and unpack the jars into.
     * @param downloads       The downloads for this version.
     * @param clientLibraries The client libraries for this version.
     * @return The resulting {@linkplain SplitArtifacts minecraft artifacts}.
     * @throws IOException If the jars could not be downloaded.
     */
    public RawArtifacts provideRaw(
        Path target,
        MinecraftVersion.Downloads downloads,
        List<MinecraftVersion.Library> clientLibraries
    ) throws IOException {
        var clientJar = target.resolve("client.jar");
        var clientMappings = target.resolve("client.txt");
        var fullServerJar = target.resolve("server.jar");
        var serverMappings = target.resolve("server.txt");

        try (var scope = downloader.openScope()) {
            downloads.client().downloadTo(clientJar).download(scope);
            downloads.client_mappings().downloadTo(clientMappings).download(scope);
            downloads.server().downloadTo(fullServerJar).download(scope);
            downloads.server_mappings().downloadTo(serverMappings).download(scope);
        }

        var extractedServerJar = target.resolve("server-extracted.jar");

        // Extract server dependencies and main jar
        List<String> serverLibraries;
        try (var serverFs = FileSystemReference.openJar(fullServerJar)) {
            var jarPath = serverFs.getPath("META-INF");
            ServerMetadata serverMetadata = ServerMetadata.parse(jarPath);
            if (serverMetadata.versions().size() != 1) {
                throw new IllegalStateException("Got multiple versions in server version list.");
            }

            var serverVersion = serverMetadata.versions().get(0);
            copyIfNeeded(jarPath.resolve("versions"), serverVersion, extractedServerJar);

            serverLibraries = serverMetadata.libraries().stream().map(ServerMetadata.IncludedFile::id).toList();
        }

        // Extract client dependencies.
        var clientLibraryNames = clientLibraries.stream()
            .filter(x -> x.rules() == null || x.rules().isEmpty())
            .map(MinecraftVersion.Library::name).toList();

        return new RawArtifacts(
            new MinecraftJar(
                new FileFingerprint(extractedServerJar, downloads.server().sha1()),
                new FileFingerprint(serverMappings, downloads.server_mappings().sha1()),
                serverLibraries
            ),
            new MinecraftJar(
                new FileFingerprint(clientJar, downloads.client().sha1()),
                new FileFingerprint(clientMappings, downloads.client_mappings().sha1()),
                clientLibraryNames
            )
        );
    }

    /**
     * Split the vanilla Minecraft jars into separate common and client jars.
     *
     * @param target       The directory to download and unpack the jars into.
     * @param rawArtifacts The raw Minecraft artifacts, as returned by {@link #provideRaw(Path, MinecraftVersion.Downloads, List)}.
     * @param refresh      Re-process the split files.
     * @return The resulting {@linkplain SplitArtifacts minecraft artifacts}.
     * @throws IOException If the jars could not be downloaded.
     */
    public SplitArtifacts provideSplit(Path target, RawArtifacts rawArtifacts, boolean refresh) throws IOException {
        // Split the client and server jars.
        var clientOnlyJar = target.resolve("client-only.jar");
        var commonJar = target.resolve("common.jar");
        // FIXME: This doesn't correctly handle the jars changing. I don't think that'll ever happen, but worth checking!
        if (refresh || !MoreFiles.exists(commonJar) || !MoreFiles.exists(clientOnlyJar)) {
            JarContentsFilter.split(rawArtifacts.server().jar().path(), rawArtifacts.client().jar().path(), commonJar, clientOnlyJar);
        }

        var clientLibrarySet = new HashSet<>(rawArtifacts.client().dependencies());
        var commonDependencies = rawArtifacts.server().dependencies().stream().filter(clientLibrarySet::contains).toList();

        return new SplitArtifacts(
            new MinecraftJar(FileFingerprint.createImmutable(commonJar), rawArtifacts.server().mappings(), commonDependencies),
            new MinecraftJar(FileFingerprint.createImmutable(clientOnlyJar), rawArtifacts.client().mappings(), rawArtifacts.client().dependencies())
        );
    }

    private static void copyIfNeeded(Path jarPath, ServerMetadata.IncludedFile file, Path destination) throws IOException {
        if (Objects.equals(file.sha1(), MoreFiles.tryGetSha1(destination))) return;

        try (var scratch = MoreFiles.scratch(destination)) {
            Files.copy(jarPath.resolve(file.path()), scratch.path(), StandardCopyOption.REPLACE_EXISTING);
            scratch.commit();
        }
    }
}
