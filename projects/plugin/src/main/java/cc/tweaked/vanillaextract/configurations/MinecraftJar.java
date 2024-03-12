package cc.tweaked.vanillaextract.configurations;

import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import org.gradle.api.artifacts.Configuration;

/**
 * A {@link Configuration} that Minecraft dependencies are added to.
 *
 * @see MinecraftSetup
 */
public enum MinecraftJar {
    COMMON("minecraftCommon"),
    CLIENT_ONLY("minecraftClientOnly");

    private final String compile;
    private final String runtime;

    MinecraftJar(String name) {
        this.compile = name + "Compile";
        this.runtime = name + "Runtime";
    }

    public TransformedMinecraftProvider.TransformedJar getJar(TransformedMinecraftProvider.TransformedJars jars) {
        return switch (this) {
            case COMMON -> jars.common();
            case CLIENT_ONLY -> jars.clientOnly();
        };
    }

    public String getCompileConfigurationName() {
        return compile;
    }

    public String getRuntimeConfigurationName() {
        return runtime;
    }
}
