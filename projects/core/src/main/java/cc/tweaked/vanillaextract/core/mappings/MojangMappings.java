package cc.tweaked.vanillaextract.core.mappings;

import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.inputs.InputCollector;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.format.proguard.ProGuardFileReader;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Use the official mappings bundled with Minecraft.
 */
public final class MojangMappings implements MappingProvider {
    private static final MojangMappings instance = new MojangMappings();

    private MojangMappings() {
    }

    public static MojangMappings get() {
        return instance;
    }

    @Override
    public String toString() {
        return "Mojang";
    }

    @Override
    public ResolvedMappings resolve(Context context) {
        return new Resolved(context.builtinMappings());
    }

    private record Resolved(List<FileFingerprint> mappings) implements ResolvedMappings {
        @Override
        public void addInputs(InputCollector collector) {
            for (var mapping : mappings) collector.addInput(mapping);
        }

        @Override
        public void accept(MappingVisitor visitor) throws IOException {
            for (var mapping : this.mappings) {
                try (var serverBufferedReader = Files.newBufferedReader(mapping.path())) {
                    ProGuardFileReader.read(serverBufferedReader, MappingNamespaces.WORKSPACE, MappingNamespaces.OFFICIAL, visitor);
                }
            }
        }
    }
}
