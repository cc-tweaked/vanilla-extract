package cc.tweaked.vanillaextract.core.unpick;

import daomephsta.unpick.constantmappers.datadriven.parser.v2.UnpickV2Reader;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.jetbrains.annotations.Nullable;

/**
 * Remap an unpick definition from one file to another.
 */
final class UnpickRemapperV2 implements UnpickV2Reader.Visitor {
    private final MemoryMappingTree mappings;
    private final int src, dest;
    private final UnpickV2Reader.Visitor visitor;

    UnpickRemapperV2(
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
        var ownerNode = mappings.getClass(owner, src);
        if (ownerNode != null) {
            owner = ownerNode.getDstName(dest);

            var field = ownerNode.getField(name, null, src);
            if (field != null) {
                name = field.getDstName(dest);
                descriptor = descriptor == null ? null : field.getDesc(dest);
            }
        }

        visitor.visitSimpleConstantDefinition(group, owner, name, value, descriptor);
    }

    @Override
    public void visitFlagConstantDefinition(String group, String owner, String name, @Nullable String value, @Nullable String descriptor) {
        var ownerNode = mappings.getClass(owner, src);
        if (ownerNode != null) {
            owner = ownerNode.getDstName(dest);

            var field = ownerNode.getField(name, null, src);
            if (field != null) {
                name = field.getDstName(dest);
                descriptor = descriptor == null ? null : field.getDesc(dest);
            }
        }

        visitor.visitFlagConstantDefinition(group, owner, name, value, descriptor);
    }

    @Override
    public UnpickV2Reader.TargetMethodDefinitionVisitor visitTargetMethodDefinition(String owner, String name, String descriptor) {
        var ownerNode = mappings.getClass(owner, src);
        if (ownerNode != null) {
            owner = ownerNode.getDstName(dest);

            var method = ownerNode.getMethod(name, descriptor, src);
            if (method != null) {
                name = method.getDstName(dest);
                descriptor = method.getDesc(dest);
            }
        }

        return visitor.visitTargetMethodDefinition(owner, name, descriptor);
    }

    @Override
    public void endVisit() {
        visitor.endVisit();
    }
}
