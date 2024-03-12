package cc.tweaked.vanillaextract.configurations;

import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.attributes.java.TargetJvmEnvironment;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.jvm.tasks.Jar;

import java.io.File;

import static cc.tweaked.vanillaextract.GradleUtils.lazyNamed;

/**
 * Sets up the project for working with Minecraft. This includes:
 *
 * <ul>
 *     <li>Adding a new client source set {@link #createClientSources()}.</li>
 *     <li>
 *         Creating the configurations needed for the Minecraft jars, adding them to the classpath for main/client/test.
 *     </li>
 *     <li>Creating outgoing configurations for multi-project workspaces ({@link #setupOutgoingConfigurations()}</li>
 * </ul>
 * <p>
 * We also provide several convenience functions which may be used for custom build logic:
 * <ul>
 *     <li>{@link #addMinecraftDependency(SourceSet, MinecraftJar)}: adds the Minecraft jar to other source sets.</li>
 *     <li>
 *         {@link #extendClasspath(SourceSet, SourceSet)}: Perform some magic tricks to inherit dependencies between
 *         source sets.
 *     </li>
 * </ul>
 */
public class MinecraftSetup {
    public static final String CLIENT_SOURCE_SET_NAME = "client";

    private final Project project;
    private final ObjectFactory objects;
    private final ConfigurationContainer configurations;
    private final SourceSetContainer sourceSets;

    public MinecraftSetup(Project project) {
        this.project = project;
        this.objects = project.getObjects();
        this.configurations = project.getConfigurations();
        this.sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    }

