package cc.tweaked.vanillaextract.configurations;

import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.tasks.SourceSet;

/**
 * Capabilities which describe which "side" a module's classes are available for.
 *
 * @see MinecraftSetup#setupOutgoingConfigurations() A deeper explanation of how we use capabilities.
 */
public class Capabilities {
    /**
     * Classes "common" to the client and server, provided by the {@linkplain SourceSet#MAIN_SOURCE_SET_NAME main source
     * set}.
     */
    public static final String COMMON = "common";

    /**
     * Classes exclusive to the client, provided by the {@linkplain MinecraftSetup#CLIENT_SOURCE_SET_NAME client source
     * set}.
     */
    public static final String CLIENT = "client";

    /**
     * Configure the passed dependency to use client classes.
     *
     * @param dependency The dependency to configure.
     * @return The same dependency as provided.
     */
    public static ModuleDependency clientClasses(ModuleDependency dependency) {
        return dependency.capabilities(c -> c.requireCapability(new DependencyCapability(dependency, CLIENT)));
    }

    /**
     * Configure the passed dependency to use common classes.
     *
     * @param dependency The dependency to configure.
     * @return The same dependency as provided.
     */
    public static ModuleDependency commonClasses(ModuleDependency dependency) {
        return dependency.capabilities(c -> c.requireCapability(new DependencyCapability(dependency, COMMON)));
    }
}
