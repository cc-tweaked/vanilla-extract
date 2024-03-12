package cc.tweaked.vanillaextract.core;

import java.nio.file.Path;

/**
 * A release in a maven repository.
 * <p>
 * This represents a single version of a module, such as {@code org.example:module:1.0.0}.
 *
 * @param group   The group of this release.
 * @param module  The name of this release.
 * @param version The version of this release.
 */
public record MavenRelease(String group, String module, String version) {
    /**
     * Convert this module to a string coordinate, suitable to be used as a Gradle dependency.
     *
     * @return This module's maven coordinate.
     */
    public String coordinate() {
        return group + ":" + module + ":" + version;
    }

    /**
     * Get the directory where this module would be stored.
     *
     * @param root The root of the maven repository.
     * @return This module's directory.
     */
    public Path getDirectoryLocation(Path root) {
        return root.resolve(group.replace('.', '/')).resolve(module).resolve(version);
    }

    /**
     * Get the file where the main jar of this module would be stored.
     *
     * @param root The root of the maven repository.
     * @return This module's main jar.
     */
    public Path getJarLocation(Path root) {
        return getFileLocation(root, "", "jar");
    }

    /**
     * Get the file where the pom of this module would be stored.
     *
     * @param root The root of the maven repository.
     * @return This module's pom.
     */
    public Path getPomLocation(Path root) {
        return getFileLocation(root, "", "pom");
    }

    /**
     * Get the path to a file for this module.
     *
     * @param root       The root of the maven repository.
     * @param classifier The classifier of this file.
     * @param ext        The file extension of this file, without the {@code .}.
     * @return The path to the requested file.
     */
    public Path getFileLocation(Path root, String classifier, String ext) {
        return getDirectoryLocation(root).resolve(module + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + ext);
    }
}
