package cc.tweaked.vanillaextract.core.unpick;

import cc.tweaked.vanillaextract.core.TestData;
import cc.tweaked.vanillaextract.core.download.FileDownload;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.mappings.MappingsFileProvider;
import cc.tweaked.vanillaextract.core.mappings.MojangMappings;
import cc.tweaked.vanillaextract.core.minecraft.MinecraftProvider;
import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion;
import cc.tweaked.vanillaextract.core.support.MirrorDownloader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnpickProviderTest {
    private static final String YARN_1_20_4 = "https://maven.fabricmc.net/net/fabricmc/yarn/1.20.4%2Bbuild.3/yarn-1.20.4%2Bbuild.3-mergedv2.jar";
    private static final String YARN_1_21_10 = "https://maven.fabricmc.net/net/fabricmc/yarn/1.21.10%2Bbuild.2/yarn-1.21.10%2Bbuild.2-mergedv2.jar";

    @TempDir
    private Path dir;

    private Path getMappings(MirrorDownloader downloader, MinecraftVersion.Downloads downloads) throws IOException {
        var minecraft = new MinecraftProvider(downloader).provideRaw(dir, downloads, List.of());
        var mappings = MojangMappings.get().resolve(new MappingProvider.Context(
            List.of(minecraft.client().mappings(), minecraft.server().mappings()),
            FileFingerprint::createDefault
        ));
        return new MappingsFileProvider(dir).saveMappings("1.20.4", mappings).path();
    }

    @Test
    public void testRemapUnpickV2() throws IOException {
        var downloader = MirrorDownloader.createOffline();

        var unpickJar = dir.resolve("yarn.jar");
        FileDownload.builder(YARN_1_20_4, unpickJar).expectSha1("2eb65758cee8ee7476f1d2f9d26e42993d071ee8").download(downloader);

        var remapped = UnpickProvider.remapUnpick(getMappings(downloader, TestData.MC_1_20_4), unpickJar);
        assertEquals(Files.readString(Path.of("src/test/resources/unpick/1.20.4.txt")), remapped);
    }

    @Test
    public void testRemapUnpickV3() throws IOException {
        var downloader = MirrorDownloader.createOffline();

        var unpickJar = dir.resolve("yarn.jar");
        FileDownload.builder(YARN_1_21_10, unpickJar).expectSha1("8a87a8106cfba6ce109fa2dbe5be0c00eda79b19").download(downloader);

        var remapped = UnpickProvider.remapUnpick(getMappings(downloader, TestData.MC_1_21_10), unpickJar);
        assertEquals(Files.readString(Path.of("src/test/resources/unpick/1.21.10.txt")), remapped);
    }

    @Test
    public void testUnpick() throws IOException {
        var downloader = MirrorDownloader.createOffline();

        var unpickJar = dir.resolve("yarn.jar");
        FileDownload.builder(YARN_1_20_4, unpickJar).expectSha1("2eb65758cee8ee7476f1d2f9d26e42993d071ee8").download(downloader);

        var minecraft = TestData.setupMinecraft(dir, downloader);
        var mappings = MojangMappings.get().resolve(new MappingProvider.Context(minecraft.mappings(), FileFingerprint::createDefault));
        var mappingsFile = new MappingsFileProvider(dir).saveMappings("1.20.4", mappings);
        var transformed = new TransformedMinecraftProvider(dir).provide("1.20.4", minecraft, mappingsFile, List.of(), false);

        try (var unpick = new UnpickProvider(getMappings(downloader, TestData.MC_1_20_4), unpickJar, List.of(transformed.common().path()))) {
            unpick.unpick(transformed.common().path(), dir.resolve("unpicked.jar"));
        }
    }
}
