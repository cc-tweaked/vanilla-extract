package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.api.VanillaMinecraftExtension;
import cc.tweaked.vanillaextract.configurations.MinecraftConfiguration;
import cc.tweaked.vanillaextract.configurations.MinecraftSetup;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import cc.tweaked.vanillaextract.decompile.Decompile;
import cc.tweaked.vanillaextract.utils.Providers;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileSystemLocation;

import java.util.List;
import java.util.Set;

public abstract class VanillaPlugin extends CommonPlugin {
    @Override
    public void apply(Project project) {
        var service = setup(project);

        // Register our extension, which declares the user-configurable properties for Minecraft version, mappings and
        // access wideners.
        var extension = (MinecraftExtensionImpl) project.getExtensions().create(VanillaMinecraftExtension.class, "minecraft", MinecraftExtensionImpl.class);

        /*
        Then use these properties to set up our Minecraft jars. There's lots of different ways other plugins go about
        this:
         - Prepare Minecraft inside project.afterEvaluate (so once build.gradle has been run, but still during the
           configuration phase). This is what loom does, and is definitely simplest (and thus probably the best)
           option. However, it means we need to do this every build, even if we're not running tasks for that project!

        - VanillaGradle does some clever things with dependency resolution/variant selection hooks (documented on the
          wiki at https://github.com/SpongePowered/VanillaGradle/wiki/Implementation-Details#gradle-hooks). This is
          actually really cool and flexible, but gets into areas of Gradle I'm unfamiliar with, so avoiding for now.

        - ForgeGradle and NeoGradle scare me.

        The approach we go for is slightly different. We zip together our Minecraft service, and the user-configured
        version, mappings and access wideners, and then map over the resulting provider to transform the jars giving us
        a Provider<TransformedJars>.

        We can then use this property to derive our dependencies then add them to the configuration with addProvider.
        */
        var minecraft = Providers.cacheViaProperty(project.getObjects(), TransformedMinecraftProvider.TransformedJars.class, Providers.zip(
            service, extension.getVersion(), extension.getMappings(), extension.getAccessWideners().getElements(),
            VanillaPlugin::configureMinecraft
        ));

        // Set up the Minecraft configurations, and add our generated jars to their appropriate config.
        var setup = new MinecraftSetup(project);
        setup.setup();
        setup.addDependency(MinecraftConfiguration.COMMON, minecraft.map(x -> x.common().release().coordinate()), true, true);
        setup.addDependency(MinecraftConfiguration.CLIENT_ONLY, minecraft.map(x -> x.clientOnly().release().coordinate()), true, true);

        // Set up the decompile task, with our two jars.
        Decompile.setup(project, extension, List.of(
            new Decompile.Target(MinecraftConfiguration.COMMON, minecraft.map(x -> x.common().path().toFile())),
            new Decompile.Target(MinecraftConfiguration.CLIENT_ONLY, minecraft.map(x -> x.clientOnly().path().toFile()))
        ));
    }

    private static TransformedMinecraftProvider.TransformedJars configureMinecraft(GlobalMinecraftProvider service, String version, MappingProvider mappings, Set<FileSystemLocation> accessWideners) {
        try {
            return service.provide(version, mappings, accessWideners.stream().map(x -> x.getAsFile().toPath()).toList(), false).jars();
        } catch (Exception e) {
            throw new GradleException("Failed to setup Minecraft jars", e);
        }
    }
}
