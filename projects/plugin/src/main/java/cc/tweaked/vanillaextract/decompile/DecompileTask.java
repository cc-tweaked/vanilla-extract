package cc.tweaked.vanillaextract.decompile;

import cc.tweaked.vanillaextract.GlobalMinecraftProvider;
import cc.tweaked.vanillaextract.api.VanillaMinecraftExtension;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.unpick.UnpickProvider;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import cc.tweaked.vanillaextract.core.util.Timing;
import com.sun.management.OperatingSystemMXBean;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.services.ServiceReference;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;
import org.gradle.work.DisableCachingByDefault;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Decompile Minecraft using Vineflower.
 * <p>
 * This is quite a messy class, as it accepts inputs from several places, including:
 * <ul>
 *     <li>The Minecraft jars, and their classpaths.</li>
 *     <li>The configuration in {@link VanillaMinecraftExtension}.</li>
 *     <li>Unpick mappings (also configured in {@link VanillaMinecraftExtension}) and its classpath.</li>
 *     <li>The decompiler classpath (and several other options).</li>
 * </ul>
 *
 * @see Decompile Setting up and configuring this task
 */
@DisableCachingByDefault(because = "Useful to run multiple times")
public abstract class DecompileTask extends DefaultTask {
    // region Input files

    /**
     * A target jar to decompile.
     */
    public interface Target {
        /**
         * The input jar to decompile.
         */
        @InputFile
        RegularFileProperty getInputFile();

        /**
         * The classpath for this jar.
         */
        @Classpath
        ConfigurableFileCollection getClasspath();
    }

    /**
     * The list of targets to decompile.
     */
    @Input
    public abstract ListProperty<Target> getTargets();

    /**
     * Add a target to the decompile task.
     *
     * @param configure The function to configure the target.
     */
    public void addTarget(Action<? super Target> configure) {
        var instance = getObjectFactory().newInstance(Target.class);
        configure.execute(instance);
        instance.getInputFile().disallowChanges();
        instance.getClasspath().disallowChanges();
        getTargets().add(instance);
    }
    // endregion

    // region Minecraft

    /**
     * The targeted Minecraft version. This is bound to {@linkplain VanillaMinecraftExtension#version(String) the
     * extension-configured version}.
     */
    @Input
    public abstract Property<String> getVersion();

    /**
     * The current mappings. This is bound to {@linkplain VanillaMinecraftExtension#mappings(Action) the
     * extension-configured mappings}.
     */
    @Input
    public abstract Property<MappingProvider> getMappings();

    /**
     * The current access wideners. This is bound to {@linkplain VanillaMinecraftExtension#accessWideners(Object...)} the
     * extension-configured mappings}.
     */
    @InputFiles
    public abstract ConfigurableFileCollection getAccessWideners();

    // endregion

    // region Unpick configuration

    @InputFile
    @Optional
    public abstract RegularFileProperty getUnpickMappings();

    @Classpath
    @Optional
    public abstract ConfigurableFileCollection getUnpickClasspath();

    // endregion

    // region Decompiler

    /**
     * The number of threads the decompiler will use.
     */
    @Internal
    public abstract Property<Integer> getThreadCount();

    /**
     * The maximum memory the decompiler will use.
     */
    @Internal
    public abstract Property<String> getHeapLimit();

    /**
     * The classpath containing the decompiler jar, used to spawn a new worker.
     *
     * @see Decompile#setup Setting up the configuration.
     */
    @Classpath
    public abstract ConfigurableFileCollection getDecompilerClasspath();

    /**
     * Whether to log decompiler messages.
     */
    @Internal
    public abstract Property<Boolean> getLogEnabled();

    // endregion

    // region Services

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    @ServiceReference(DecompileService.NAME)
    protected abstract Property<DecompileService> getDecompileService();

    @ServiceReference(GlobalMinecraftProvider.NAME)
    protected abstract Property<GlobalMinecraftProvider> getMinecraftService();

    // endregion

    public DecompileTask() {
        getTargets().finalizeValueOnRead();
        getDecompilerClasspath().finalizeValueOnRead();

        getThreadCount().convention(getProviderFactory().provider(() -> ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()));
        getHeapLimit().convention(getProviderFactory().provider(() -> {
            long systemMemory = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalMemorySize();
            return Math.max(systemMemory / (1024L * 1024L) / 4, 4096) + "M";
        }));
        getLogEnabled().convention(false);
    }

