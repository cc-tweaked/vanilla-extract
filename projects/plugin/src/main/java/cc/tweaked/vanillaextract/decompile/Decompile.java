package cc.tweaked.vanillaextract.decompile;

import cc.tweaked.vanillaextract.MinecraftExtensionImpl;
import cc.tweaked.vanillaextract.configurations.MinecraftConfiguration;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.util.List;

/**
 * Configuration for our {@linkplain DecompileTask decompile task}.
 */
public final class Decompile {
    private static final String VINEFLOWER_CONFIGURATION_NAME = "vineflower";
    private static final String VINEFLOWER_VERSION = "1.11.1";

    /**
     * @param project   The current project.
     * @param extension The Minecraft extension, used to (re-)provision Minecraft and get Loom version.
     * @param jars      The classes to decompile.
     */
    public static void setup(Project project, MinecraftExtensionImpl extension, List<Target> jars) {
        // Create a new configuration and add Vineflower to it. This allows users to override Vineflower if needed.
        var decompiler = project.getConfigurations().create(VINEFLOWER_CONFIGURATION_NAME, c -> {
            c.setDescription("The Vineflower decompiler");
            c.setCanBeResolved(true);
            c.setCanBeConsumed(false);
        });
        decompiler.defaultDependencies(deps -> deps.add(project.getDependencies().create("org.vineflower:vineflower:" + VINEFLOWER_VERSION)));

        // Finally create our decompile task. This largely involves binding all our options from the extension to the
        // task.
        project.getTasks().register("decompile", DecompileTask.class, task -> {
            task.getVersion().set(extension.getVersion());
            task.getVersion().disallowChanges();

            task.getAccessWideners().setFrom(extension.getAccessWideners());
            task.getAccessWideners().disallowChanges();

            task.getMappings().set(extension.getMappings());
            task.getMappings().disallowChanges();

            task.getUnpickMappings().set(extension.getUnpickMappings());
            task.getUnpickMappings().disallowChanges();

            task.getDecompilerClasspath().setFrom(decompiler);
            task.getDecompilerClasspath().disallowChanges();

            for (var jar : jars) {
                task.getTargets().add(project.getLayout().file(jar.file()));
                task.getClasspath().from(task.getProject().getConfigurations().getByName(jar.configuration().getCompileConfigurationName()));
            }
            task.getTargets().disallowChanges();
        });
    }

    /**
     * A target jar that we should decompile.
     *
     * @param configuration The Minecraft configuration this jar exists in.
     * @param file          The file to decompile.
     */
    public record Target(MinecraftConfiguration configuration, Provider<File> file) {
    }
}
