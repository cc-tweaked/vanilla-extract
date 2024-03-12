package cc.tweaked.vanillaextract.core.minecraft;

import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public class TransformedMinecraftProviderTest {
    @TempDir
    private Path dir;

    /*
    @Test
    public void transformOfficial() throws IOException {
        TransformedMinecraftProvider.provide(
            "1.20.4", MinecraftProviderTest.setupMinecraft(dir, MirrorDownloader.createOffline()),
            MojangMappings.get(),
            List.of(),
            DependencyResolver.unsupported(),
            dir
        );

        assertTrue(
            Files.isRegularFile(dir.resolve("net/minecraft/minecraft-common/1.20.4-471850e00f249c93/minecraft-common-1.20.4-471850e00f249c93.jar")),
            "Jar was created"
        );
    }

    @Test
    public void transformParchment() throws IOException {
        TransformedMinecraftProvider.provide(
            "1.20.4", MinecraftProviderTest.setupMinecraft(dir, MirrorDownloader.createOffline()),
            new ParchmentMappings("1.20.3", "2023.12.31"),
            List.of(),
            new DownloadingDependencyResolver("https://maven.parchmentmc.org/", dir, MirrorDownloader.createWithFallback()),
            dir
        );
    }
    */
}
