package cc.tweaked.vanillaextract.decompile;

import net.fabricmc.mappingio.tree.MappingTreeView;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link JavadocProvider} which provides comments from mapping information.
 */
public class MappingsJavadocProvider implements JavadocProvider {
    private final MappingTreeView mappings;

    public MappingsJavadocProvider(MappingTreeView mappings) {
        this.mappings = mappings;
    }

    @Override
    public @Nullable String getClassDoc(String name) {
        var node = mappings.getClass(name);
        return node == null ? null : node.getComment();
    }

    @Override
    public @Nullable String getMethodDoc(String owner, String name, String descriptor) {
        var node = mappings.getMethod(owner, name, descriptor);
        return node == null ? null : node.getComment();
    }

    @Override
    public @Nullable String getFieldDocs(String owner, String name, String descriptor) {
        var node = mappings.getField(owner, name, descriptor);
        return node == null ? null : node.getComment();
    }
}
