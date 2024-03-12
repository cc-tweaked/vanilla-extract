package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.core.mappings.ParchmentMappings;
import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MojangUrls;
import cc.tweaked.vanillaextract.decompile.DecompileService;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ArtifactRepositoryContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.MavenRepositoryContentDescriptor;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.build.event.BuildEventsListenerRegistry;

import javax.inject.Inject;
import java.nio.file.Path;

public abstract class CommonPlugin implements Plugin<Project> {
    @Inject
    protected abstract BuildEventsListenerRegistry getEventsListenerRegistry();

    protected Provider<GlobalMinecraftProvider> setup(Project project) {
        // We need all the core Java extensions and configurations, so we depend on the java-library plugin.
        project.getPlugins().apply(JavaLibraryPlugin.class);

        var gradle = project.getGradle();
        var globalGradleCache = gradle.getGradleUserHomeDir().toPath().resolve("caches");
        var localCache = project.getRootDir().toPath().resolve(".gradle").resolve("caches").resolve("VanillaExtract");

        // Create our service and register it as an event listener to keep it alive.
        var service = project.getGradle().getSharedServices().registerIfAbsent(GlobalMinecraftProvider.NAME, GlobalMinecraftProvider.class, options -> {
            var parameters = options.getParameters();
            parameters.getGlobalGradleCache().set(globalGradleCache.toFile());
            parameters.getLocalCache().set(localCache.toFile());
            parameters.getIsOffline().set(gradle.getStartParameter().isOffline());
            parameters.getRefresh().set(gradle.getStartParameter().isRefreshDependencies());
        });
        getEventsListenerRegistry().onTaskCompletion(service);

        // Set up our decompiler service. This just acts as a rate limiter.
        project.getGradle().getSharedServices().registerIfAbsent(DecompileService.NAME, DecompileService.class, options -> {
            options.getMaxParallelUsages().set(1);
        });

        // Declare our Maven repositories.
        declareRepositories(project.getRepositories(), localCache);

        return service;
    }

    private void declareRepositories(RepositoryHandler repositoryHandler, Path localCache) {
        var mojangMaven = repositoryHandler.maven(repo -> {
            repo.setName("Mojang");
            repo.setUrl(MojangUrls.LIBRARIES);
            repo.mavenContent(MavenRepositoryContentDescriptor::releasesOnly);
            repo.metadataSources(sources -> {
                sources.mavenPom();
                sources.artifact();
                sources.ignoreGradleMetadataRedirection();
            });
        });

        // Re-insert the Mojang maven as the first entry, to ensure it has the highest precedence.
        if (repositoryHandler.size() > 1) {
            repositoryHandler.remove(mojangMaven);
            repositoryHandler.add(0, mojangMaven);
        }

        // Mojang's maven doesn't publish the "parent" artifacts that we require when resolving modules,  so make sure
        // we have Maven Central available too.
        if (repositoryHandler.findByName(ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
            repositoryHandler.mavenCentral();
        }

        repositoryHandler.exclusiveContent(exclusive -> {
            exclusive.forRepositories(repositoryHandler.maven(repo -> {
                repo.setName("VanillaExtract Project Cache");
                repo.setUrl(localCache.resolve(GlobalMinecraftProvider.MAVEN_DIRECTORY));
            }));
            exclusive.filter(content -> {
                content.includeModule(TransformedMinecraftProvider.GROUP, TransformedMinecraftProvider.COMMON_MODULE);
                content.includeModule(TransformedMinecraftProvider.GROUP, TransformedMinecraftProvider.CLIENT_ONLY_MODULE);
            });
        });

        // Add Fabric and Parchment repos for convenience

        repositoryHandler.exclusiveContent(exclusive -> {
            exclusive.forRepositories(repositoryHandler.maven(repo -> {
                repo.setName("Fabric");
                repo.setUrl("https://maven.fabricmc.net");
            }));
            exclusive.filter(content -> {
                content.includeGroup("net.fabricmc");
                content.includeGroup("net.fabricmc.unpick");
            });
        });

        repositoryHandler.exclusiveContent(exclusive -> {
            exclusive.forRepositories(repositoryHandler.maven(repo -> {
                repo.setName("Parchment");
                repo.setUrl("https://maven.parchmentmc.org");
            }));
            exclusive.filter(content -> content.includeGroup(ParchmentMappings.GROUP));
        });
    }
}
