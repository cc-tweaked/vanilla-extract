package cc.tweaked.vanillaextract.core.mappings;

import cc.tweaked.vanillaextract.core.inputs.BuildInput;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.inputs.InputCollector;
import net.fabricmc.mappingio.MappingVisitor;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.nio.file.Path;

/**
 * A resolved collection of mappings used to deobfuscate Minecraft, provided by a {@link MappingProvider}.
 * <p>
 * This class represents an immutable snapshot of some mappings. For instance, rather than storing a {@link Path}, the
 * resolve mappings store a {@link FileFingerprint}. This allows us to convert the mappings to a digest/fingerprint
 * (via {@link ResolvedMappings#addInputs(InputCollector)}), and thus cache them.
 * <p>
 * Mappings are collected via {@link ResolvedMappings#accept(MappingVisitor)}. They are typically written to a
 * {@link MemoryMappingTree} first, allowing for easier merging of layers.
 */
public interface ResolvedMappings extends MappingSource, BuildInput {
}
