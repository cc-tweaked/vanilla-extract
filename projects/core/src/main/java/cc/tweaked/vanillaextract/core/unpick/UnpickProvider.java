package cc.tweaked.vanillaextract.core.unpick;

import cc.tweaked.vanillaextract.core.mappings.MappingNamespaces;
import cc.tweaked.vanillaextract.core.mappings.MappingsFileProvider;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Writer;
import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingNsRenamer;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.tinyremapper.FileSystemReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class UnpickProvider {
    public static void provideUnpick(Path mappings, Path unpick, Path output) throws IOException {
        try (var loomFs = FileSystemReference.openJar(unpick)) {
            // Read official, intermediary and yarn from the unpick jar.
            var mappingTree = new MemoryMappingTree();
            MappingReader.read(
                loomFs.getPath("mappings/mappings.tiny"), MappingFormat.TINY_2_FILE,
                new MappingNsRenamer(mappingTree, Map.of("named", "yarn"))
            );

            // Then read in our current mappings, which includes official and the workspace.
            MappingsFileProvider.readMappings(mappings, new MappingSourceNsSwitch(mappingTree, "official"));

            // This gives us a link of yarn ---> workspace, so we can then remap the unpick file.
            var writer = new UnpickV2Writer();
            try (var reader = new UnpickV2Reader(Files.newInputStream(loomFs.getPath("extras/definitions.unpick")))) {
                reader.accept(new UnpickRemapper(mappingTree, "yarn", MappingNamespaces.WORKSPACE, writer));
            }
            Files.writeString(output, writer.getOutput());
        }
    }
}
