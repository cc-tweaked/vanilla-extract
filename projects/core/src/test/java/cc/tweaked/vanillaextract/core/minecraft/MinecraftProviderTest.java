package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.download.FileDownload;
import cc.tweaked.vanillaextract.core.download.FileDownloader;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion;
import cc.tweaked.vanillaextract.core.support.MirrorDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static cc.tweaked.vanillaextract.core.minecraft.MinecraftVersionProviderTest.uri;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class MinecraftProviderTest {
    @TempDir
    private Path dir;

    public static MinecraftProvider.SplitArtifacts setupMinecraft(Path dir, FileDownloader downloader) throws IOException {
        var downloads = new MinecraftVersion.Downloads(
            new MinecraftVersion.Download("fd19469fed4a4b4c15b2d5133985f0e3e7816a8a", 0, "https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar"),
            new MinecraftVersion.Download("be76ecc174ea25580bdc9bf335481a5192d9f3b7", 0, "https://piston-data.mojang.com/v1/objects/be76ecc174ea25580bdc9bf335481a5192d9f3b7/client.txt"),
            new MinecraftVersion.Download("8dd1a28015f51b1803213892b50b7b4fc76e594d", 0, "https://piston-data.mojang.com/v1/objects/8dd1a28015f51b1803213892b50b7b4fc76e594d/server.jar"),
            new MinecraftVersion.Download("c1cafe916dd8b58ed1fe0564fc8f786885224e62", 0, "https://piston-data.mojang.com/v1/objects/c1cafe916dd8b58ed1fe0564fc8f786885224e62/server.txt")
        );
        var libraries = List.of(
            new MinecraftVersion.Library(null, "org.slf4j:slf4j-api:2.0.7", null)
        );

        var provider = new MinecraftProvider(downloader);
        var rawArtifacts = provider.provideRaw(dir, downloads, libraries);
        return new MinecraftProvider(downloader).provideSplit(dir, rawArtifacts, false);
    }

    @Test
    public void downloadsFiles() throws IOException {
        var downloader = MirrorDownloader.createOffline();
        var result = setupMinecraft(dir, downloader);

        assertIterableEquals(List.of(
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar"), dir.resolve("client.jar"), "fd19469fed4a4b4c15b2d5133985f0e3e7816a8a", false),
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/be76ecc174ea25580bdc9bf335481a5192d9f3b7/client.txt"), dir.resolve("client.txt"), "be76ecc174ea25580bdc9bf335481a5192d9f3b7", false),
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/8dd1a28015f51b1803213892b50b7b4fc76e594d/server.jar"), dir.resolve("server.jar"), "8dd1a28015f51b1803213892b50b7b4fc76e594d", false),
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/c1cafe916dd8b58ed1fe0564fc8f786885224e62/server.txt"), dir.resolve("server.txt"), "c1cafe916dd8b58ed1fe0564fc8f786885224e62", false)
        ), downloader.takeDownloads());

        assertIterableEquals(
            result.client().dependencies(),
            List.of("org.slf4j:slf4j-api:2.0.7"),
            "Client dependencies are the same"
        );
    }
}