    @TaskAction
    public void run() throws IOException {
        var minecraft = getMinecraftService().get();
        var inputJars = getTargets().get();

        // This is a bit of a hack, but we regenerate our transformed jars, to ensure we apply line maps to the
        // "original" jar, rather than an already line-mapped jar.
        var everything = minecraft.provide(
            getVersion().get(),
            getMappings().get(),
            getAccessWideners().getFiles().stream().map(File::toPath).toList(),
            true
        );

        // We do some quick sanity checks to make sure the transformed jars are the same as the ones that have been
        // configured.
        var minecraftJars = everything.jars();
        var expectedJars = Set.of(
            minecraftJars.common().path(),
            minecraftJars.clientOnly().path()
        );
        if (inputJars.size() != expectedJars.size() || inputJars.stream().anyMatch(x -> !expectedJars.contains(x.getInputFile().get().getAsFile().toPath()))) {
            getLogger().warn("Expected to be transforming {}, but actually transforming {}.", expectedJars, inputJars);
        }

        List<Path> toDelete = new ArrayList<>();
        try {
            // If we have unpick definitions available, remap them and save them to a temporary file.
            Path unpickDefinitions, unpickLogConfig;
            var unpick = getUnpickMappings().getOrNull();
            if (unpick != null) {
                unpickDefinitions = getUnpickDefinitions(unpick.getAsFile().toPath(), everything.mappings());
                toDelete.add(unpickDefinitions);

                unpickLogConfig = Files.createTempFile("logging", "properties");
                toDelete.add(unpickLogConfig);

                try (var is = getClass().getClassLoader().getResourceAsStream("unpick-logging.properties");
                     var os = Files.newOutputStream(unpickLogConfig, StandardOpenOption.WRITE)) {
                    Objects.requireNonNull(is, "Cannot find unpick logging config").transferTo(os);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to copy unpick logging config", e);
                }
            } else {
                unpickDefinitions = null;
                unpickLogConfig = null;
            }

            // We then spin up a new process for running our decompiler.
            var queue = getWorkerExecutor().processIsolation(p -> {
                p.forkOptions(f -> f.setMaxHeapSize(getHeapLimit().get()));
                p.getClasspath().from(getDecompilerClasspath());
            });

            // And start processing our inputs.
            for (var jar : inputJars) {
                var inputJar = jar.getInputFile().get().getAsFile().toPath();

                // If we have unpick definitions available, apply them to our current jar, and use that as an input to
                // the decompiler instead.
                Path unpickJar;
                if (unpickDefinitions != null) {
                    unpickJar = getUnpickJar(inputJar, jar.getClasspath(), unpickDefinitions, unpickLogConfig);
                    toDelete.add(unpickJar);
                } else {
                    unpickJar = inputJar;
                }

                var outputSources = MoreFiles.addSuffix(inputJar, "-sources");
                try (var outputClasses = MoreFiles.scratch(inputJar)) {
                    long start = System.nanoTime();
                    getLogger().info("Decompiling {}.", inputJar);

                    // Finally, run the decompiler on our external executor.
                    queue.submit(DecompileAction.class, p -> {
                        p.getOriginalInput().set(inputJar.toFile());
                        p.getUnpickedInput().set(unpickJar.toFile());
                        p.getOutputClasses().set(outputClasses.path().toFile());
                        p.getOutputSources().set(outputSources.toFile());
                        p.getClasspath().from(jar.getClasspath());
                        p.getThreadCount().set(getThreadCount());
                        p.getMappings().set(everything.mappings().toFile());
                        p.getLog().set(getLogEnabled());
                    });

                    queue.await();

                    getLogger().info("Decompiling took {}.", Timing.formatSince(start));

                    outputClasses.commit();
                }
            }
        } finally {
            for (var file : toDelete) MoreFiles.tryDelete(file);

            // TODO: Do we want to terminate the external worker, like Fabric does?
        }
    }

    /**
     * Generate our unpick definitions.
     *
     * @param unpick   The jar containing unpick definitions and their mappings.
     * @param mappings The workspace mappings.
     * @return The path the remapped unpick definitions were written to.
     * @throws IOException If we failed to generate the definitions.
     */
    private Path getUnpickDefinitions(Path unpick, Path mappings) throws IOException {
        var output = Files.createTempFile("definitions", ".unpick");

        long start = System.nanoTime();
        getLogger().info("Remapping unpick definitions from {}.", unpick);

        UnpickProvider.provideUnpick(mappings, unpick, output);

        getLogger().info("Remapping unpick definitions took {}.", Timing.formatSince(start));

        return output;
    }

    /**
     * Generate an unpicked jar.
     *
     * @param input       The input jar.
     * @param classpath   The classpath of the input jar.
     * @param definitions The unpick definitions.
     * @return The jar the unpicked classes were written to.
     */
    private Path getUnpickJar(Path input, FileCollection classpath, Path definitions, Path logConfig) {
        var output = MoreFiles.addSuffix(input, "-unpick");

        long start = System.nanoTime();
        getLogger().info("Unpicking {} using {}.", input, definitions);

        getExecOperations().javaexec(x -> {
            x.getMainClass().set("daomephsta.unpick.cli.Main");
            x.setClasspath(getUnpickClasspath());

            x.systemProperty("java.util.logging.config.file", logConfig.toFile().getAbsolutePath());

            List<File> args = new ArrayList<>();
            args.add(input.toFile());
            args.add(output.toFile());
            args.add(definitions.toFile());
            args.addAll(classpath.getFiles());
            x.setArgs(args.stream().map(File::getAbsolutePath).toList());
        });

        getLogger().info("Unpicking took {}.", Timing.formatSince(start));

        return output;
    }
}
