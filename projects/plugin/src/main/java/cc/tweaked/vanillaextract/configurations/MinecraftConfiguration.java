package cc.tweaked.vanillaextract.configurations;

import org.gradle.api.artifacts.Configuration;

/**
 * A {@link Configuration} that Minecraft dependencies are added to.
 *
 * @see MinecraftSetup
 */
public enum MinecraftConfiguration {
    COMMON("minecraftCommon"),
    CLIENT_ONLY("minecraftClientOnly");

    private final String compile;
    private final String runtime;

    MinecraftConfiguration(String name) {
        this.compile = name + "Compile";
        this.runtime = name + "Runtime";
    }

    public String getCompileConfigurationName() {
        return compile;
    }

    public String getRuntimeConfigurationName() {
        return runtime;
    }
}
