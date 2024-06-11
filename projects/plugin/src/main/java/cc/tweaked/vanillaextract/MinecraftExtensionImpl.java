package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.api.MappingsConfiguration;
import cc.tweaked.vanillaextract.api.VanillaMinecraftExtension;
import cc.tweaked.vanillaextract.configurations.MinecraftJar;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.mappings.MojangMappings;
import cc.tweaked.vanillaextract.core.mappings.ParchmentMappings;
import cc.tweaked.vanillaextract.utils.Dependencies;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;

/**
 * The concrete implementation of {@link VanillaMinecraftExtension}.
 */
public abstract class MinecraftExtensionImpl implements VanillaMinecraftExtension {
    /**
     * The currently configured Minecraft version, set by {@link #version(String)}.
     */
    public abstract Property<String> getVersion();

    /**
     * The set of access wideners to use.
     *
     * @see #accessWideners(Object...)
     */
    public abstract ConfigurableFileCollection getAccessWideners();

    /**
     * The currently configured mappings, set by {@link #mappings(Action)}.
     */
    public abstract Property<MappingProvider> getMappings();

    /**
     * The currently configured unpick mappings, set by {@link #unpick(Object)}.
     */
    public abstract RegularFileProperty getUnpickMappings();

    @Inject
    protected abstract Project getProject();

    public MinecraftExtensionImpl() {
        getVersion().finalizeValueOnRead();
        getAccessWideners().finalizeValueOnRead();
        getMappings().convention(MojangMappings.get()).finalizeValueOnRead();
        getUnpickMappings().finalizeValueOnRead();
    }

    @Override
    public void version(String version) {
        getVersion().set(version);
        getVersion().disallowChanges();
    }

    @Override
    public void version(Provider<String> version) {
        getVersion().set(version);
        getVersion().disallowChanges();
    }

    @Override
    public void accessWideners(Object... file) {
        getAccessWideners().from(file);
    }

    @Override
    public void mappings(Action<? super MappingsConfiguration> configure) {
        configure.execute(new MappingsConfiguration() {
            @Override
            public void official() {
                getMappings().set(MojangMappings.get());
                getMappings().disallowChanges();
            }

            @Override
            public void parchment(String mcVersion, String version) {
                var dependency = ParchmentMappings.getArtifact(mcVersion, version).toDependencyString();

                var project = getProject();
                var configuration = Dependencies.createDetachedConfiguration(
                    project.getConfigurations(), "Parchment mappings", project.getDependencies().create(dependency)
                );
                // This prevents changing versions like 1.20.4-2024.+. We don't need this - we handle mappings changing
                // already - but it feels a sensible precaution.
                configuration.resolutionStrategy(ResolutionStrategy::failOnNonReproducibleResolution);

                getMappings().set(Dependencies.getSingleFile(configuration).map(x -> new ParchmentMappings(x.getAsFile().toPath())));
                getMappings().disallowChanges();
            }
        });
    }

    @Override
    public void unpick(Object dependencySpec) {
        var project = getProject();

        var baseDependency = project.getDependencies().create(dependencySpec);
        if (!(baseDependency instanceof ModuleDependency module)) {
            throw new GradleException("Unpick dependency must be a module.");
        }

        unpickImpl(getProject().getProviders().provider(() -> module));
    }

    @Override
    public void unpick(Provider<? extends ModuleDependency> dependency) {
        unpickImpl(dependency);
    }

    public void unpickImpl(Provider<? extends ModuleDependency> dependency) {
        var project = getProject();

        // Create a dependency on the :mergedv2 jar, put it in its own detached configuration, and then bind
        // getUnpickMappings() to the result. This is then consumed in the decompile task.
        // Arguably we should be using DependencyHandler.variantOf here, but that only works with external modules.
        var mergedDependency = Dependencies.withClassifier(dependency, "mergedv2");

        var configuration = Dependencies.createDetachedConfiguration(project.getConfigurations(), "Unpick mappings (merged)", mergedDependency);
        // We don't strictly-speaking need failOnNonReproducibleResolution - unpick is only used for decompilation, so
        // doesn't need to be reproducible, but nice to have.
        configuration.resolutionStrategy(ResolutionStrategy::failOnNonReproducibleResolution);

        getUnpickMappings().fileProvider(Dependencies.getSingleFile(configuration).map(FileSystemLocation::getAsFile));
        getUnpickMappings().disallowChanges();

        // Now add a dependency on the :constants jar. This includes some classes defining additional constants. We
        // need on the classpath for Unpick to work, so let's add them in.
        var constantsDependency = Dependencies.withClassifier(dependency, "constants");
        project.getDependencies().addProvider(MinecraftJar.COMMON.getCompileConfigurationName(), constantsDependency);
        project.getDependencies().addProvider(MinecraftJar.CLIENT_ONLY.getCompileConfigurationName(), constantsDependency);
    }
}
