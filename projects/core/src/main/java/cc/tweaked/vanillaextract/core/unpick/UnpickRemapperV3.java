package cc.tweaked.vanillaextract.core.unpick;

import daomephsta.unpick.constantmappers.datadriven.parser.v3.UnpickV3Remapper;
import daomephsta.unpick.constantmappers.datadriven.tree.UnpickV3Visitor;
import net.fabricmc.mappingio.tree.MemoryMappingTree;

import java.util.List;
import java.util.Objects;

/**
 * Remap an unpick definition from one file to another.
 */
final class UnpickRemapperV3 extends UnpickV3Remapper {
    private final MemoryMappingTree mappings;
    private final int src, dest;

    UnpickRemapperV3(
        MemoryMappingTree mappings,
        String srcNamepace, String destNamespace,
        UnpickV3Visitor visitor
    ) {
        super(visitor);
        this.mappings = mappings;
        this.src = mappings.getNamespaceId(srcNamepace);
        this.dest = mappings.getNamespaceId(destNamespace);
    }

    @Override
    protected String mapClassName(String className) {
        return mappings.mapClassName(className.replace('.', '/'), src, dest).replace('/', '.');
    }

    @Override
    protected String mapFieldName(String className, String fieldName, String fieldDesc) {
        var field = mappings.getField(className.replace('.', '/'), fieldName, null, src);
        if (field == null) return fieldName;

        var destName = field.getDstName(dest);
        return destName == null ? fieldName : destName;
    }

    @Override
    protected String getFieldDesc(String className, String fieldName) {
        return "unknown"; // This is only used for mapFieldName, so can be ignored.
    }

    @Override
    protected String mapMethodName(String className, String methodName, String methodDesc) {
        var method = mappings.getMethod(className.replace('.', '/'), methodName, methodDesc, src);
        if (method == null) return methodName;

        var destName = method.getDstName(dest);
        return destName == null ? methodName : destName;
    }

    @Override
    protected List<String> getClassesInPackage(String pkg) {
        return mappings.getClasses().stream().map(x -> x.getDstName(dest)).filter(Objects::nonNull).toList();
    }
}
