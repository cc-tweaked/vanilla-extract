package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.api.MappingsConfiguration;
import cc.tweaked.vanillaextract.api.VanillaMinecraftExtension;
import cc.tweaked.vanillaextract.configurations.MinecraftConfiguration;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.mappings.MojangMappings;
import cc.tweaked.vanillaextract.core.mappings.ParchmentMappings;
import cc.tweaked.vanillaextract.core.unpick.UnpickMetadata;
import cc.tweaked.vanillaextract.utils.Dependencies;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

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

        // Now add a dependency on the constants jar. This includes some classes defining additional constants. We
        // need on the classpath for Unpick to work, so let's add them in.
        // This is a bit awkward, as we can't guarantee the jar is present, so need to return a list of 0-1 items.
        var constantsDependency = getUnpickMappings().<List<Dependency>>flatMap(unpick -> {
            UnpickMetadata metadata;
            try {
                metadata = UnpickMetadata.fromJar(unpick.getAsFile().toPath());
            } catch (IOException e) {
                throw new GradleException("Cannot read unpick metadata", e);
            }

            return switch (metadata) {
                case UnpickMetadata.V1 v1 -> Dependencies.withClassifier(dependency, "constants").map(List::of);
                case UnpickMetadata.V2 v2 -> project.getProviders().provider(
                    () -> v2.constants() == null ? List.of() : List.of(project.getDependencies().create(v2.constants()))
                );
            };
        });
        project.getConfigurations().getByName(MinecraftConfiguration.COMMON.getCompileConfigurationName()).getDependencies().addAllLater(constantsDependency);
    }
}
