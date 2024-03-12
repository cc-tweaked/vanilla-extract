package cc.tweaked.vanillaextract.core.util;

/**
 * An ordered pair of values.
 *
 * @param first  The first value in the pair.
 * @param second The second value in the pair.
 * @param <L>    The first value.
 * @param <R>    The second value.
 */
public record Pair<L, R>(L first, R second) {
}
