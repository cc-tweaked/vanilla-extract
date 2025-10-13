package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.download.FileDownloader;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.inputs.HashingInputCollector;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion;
import cc.tweaked.vanillaextract.core.minecraft.manifest.ServerMetadata;
import cc.tweaked.vanillaextract.core.util.JarContentsFilter;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import net.fabricmc.tinyremapper.FileSystemReference;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
        Path clientJar, clientMappings, fullServerJar, serverMappings;
        try (var scope = downloader.openScope()) {
            clientJar = downloads.client().downloadLike(target, "client", "jar").download(scope);
            clientMappings = downloads.client_mappings().downloadLike(target, "client", "txt").download(scope);
            fullServerJar = downloads.server().downloadLike(target, "server", "jar").download(scope);
            serverMappings = downloads.server_mappings().downloadLike(target, "server", "txt").download(scope);
        }

        var extractedServerJar = target.resolve("server-extracted-" + downloads.server().sha1() + ".jar");

        // Extract server dependencies and main jar
        List<String> serverLibraries;
        try (var serverFs = FileSystemReference.openJar(fullServerJar)) {
            var jarPath = serverFs.getPath("META-INF");
            ServerMetadata serverMetadata = ServerMetadata.parse(jarPath);
            if (serverMetadata.versions().size() != 1) {
                throw new IllegalStateException("Got multiple versions in server version list.");
            }

            var extractedServerVersion = serverMetadata.versions().get(0);
            copyIfNeeded(jarPath.resolve("versions"), extractedServerVersion, extractedServerJar);

            serverLibraries = serverMetadata.libraries().stream().map(ServerMetadata.IncludedFile::id).toList();
        }

        // Extract client dependencies.
        var clientLibraryNames = clientLibraries.stream()
            .filter(x -> x.rules() == null || x.rules().isEmpty())
            .map(MinecraftVersion.Library::name).toList();

        return new RawArtifacts(
            new MinecraftJar(
                FileFingerprint.createImmutable(extractedServerJar),
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
        var inputs = new HashingInputCollector("Split jars");
        rawArtifacts.client().jar().addInputs(inputs);
        rawArtifacts.server().jar().addInputs(inputs);
        var digest = inputs.getDigest();

        var clientOnlyJar = target.resolve("client-only-" + digest + ".jar");
        var commonJar = target.resolve("common-" + digest + ".jar");
        if (refresh || !MoreFiles.exists(commonJar) || !MoreFiles.exists(clientOnlyJar)) {
            JarContentsFilter.split(rawArtifacts.server().jar().path(), rawArtifacts.client().jar().path(), commonJar, clientOnlyJar);

            try (var scratch = MoreFiles.scratch(clientOnlyJar.resolveSibling(digest + ".log"))) {
                Files.writeString(scratch.path(), inputs.toString());
                scratch.commit();
            }
        }

        var clientLibrarySet = new HashSet<>(rawArtifacts.client().dependencies());
        var commonDependencies = rawArtifacts.server().dependencies().stream().filter(clientLibrarySet::contains).toList();

        return new SplitArtifacts(
            new MinecraftJar(FileFingerprint.createImmutable(commonJar), rawArtifacts.server().mappings(), commonDependencies),
            new MinecraftJar(FileFingerprint.createImmutable(clientOnlyJar), rawArtifacts.client().mappings(), rawArtifacts.client().dependencies())
        );
    }

    private static void copyIfNeeded(Path jarPath, ServerMetadata.IncludedFile file, Path destination) throws IOException {
        try {
            var sha256 = MoreFiles.computeSha256(destination);
            if (Objects.equals(file.sha256(), sha256)) return;
        } catch (FileNotFoundException | NoSuchFileException ignored) {
        }

        try (var scratch = MoreFiles.scratch(destination)) {
            Files.copy(jarPath.resolve(file.path()), scratch.path(), StandardCopyOption.REPLACE_EXISTING);
            scratch.commit();
        }
    }
}
