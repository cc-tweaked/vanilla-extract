package cc.tweaked.vanillaextract.decompile;

import cc.tweaked.vanillaextract.MinecraftExtensionImpl;
import cc.tweaked.vanillaextract.configurations.MinecraftConfiguration;
import cc.tweaked.vanillaextract.core.unpick.UnpickMetadata;
import cc.tweaked.vanillaextract.utils.Dependencies;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Configuration for our {@linkplain DecompileTask decompile task}.
 */
public final class Decompile {
    private static final String VINEFLOWER_CONFIGURATION_NAME = "vineflower";
    private static final String VINEFLOWER_VERSION = "1.11.0";
    private static final String ASM_VERSION = "9.7.1";

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

        // The unpick mappings contain a config file describing which version of Unpick we need. We lazily extract that
        // from the jar and map it to a Gradle dependency.
        var config = Dependencies.createDetachedConfiguration(project.getConfigurations(), "Unpick CLI tool", extension.getUnpickMappings().map(x -> {
            UnpickMetadata metadata;
            try {
                metadata = UnpickMetadata.fromJar(x.getAsFile().toPath());
            } catch (IOException e) {
                throw new GradleException("Cannot read unpick metadata", e);
            }
            return project.getDependencies().create(metadata.getCliTool().toDependencyString());
        }));

        // Ensure we're using the latest version of ASM.
        for (var asmModule : List.of("asm", "asm-tree", "asm-commons", "asm-util")) {
            config.getDependencies().add(project.getDependencies().create(Map.of(
                "group", "org.ow2.asm",
                "name", asmModule,
                "version", ASM_VERSION
            )));
        }

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

            task.getUnpickClasspath().setFrom(config);
            task.getUnpickClasspath().disallowChanges();

            task.getDecompilerClasspath().setFrom(decompiler);
            task.getDecompilerClasspath().disallowChanges();

            for (var jar : jars) {
                var file = jar.file();
                var classpath = task.getProject().getConfigurations().getByName(jar.configuration().getCompileConfigurationName());

                task.addTarget(t -> {
                    t.getInputFile().fileProvider(file);
                    t.getClasspath().from(classpath.minus(task.getProject().getLayout().files(file)));
                });
            }
            task.getTargets().disallowChanges();
        });
    }

    /**
     * A target jar that we should decompile.
     *
     * @param configuration The Minecraft configuration this jar exists in.
     * @param file          The file to decompile.
     * @see DecompileTask.Target
     */
    public record Target(MinecraftConfiguration configuration, Provider<File> file) {
    }
}