    public void setup(Provider<TransformedMinecraftProvider.TransformedJars> jars) {
        createClientSources();
        setupOutgoingConfigurations();

        // Create a new runtime and compile-time configuration for the common/server and client jars.
        for (var config : MinecraftJar.values()) {
            configurations.create(config.getCompileConfigurationName(), c -> {
                c.setVisible(false);
                c.setCanBeResolved(true); // Bit nasty, but needed for decompilation.
                c.setCanBeConsumed(false);
            });

            configurations.create(config.getRuntimeConfigurationName(), c -> {
                c.setVisible(false);
                c.setCanBeResolved(false);
                c.setCanBeConsumed(false);
            });

            // And add our Minecraft jar as a dependency for these configurations.
            var dependency = jars.map(x -> config.getJar(x).release().coordinate());
            project.getDependencies().addProvider(config.getCompileConfigurationName(), dependency);
            project.getDependencies().addProvider(config.getRuntimeConfigurationName(), dependency);
        }

        // TODO: It would be nice to handle Minecraft's native dependencies in a dynamic way using Gradle's variants.
        //  There's an example in the docs on how to do this:
        //  (https://docs.gradle.org/current/userguide/component_metadata_rules.html#adding_variants_for_native_jars),
        //  which we might be able to expand.

        // Then add these dependencies to the main, client and test source set.
        addMinecraftDependency(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME), MinecraftJar.COMMON);
        addMinecraftDependency(sourceSets.getByName(CLIENT_SOURCE_SET_NAME), MinecraftJar.CLIENT_ONLY);
        addMinecraftDependency(sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME), MinecraftJar.CLIENT_ONLY);
    }

    /**
     * Creates a new {@linkplain #CLIENT_SOURCE_SET_NAME "client"} source set.
     */
    private void createClientSources() {
        var main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        var test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

        // First, create a new client source set for the client classes.
        var client = sourceSets.create(CLIENT_SOURCE_SET_NAME);

        // Source sets don't come with an "api" configuration by default. We add a "clientApi" to be consistent with
        // the common source set.
        var clientApi = configurations.create(client.getApiConfigurationName(), c -> {
            c.setVisible(false);
            c.setCanBeConsumed(false);
            c.setCanBeResolved(false);
        });
        configurations.named(client.getImplementationConfigurationName(), c -> c.extendsFrom(clientApi));

        // The client classes should depend on the common classes.
        project.getDependencies().add(
            client.getImplementationConfigurationName(),
            Capabilities.commonClasses((ModuleDependency) project.getDependencies().create(project))
        );
        // Additionally, make client "inherit" main's dependencies, so dependencies declared for the common classes are
        // also available for the client ones.
        extendClasspath(client, main);

        // Do the same for tests, so client and main sources (and dependencies) are available to the test classes.
        project.getDependencies().add(
            test.getImplementationConfigurationName(),
            Capabilities.clientClasses((ModuleDependency) project.getDependencies().create(project))
        );
        extendClasspath(test, client);

        // We create an empty jar for the client source set, which we then add as an artifact in our outgoing variants
        // (setupOutgoingConfigurations). This doesn't change any runtime behaviour (it doesn't add new classes), but
        // means IntelliJ IDEA is able to map Gradle dependencies on clientClasses back to a dependency on the original
        // sources.
        project.getTasks().register(client.getJarTaskName(), Jar.class, t -> {
            t.setDescription("An empty jar standing in for the client classes.");
            t.setGroup(BasePlugin.BUILD_GROUP);
            t.getArchiveClassifier().set("client");
        });

        // Configure all the "main" artifact tasks (jar, javadoc, sources jar) to also include the client classes.
        project.getTasks().named(main.getJarTaskName(), Jar.class, t -> t.from(client.getOutput()));
        project.getTasks().named(main.getJavadocTaskName(), Javadoc.class, t -> {
            t.source(client.getAllJava());
            t.setClasspath(main.getCompileClasspath().plus(main.getOutput()).plus(client.getCompileClasspath()).plus(client.getOutput()));
        });
        lazyNamed(project.getTasks(), main.getSourcesJarTaskName(), Jar.class, t -> t.from(client.getAllSource()));
    }

    /**
     * "Extend" a source set's classpath, adding all dependencies of {@code parent} to {@code toExtend}.
     * <p>
     * By default, a source set's compile classpath inherits from "implementation" and "compileOnly", and the runtime
     * classpath from "implementation" and "compileOnly" (see {@link org.gradle.api.plugins.JavaBasePlugin},
     * specifically {@code defineConfigurationsForSourceSet}, for where these are set up).
     * <p>
     * We mirror that logic, but instead change {@code toExtend}'s compile classpath to also inherit {@code parent}'s
     * "implementation" and "compileOnly" (and similarly for the runtime classpath).
     * <p>
     * This is inspired by {@link org.gradle.api.plugins.jvm.internal.DefaultJvmFeature}'s {@code doExtendProductionCode}.
     * However, the caveats there (missing dependencies in the published metadata) are less relevant here as we only
     * publish a single jar.
     *
     * @param toExtend The source set to extend.
     * @param parent   The "parent" source set, whose dependencies should be inherited.
     */
    public void extendClasspath(SourceSet toExtend, SourceSet parent) {
        configurations.getByName(toExtend.getCompileClasspathConfigurationName()).extendsFrom(
            configurations.getByName(parent.getImplementationConfigurationName()),
            configurations.getByName(parent.getCompileOnlyConfigurationName())
        ).shouldResolveConsistentlyWith(configurations.getByName(parent.getCompileClasspathConfigurationName()));
        configurations.getByName(toExtend.getRuntimeClasspathConfigurationName()).extendsFrom(
            configurations.getByName(parent.getImplementationConfigurationName()),
            configurations.getByName(parent.getRuntimeOnlyConfigurationName())
        ).shouldResolveConsistentlyWith(configurations.getByName(parent.getRuntimeClasspathConfigurationName()));
    }

    /**
     * Set up our outgoing configurations, allowing other projects to depend on the common or client-only classes in a
     * more convenient manner.
     * <p>
     * This effectively exposes two variants to other Gradle subprojects, a bit like publishing separate {@code -client}
     * and {@code -commonOnly} jars. The {@link Capabilities#commonClasses(ModuleDependency)} and
     * {@link Capabilities#clientClasses(ModuleDependency)} functions then allow you to select one of these variants.
     */
    public void setupOutgoingConfigurations() {
        // TODO: It would be nice if we didn't need to do it like this, and instead automatically selected the main/client
        //  version depending on the project. This might be possible to do with custom attributes
        //  (https://docs.gradle.org/current/userguide/variant_attributes.html#sec:declaring_attributes), but not
        //  something we need to do now.
        setupOutgoingConfigurations("common", sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME), "common code", Capabilities.COMMON);
        setupOutgoingConfigurations("client", sourceSets.getByName(CLIENT_SOURCE_SET_NAME), "client code", Capabilities.CLIENT);
    }

    /**
     * Set up an outgoing configuration for a source set.
     * <p>
     * See {@link org.gradle.api.plugins.jvm.internal.DefaultJvmFeature}'s {@code createApiElements} and
     * {@code createRuntimeElements} for where Gradle itself sets up these features - we largely mirror the setup here.
     */
    private void setupOutgoingConfigurations(
        String configurationName, SourceSet sourceSet, String description, String capabilityName
    ) {
        setupOutgoingConfiguration(
            configurationName + "ApiElements",
            sourceSet,
            capabilityName,
            objects.named(Usage.class, Usage.JAVA_API),
            c -> {
                c.setDescription("API elements for " + description);
                c.extendsFrom(configurations.getByName(sourceSet.getApiConfigurationName()));
            }
        );

        setupOutgoingConfiguration(
            configurationName + "RuntimeElements",
            sourceSet,
            capabilityName,
            objects.named(Usage.class, Usage.JAVA_RUNTIME),
            c -> {
                c.setDescription("Runtime elements for " + description);
                c.extendsFrom(
                    configurations.getByName(sourceSet.getImplementationConfigurationName()),
                    configurations.getByName(sourceSet.getRuntimeOnlyConfigurationName())
                );
            }
        );
    }

    /**
     * Set up an outgoing configuration for a source set.
     */
    private void setupOutgoingConfiguration(
        String configurationName, SourceSet sourceSet, String capabilityName, Usage usage,
        Action<? super Configuration> configure
    ) {
        configurations.register(configurationName, c -> {
            c.setVisible(false);
            c.setCanBeConsumed(true);
            c.setCanBeResolved(false);

            configure.execute(c);

            c.attributes(a -> {
                a.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
                a.attribute(Usage.USAGE_ATTRIBUTE, usage);
                a.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.EXTERNAL));
                a.attribute(TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE, objects.named(TargetJvmEnvironment.class, TargetJvmEnvironment.STANDARD_JVM));
                a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.JAR));
            });

            c.outgoing(o -> {
                // Mark this as an outgoing variant with the appropriate capability.
                o.capability(new ProjectCapability(project, capabilityName));

                // Expose the jar as an artifact.
                o.artifact(project.getTasks().named(sourceSet.getJarTaskName()));

                // Expose our classes as an artifact. This is important for the main/common source set - Gradle will
                // prefer to use the classes directly (rather than the jar) as it's more efficient, so this ensures that
                // other projects compile against *just* the common classes, rather than the jar (which contains the
                // client classes as well).
                o.getVariants().create("classes", v -> {
                    v.getAttributes().attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.class, LibraryElements.CLASSES));

                    for (File output : sourceSet.getOutput().getClassesDirs()) {
                        v.artifact(output, x -> x.builtBy(sourceSet.getOutput()));
                    }
                });
            });
        });
    }

    /**
     * Configure a source set to depend on Minecraft.
     * <p>
     * This adds Minecraft directly to the compile/runtime classpath. This means that Minecraft is still available at
     * runtime (unlike {@code compileOnly}) but the dependency is not propagated to other projects or published in the
     * resulting POM (unlike {@code implementation}/{@code api}).
     *
     * @param sourceSet     The source set to add Minecraft to.
     * @param configuration The Minecraft configuration to depend on.
     */
    public void addMinecraftDependency(SourceSet sourceSet, MinecraftJar configuration) {
        extendsFrom(sourceSet.getCompileClasspathConfigurationName(), configuration.getCompileConfigurationName());
        extendsFrom(sourceSet.getRuntimeClasspathConfigurationName(), configuration.getRuntimeConfigurationName());
    }

    private void extendsFrom(String name, String from) {
        configurations.getByName(name).extendsFrom(configurations.getByName(from));
    }
}
