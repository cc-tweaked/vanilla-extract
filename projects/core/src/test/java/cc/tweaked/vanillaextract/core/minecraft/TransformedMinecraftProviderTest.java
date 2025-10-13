package cc.tweaked.vanillaextract.core.minecraft;

import cc.tweaked.vanillaextract.core.TestData;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.mappings.MappingsFileProvider;
import cc.tweaked.vanillaextract.core.mappings.MojangMappings;
import cc.tweaked.vanillaextract.core.support.MirrorDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransformedMinecraftProviderTest {
    @TempDir
    private Path dir;


    @Test
    public void transformOfficial() throws IOException {
        var minecraft = TestData.setupMinecraft(dir, MirrorDownloader.createOffline());

        var mappings = MojangMappings.get().resolve(new MappingProvider.Context(minecraft.mappings(), FileFingerprint::createDefault));
        var mappingsFile = new MappingsFileProvider(dir).saveMappings("1.20.4", mappings);

        var transformed = new TransformedMinecraftProvider(dir).provide("1.20.4", minecraft, mappingsFile, List.of(), false);
        assertEquals(
            dir.resolve("net/minecraft/minecraft-common/1.20.4-ee691eab37317d70/minecraft-common-1.20.4-ee691eab37317d70.jar"),
            transformed.common().path()
        );
    }

    /*@Test
    public void transformParchment() throws IOException {
        TransformedMinecraftProvider.provide(
            "1.20.4", MinecraftProviderTest.setupMinecraft(dir, MirrorDownloader.createOffline()),
            new ParchmentMappings("1.20.3", "2023.12.31"),
            List.of(),
            new DownloadingDependencyResolver("https://maven.parchmentmc.org/", dir, MirrorDownloader.createWithFallback()),
            dir
        );
    }*/
}
