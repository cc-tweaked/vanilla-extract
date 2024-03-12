package cc.tweaked.vanillaextract.core.mappings;

import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;

import java.io.IOException;
import java.util.List;

/**
 * Provides mappings for deobfuscating Minecraft.
 * <p>
 * {@link MappingProvider}s are a specification of the mappings used to decompile Minecraft, and how to acquire them.
 * For instance, {@linkplain MojangMappings the MojMap provider} uses {@linkplain Context#builtinMappings() the
 * built-in} mappings, while {@linkplain ParchmentMappings parchment} reads them from a file.
 * <p>
 * However, {@link MappingProvider}s do not read the mapping files directly. Instead, there is a two-step process, where
 * mappings are first {@linkplain #resolve(Context) resolved}. This takes a snapshot of all inputs to the mappings, such
 * as {@linkplain FileFingerprint file fingerprints}.
 * <p>
 * The returned {@link ResolvedMappings}s is then used to actually populate the mappings.
 * <p>
 * Once mappings are populated, they're then {@link MappingsFileProvider written to an intermediate file} in a
 * content-addressed cache.
 *
 * @see MojangMappings
 * @see ParchmentMappings
 */
public interface MappingProvider {
    /**
     * Resolve these mappings to an immutable snapshot.
     *
     * @param context The current mapping context.
     * @return The resolved mappings.
     * @throws IOException IO errors when trying to read/prepare mappings.
     */
    ResolvedMappings resolve(Context context) throws IOException;

    /**
     * The context under which {@link MappingProvider}s are resolved.
     *
     * @param builtinMappings The builtin mappings bundled with Minecraft.
     * @param fingerprint     The function used to fingerprint input files.
     */
    record Context(
        List<FileFingerprint> builtinMappings,
        FileFingerprint.Provider fingerprint
    ) {
    }
}
