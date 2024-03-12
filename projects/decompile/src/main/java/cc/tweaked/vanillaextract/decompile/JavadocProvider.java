package cc.tweaked.vanillaextract.decompile;

import org.jetbrains.annotations.Nullable;

/**
 * Provides Javadoc to the decompiler.
 *
 * @see Decompiler.Parameters#javadoc()
 */
public interface JavadocProvider {
    /**
     * Get Javadoc for a class.
     *
     * @param name The name of the class.
     * @return The javadoc, if known.
     */
    @Nullable String getClassDoc(String name);

    /**
     * Get Javadoc for a method.
     *
     * @param owner      The name of the owning class.
     * @param name       The name of the method.
     * @param descriptor The method's descriptor.
     * @return The javadoc, if known.
     */
    @Nullable String getMethodDoc(String owner, String name, String descriptor);

    /**
     * Get Javadoc for a field.
     *
     * @param owner      The name of the owning class.
     * @param name       The name of the field.
     * @param descriptor The field's descriptor.
     * @return The javadoc, if known.
     */
    @Nullable String getFieldDocs(String owner, String name, String descriptor);

    /**
     * Get a Javadoc provider which always returns {@code null}.
     *
     * @return The empty javadoc provider.
     */
    static JavadocProvider none() {
        return EmptyJavadocProvider.instance;
    }

    /**
     * A javadoc provider which always returns null.
     *
     * @see #none()
     */
    class EmptyJavadocProvider implements JavadocProvider {
        private static final EmptyJavadocProvider instance = new EmptyJavadocProvider();

        @Override
        public @Nullable String getClassDoc(String name) {
            return null;
        }

        @Override
        public @Nullable String getMethodDoc(String owner, String name, String descriptor) {
            return null;
        }

        @Override
        public @Nullable String getFieldDocs(String owner, String name, String descriptor) {
            return null;
        }
    }
}
