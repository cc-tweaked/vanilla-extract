package cc.tweaked.vanillaextract.core.mappings;

import net.fabricmc.mappingio.MappedElementKind;
import net.fabricmc.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * The contents of a parchment file.
 *
 * @param version  The version number, assumed to be {@code 1.x}.
 * @param packages Additional information about packages.
 * @param classes  Additional information about classes.
 * @see ParchmentMappings
 */
record ParchmentData(
    String version,
    @Nullable List<PackageInfo> packages,
    @Nullable List<ClassInfo> classes
) {
    public void visit(MappingVisitor visitor, String namespace) throws IOException {
        do {
            if (visitor.visitHeader()) visitor.visitNamespaces(namespace, Collections.emptyList());
            if (visitor.visitContent()) MappingSource.acceptAll(classes(), visitor);
        } while (!visitor.visitEnd());
    }

    public record PackageInfo(String name, List<String> javadoc) {
    }

    public record ClassInfo(
        String name,
        @Nullable List<String> javadoc,
        @Nullable List<MethodInfo> methods,
        @Nullable List<FieldInfo> fields
    ) implements MappingSource {
        @Override
        public void accept(MappingVisitor visitor) throws IOException {
            if (!visitor.visitClass(name())) return;
            if (!visitor.visitElementContent(MappedElementKind.CLASS)) return;

            visitJavadoc(visitor, MappedElementKind.CLASS, javadoc());
            MappingSource.acceptAll(fields(), visitor);
            MappingSource.acceptAll(methods(), visitor);
        }
    }

    public record MethodInfo(
        String name, String descriptor, @Nullable List<String> javadoc, @Nullable List<ParameterInfo> parameters
    ) implements MappingSource {
        @Override
        public void accept(MappingVisitor visitor) throws IOException {
            if (!visitor.visitMethod(name(), descriptor())) return;
            if (!visitor.visitElementContent(MappedElementKind.METHOD)) return;

            visitJavadoc(visitor, MappedElementKind.METHOD, javadoc());
            MappingSource.acceptAll(parameters(), visitor);
        }
    }

    public record ParameterInfo(int index, String name, @Nullable String javadoc) implements MappingSource {
        @Override
        public void accept(MappingVisitor visitor) throws IOException {
            if (!visitor.visitMethodArg(index, index, name)) return;
            if (!visitor.visitElementContent(MappedElementKind.METHOD_ARG)) return;

            if (javadoc() != null) visitor.visitComment(MappedElementKind.METHOD_ARG, javadoc);
        }
    }

    public record FieldInfo(
        String name,
        String descriptor,
        @Nullable List<String> javadoc
    ) implements MappingSource {
        @Override
        public void accept(MappingVisitor visitor) throws IOException {
            if (!visitor.visitField(name, descriptor)) return;
            if (!visitor.visitElementContent(MappedElementKind.FIELD)) return;

            visitJavadoc(visitor, MappedElementKind.FIELD, javadoc());
        }
    }

    private static void visitJavadoc(MappingVisitor visitor, MappedElementKind kind, @Nullable List<String> javadoc) throws IOException {
        if (javadoc != null) visitor.visitComment(kind, String.join("\n", javadoc));
    }
}
