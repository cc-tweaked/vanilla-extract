package cc.tweaked.vanillaextract.core.unpick;

import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.Nullable;

/**
 * Remap an unpick definition from one file to another.
 */
public final class UnpickRemapper implements UnpickV2Reader.Visitor {
    private final MemoryMappingTree mappings;
    private final int src, dest;
    private final UnpickV2Reader.Visitor visitor;

    public UnpickRemapper(
        MemoryMappingTree mappings,
        String srcNamepace, String destNamespace,
        UnpickV2Reader.Visitor visitor
    ) {
        this.mappings = mappings;
        this.src = mappings.getNamespaceId(srcNamepace);
        this.dest = mappings.getNamespaceId(destNamespace);
        this.visitor = visitor;
    }

    @Override
    public void startVisit() {
        visitor.startVisit();
    }

    @Override
    public void visitSimpleConstantDefinition(String group, String owner, String name, @Nullable String value, @Nullable String descriptor) {
        var field = mappings.getField(owner, name, null, src);
        if (field == null) {
            visitor.visitSimpleConstantDefinition(group, owner, name, value, descriptor);
        } else {
            visitor.visitSimpleConstantDefinition(
                group, field.getOwner().getName(dest), field.getName(dest), value, descriptor == null ? null : field.getDesc(dest)
            );
        }
    }

    @Override
    public void visitFlagConstantDefinition(String group, String owner, String name, @Nullable String value, @Nullable String descriptor) {
        var field = mappings.getField(owner, name, null, src);
        if (field == null) {
            visitor.visitFlagConstantDefinition(group, owner, name, value, descriptor);
        } else {
            visitor.visitFlagConstantDefinition(
                group, field.getOwner().getName(dest), field.getName(dest), value, descriptor == null ? null : field.getDesc(dest)
            );
        }
    }

    @Override
    public UnpickV2Reader.TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor) {
        var method = mappings.getMethod(owner, name, descriptor, src);
        return method == null
            ? visitor.visitTargetMethodDefinition(owner, name, descriptor)
            : visitor.visitTargetMethodDefinition(method.getOwner().getName(dest), method.getName(dest), method.getDesc(dest));
    }

    @Override
    public void endVisit() {
        visitor.endVisit();
    }
}
