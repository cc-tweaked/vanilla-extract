package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.TestData;
import cc.tweaked.vanillaextract.core.download.FileDownload;
import cc.tweaked.vanillaextract.core.support.MirrorDownloader;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static cc.tweaked.vanillaextract.core.minecraft.MinecraftVersionProviderTest.uri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class MinecraftProviderTest {
    @TempDir
    private Path dir;


    @Test
    public void testRawDownloadsFiles() throws IOException {
        var downloader = MirrorDownloader.createOffline();
        new MinecraftProvider(downloader).provideRaw(dir, TestData.MC_1_20_4, TestData.MC_1_20_4_CLIENT_LIBRARIES);

        assertIterableEquals(List.of(
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar"), dir.resolve("client-fd19469fed4a4b4c15b2d5133985f0e3e7816a8a.jar"), "fd19469fed4a4b4c15b2d5133985f0e3e7816a8a", false),
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/be76ecc174ea25580bdc9bf335481a5192d9f3b7/client.txt"), dir.resolve("client-be76ecc174ea25580bdc9bf335481a5192d9f3b7.txt"), "be76ecc174ea25580bdc9bf335481a5192d9f3b7", false),
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/8dd1a28015f51b1803213892b50b7b4fc76e594d/server.jar"), dir.resolve("server-8dd1a28015f51b1803213892b50b7b4fc76e594d.jar"), "8dd1a28015f51b1803213892b50b7b4fc76e594d", false),
            new FileDownload(uri("https://piston-data.mojang.com/v1/objects/c1cafe916dd8b58ed1fe0564fc8f786885224e62/server.txt"), dir.resolve("server-c1cafe916dd8b58ed1fe0564fc8f786885224e62.txt"), "c1cafe916dd8b58ed1fe0564fc8f786885224e62", false)
        ), downloader.takeDownloads());
    }

    @Test
    public void testRawClientLibraries() throws IOException {
        var rawArtifacts = new MinecraftProvider(MirrorDownloader.createOffline()).provideRaw(dir, TestData.MC_1_20_4, TestData.MC_1_20_4_CLIENT_LIBRARIES);

        assertIterableEquals(
            rawArtifacts.client().dependencies(),
            List.of(
                "ca.weblite:java-objc-bridge:1.1",
                "com.github.oshi:oshi-core:6.4.5",
                "com.google.code.gson:gson:2.10.1",
                "com.google.guava:failureaccess:1.0.1",
                "com.google.guava:guava:32.1.2-jre",
                "com.ibm.icu:icu4j:73.2",
                "com.mojang:authlib:6.0.52",
                "com.mojang:blocklist:1.0.10",
                "com.mojang:brigadier:1.2.9",
                "com.mojang:datafixerupper:6.0.8",
                "com.mojang:logging:1.1.1",
                "com.mojang:patchy:2.2.10",
                "com.mojang:text2speech:1.17.9",
                "commons-codec:commons-codec:1.16.0",
                "commons-io:commons-io:2.13.0",
                "commons-logging:commons-logging:1.2",
                "io.netty:netty-buffer:4.1.97.Final",
                "io.netty:netty-codec:4.1.97.Final",
                "io.netty:netty-common:4.1.97.Final",
                "io.netty:netty-handler:4.1.97.Final",
                "io.netty:netty-resolver:4.1.97.Final",
                "io.netty:netty-transport-classes-epoll:4.1.97.Final",
                "io.netty:netty-transport-native-epoll:4.1.97.Final:linux-aarch_64",
                "io.netty:netty-transport-native-epoll:4.1.97.Final:linux-x86_64",
                "io.netty:netty-transport-native-unix-common:4.1.97.Final",
                "io.netty:netty-transport:4.1.97.Final",
                "it.unimi.dsi:fastutil:8.5.12",
                "net.java.dev.jna:jna-platform:5.13.0",
                "net.java.dev.jna:jna:5.13.0",
                "net.sf.jopt-simple:jopt-simple:5.0.4",
                "org.apache.commons:commons-compress:1.22",
                "org.apache.commons:commons-lang3:3.13.0",
                "org.apache.httpcomponents:httpclient:4.5.13",
                "org.apache.httpcomponents:httpcore:4.4.16",
                "org.apache.logging.log4j:log4j-api:2.19.0",
                "org.apache.logging.log4j:log4j-core:2.19.0",
                "org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0",
                "org.joml:joml:1.10.5",
                "org.lwjgl:lwjgl-glfw:3.3.2",
                "org.lwjgl:lwjgl-jemalloc:3.3.2",
                "org.lwjgl:lwjgl-openal:3.3.2",
                "org.lwjgl:lwjgl-opengl:3.3.2",
                "org.lwjgl:lwjgl-stb:3.3.2",
                "org.lwjgl:lwjgl-tinyfd:3.3.2",
                "org.lwjgl:lwjgl:3.3.2",
                "org.slf4j:slf4j-api:2.0.7"
            ),
            "Client dependencies are the same"
        );
    }

    @Test
    public void testRawFingerprints() throws IOException {
        var rawArtifacts = new MinecraftProvider(MirrorDownloader.createOffline()).provideRaw(dir, TestData.MC_1_20_4, TestData.MC_1_20_4_CLIENT_LIBRARIES);

        // Check fingerprints have expected fingerprints
        assertEquals(TestData.MC_1_20_4.client().sha1(), rawArtifacts.client().jar().digest());
        assertEquals(TestData.MC_1_20_4.client_mappings().sha1(), rawArtifacts.client().mappings().digest());
        assertEquals("4451682405756cc760b389e9ab6223ae01b39f47", rawArtifacts.server().jar().digest());
        assertEquals(TestData.MC_1_20_4.server_mappings().sha1(), rawArtifacts.server().mappings().digest());

        // And check our fingerprints match what's on the file
        for (var digest : List.of(
            rawArtifacts.client().jar(), rawArtifacts.client().mappings(),
            rawArtifacts.server().jar(), rawArtifacts.server().mappings()
        )) {
            assertEquals(MoreFiles.getSha1(digest.path()), digest.digest(), "Digest matches for " + digest.path());
            assertEquals(MoreFiles.computeSha1(digest.path()), digest.digest(), "Digest matches for " + digest.path());
        }
    }

    @Test
    public void testSplitFingerprints() throws IOException {
        var provider = new MinecraftProvider(MirrorDownloader.createOffline());
        var rawArtifacts = provider.provideRaw(dir, TestData.MC_1_20_4, TestData.MC_1_20_4_CLIENT_LIBRARIES);
        var splitArtifacts = provider.provideSplit(dir, rawArtifacts, false);

        // Check the conversion is deterministic
        assertEquals("f9720e74c211a561fdcf6ca97862ff41d195d737", splitArtifacts.client().jar().digest());
        assertEquals(TestData.MC_1_20_4.client_mappings().sha1(), splitArtifacts.client().mappings().digest());
        assertEquals("c5575a21a90ca32a645d1aab7084cbd168c583d7", splitArtifacts.common().jar().digest());
        assertEquals(TestData.MC_1_20_4.server_mappings().sha1(), splitArtifacts.common().mappings().digest());

        // And check our fingerprints match the file's contents.
        for (var digest : List.of(
            splitArtifacts.client().jar(), splitArtifacts.client().mappings(),
            splitArtifacts.common().jar(), splitArtifacts.common().mappings()
        )) {
            assertEquals(MoreFiles.getSha1(digest.path()), digest.digest(), "Digest matches for " + digest.path());
            assertEquals(MoreFiles.computeSha1(digest.path()), digest.digest(), "Digest matches for " + digest.path());
        }
    }
}
