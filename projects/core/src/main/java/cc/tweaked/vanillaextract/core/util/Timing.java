package cc.tweaked.vanillaextract.core.util;

public class Timing {
    /**
     * Format the duration of an operation in a user-friendly manner.
     *
     * @param ns The duration, in nanoseconds.
     * @return The user-friendly string.
     */
    public static String formatDuration(long ns) {
        if (ns >= 1_000_000_000) return String.format("%.2fs", ns * 1e-9);
        if (ns >= 1_000_000) return String.format("%.2fms", ns * 1e-6);
        if (ns >= 1_000) return String.format("%.2fus", ns * 1e-3);
        return Long.toString(ns);
    }

    /**
     * Format the time elapsed since an operation started in a user-friendly manner.
     *
     * @param start The start time of this operation, as returned by {@link System#nanoTime()}.
     * @return The user-friendly string.
     */
    public static String formatSince(long start) {
        return formatDuration(System.nanoTime() - start);
    }
}
