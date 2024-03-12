package cc.tweaked.vanillaextract.utils;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.provider.Provider;

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
}
