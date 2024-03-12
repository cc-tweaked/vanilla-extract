package cc.tweaked.vanillaextract.core.mappings;

import net.fabricmc.mappingio.MappingVisitor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A value which can populate a {@link MappingVisitor}.
 */
public interface MappingSource {
    /**
     * Visit this value with a mapping visitor, populating its mappings.
     *
     * @param visitor The visitor to populate.
     * @throws IOException If the underlying {@link MappingVisitor} throws.
     */
    void accept(MappingVisitor visitor) throws IOException;

    /**
     * Visit a sequence of values.
     *
     * @param list    The list of values to visit.
     * @param visitor The visitor to visit with.
     * @throws IOException If the underlying {@link MappingVisitor} throws.
     */
    static void acceptAll(@Nullable Iterable<? extends MappingSource> list, MappingVisitor visitor) throws IOException {
        if (list == null) return;
        for (var x : list) x.accept(visitor);
    }
}
