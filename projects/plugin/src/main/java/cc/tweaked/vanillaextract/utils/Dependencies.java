package cc.tweaked.vanillaextract.utils;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;

import java.util.function.Consumer;

/**
 * Additional functions for working with {@linkplain Dependency dependencies}.
 */
public final class Dependencies {
    private Dependencies() {
    }

    /**
     * Create a detached configuration with a single dependency.
     *
     * @param configurations The current configuration container.
     * @param description    The description of this configuration.
     * @param dependency     The dependency to resolve.
     * @return The configuration.
     */
    public static Configuration createDetachedConfiguration(ConfigurationContainer configurations, String description, Dependency dependency) {
        var configuration = configurations.detachedConfiguration(dependency);
        setupConfiguration(configuration, description);
        return configuration;
    }

    /**
     * Create a detached configuration with a single (provider-based) dependency.
     *
     * @param configurations The current configuration container.
     * @param description    The description of this configuration.
     * @param dependency     The dependency to resolve.
     * @return The configuration.
     */
    public static Configuration createDetachedConfiguration(ConfigurationContainer configurations, String description, Provider<? extends Dependency> dependency) {
        var configuration = configurations.detachedConfiguration();
        setupConfiguration(configuration, description);
        configuration.getDependencies().addLater(dependency);
        return configuration;
    }

    private static void setupConfiguration(Configuration configuration, String description) {
        configuration.setDescription(description);
        configuration.setVisible(false);
        configuration.setCanBeConsumed(false);
        configuration.setCanBeResolved(true);
    }

    /**
     * Create a provider that resolves a configuration to a single file.
     *
     * @param configuration The configuration to resolve.
     * @return The provider returning the configuration's file.
     * @see Configuration#getSingleFile()
     */
    public static Provider<FileSystemLocation> getSingleFile(Configuration configuration) {
        return configuration.getElements().map(x -> switch (x.size()) {
            case 0 ->
                throw new IllegalStateException(String.format("Expected %s to contain exactly one file, however, it contains no files.", configuration.getName()));
            case 1 -> x.iterator().next();
            default ->
                throw new IllegalStateException(String.format("Expected %s to contain exactly one file, however, it contains more than one file.", configuration.getName()));
        });
    }

    /**
     * Create a new dependency with some modifications.
     *
     * @param dependency The dependency to resolve.
     * @param modify     A function to modify a dependency.
     * @param <T>        The type of the dependency.
     * @return The modified dependency.
     */
    public static <T extends ModuleDependency> Provider<T> with(Provider<T> dependency, Consumer<T> modify) {
        return dependency.map(x -> {
            @SuppressWarnings("unchecked") var dep = (T) x.copy();
            modify.accept(dep);
            return dep;
        });
    }

    /**
     * Create a new dependency with a different classifier.
     *
     * @param dependency The dependency to resolve.
     * @param classifier The new classifier.
     * @param <T>        The type of the dependency.
     * @return The modified dependency.
     */
    public static <T extends ModuleDependency> Provider<T> withClassifier(Provider<T> dependency, String classifier) {
        return with(dependency, dep -> dep.artifact(a -> a.setClassifier(classifier)));
    }
}
