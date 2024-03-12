package cc.tweaked.vanillaextract.core;

import org.jetbrains.annotations.Nullable;

/**
 * A file in a maven repository.
 * <p>
 * This is a single file (described by a {@link #classifier()} and {@link #extension()}) belonging to a
 * {@linkplain MavenRelease release}. It may be used as a dependency.
 *
 * @param module     The module for this coordinate.
 * @param classifier The classifier for this file.
 * @param extension  The extension (or {@code type} in Maven terminology) of a
 */
public record MavenArtifact(MavenRelease module, @Nullable String classifier, @Nullable String extension) {
    public MavenArtifact(String group, String name, String version, @Nullable String classifier, @Nullable String extension) {
        this(new MavenRelease(group, name, version), classifier, extension);
    }

    /**
     * Create a {@link MavenArtifact} for the main jar.
     *
     * @param release The release to create it for.
     * @return The resulting artifact.
     */
    public static MavenArtifact main(MavenRelease release) {
        return new MavenArtifact(release, null, null);
    }

    /**
     * Parse a dependency string.
     *
     * @param dependency The dependency to parse.
     * @return The resulting artifact.
     * @throws IllegalArgumentException If the dependency cannot be parsed.
     * @see #toDependencyString()
     */
    public static MavenArtifact parse(String dependency) {
        var extIndex = dependency.lastIndexOf('@');
        String extension;
        if (extIndex > 0 && extIndex > dependency.lastIndexOf(':')) {
            extension = dependency.substring(extIndex + 1);
            dependency = dependency.substring(0, extIndex);
        } else {
            extension = null;
        }
        var parts = dependency.split(":");
        return switch (parts.length) {
            case 3 -> new MavenArtifact(parts[0], parts[1], parts[2], null, extension);
            case 4 -> new MavenArtifact(parts[0], parts[1], parts[2], parts[3], extension);
            default -> throw new IllegalArgumentException("Cannot parse " + dependency);
        };
    }

    public String group() {
        return module().group();
    }

    public String name() {
        return module().module();
    }

    public String version() {
        return module().version();
    }

    /**
     * Convert this artifact to a Gradle-style dependency string.
     *
     * @return The equivalent dependency string.
     * @see #parse(String)
     */
    public String toDependencyString() {
        var dependency = String.format("%s:%s:%s", group(), name(), version());
        if (classifier() != null) dependency += ":" + classifier();
        if (extension != null) dependency += "@" + extension;
        return dependency;
    }
}
