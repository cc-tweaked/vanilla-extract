package cc.tweaked.vanillaextract.utils;

import cc.tweaked.vanillaextract.core.util.Pair;
import org.gradle.api.Transformer;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import java.util.function.BiFunction;

/**
 * Additional functions for working with {@link Provider}s.
 */
public final class Providers {
    private Providers() {
    }

    /**
     * Zip four providers together.
     *
     * @param p1       The first provider.
     * @param p2       The second provider.
     * @param p3       The third provider.
     * @param p4       The fourth provider.
     * @param function The function to apply to the providers' values.
     * @return The zipped provider.
     */
    public static <T1, T2, T3, T4, R> Provider<R> zip(Provider<T1> p1, Provider<T2> p2, Provider<T3> p3, Provider<T4> p4, QuadFunction<T1, T2, T3, T4, R> function) {
        return p1.zip(p2, Pair::new).zip(
            p3.zip(p4, Pair::new),
            (value12, value34) -> function.apply(value12.first(), value12.second(), value34.first(), value34.second())
        );
    }

    /**
     * Cache a provider by creating a property and binding to it.
     * <p>
     * In some cases (such as {@link Provider#map(Transformer)} or {@link Provider#zip(Provider, BiFunction)}),
     * a provider's value will be re-computed every time {@link Provider#get()} is called. This may be undesirable, for
     * instance if computing the value is expensive or involves IO.
     * <p>
     * To avoid this, we can create a new property, bind it to the provider, and then force it to be
     * {@linkplain Property#finalizeValueOnRead() only computed once}. This effectively memoises/caches the provider,
     * while still preserving its lazy behaviour.
     *
     * @param objects  The {@link ObjectFactory} instance.
     * @param type     The type of the provider's contents.
     * @param provider The provider to wrap.
     * @param <T>      The type of the provider's contents.
     * @return An equivalent provider, which only computes its value once.
     */
    public static <T> Provider<T> cacheViaProperty(ObjectFactory objects, Class<T> type, Provider<T> provider) {
        var prop = objects.property(type);
        prop.set(provider);
        prop.disallowChanges();
        prop.disallowUnsafeRead();
        prop.finalizeValueOnRead();
        return prop;
    }

    @FunctionalInterface
    public interface QuadFunction<T1, T2, T3, T4, R> {
        R apply(T1 a, T2 b, T3 c, T4 d);
    }
}
