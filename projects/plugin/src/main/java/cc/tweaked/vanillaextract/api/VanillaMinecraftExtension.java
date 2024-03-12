package cc.tweaked.vanillaextract.api;

import org.gradle.api.Action;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.provider.Provider;

/**
 * The main {@code minecraft} extension for vanilla Minecraft projects.
 */
public interface VanillaMinecraftExtension {
    /**
     * Set the Minecraft version for this project.
     *
     * @param version The property which can be used by it.
     */
    void version(String version);

    /**
     * Set the Minecraft version for this project.
     *
     * @param version The property which can be used by it.
     */
    void version(Provider<String> version);

    /**
     * Apply access wideners to the Minecraft classes.
     *
     * @param file The access wideners. These must be convertible using {@link org.gradle.api.Project#file(Object)}.
     */
    void accessWideners(Object... file);

    /**
     * Configure which mappings provider to use.
     *
     * @param configure The function to configure our mappings.
     */
    void mappings(Action<? super MappingsConfiguration> configure);

    /**
     * Un-inline constants when decompiling using Fabric's Unpick tool.
     * <p>
     * We assume the dependency provides a jar with the {@code -mergedv2} classifier (providing the Unpick definitions,
     * and a way to remap them) and a {@code -constants} jar, used to supply additional constants.
     *
     * @param dependency The dependency which provides unpick configuration.
     */
    void unpick(Object dependency);

    /*
     * Un-inline constants when decompiling using Fabric's Unpick tool.
     * <p>
     * See {@link #unpick(Object)} for further details.
     *
     * @param dependency The dependency which provides unpick configuration.
     */
    void unpick(Provider<? extends ModuleDependency> dependency);
}
