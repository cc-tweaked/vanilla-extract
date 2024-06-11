package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.core.download.BasicFileDownloader;
import cc.tweaked.vanillaextract.core.inputs.FileFingerprint;
import cc.tweaked.vanillaextract.core.mappings.MappingProvider;
import cc.tweaked.vanillaextract.core.mappings.MappingsFileProvider;
import cc.tweaked.vanillaextract.core.minecraft.MinecraftProvider;
import cc.tweaked.vanillaextract.core.minecraft.MinecraftVersionProvider;
import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion;
import cc.tweaked.vanillaextract.core.util.Timing;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationCompletionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.GuardedBy;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * A global build service for downloading and providing Minecraft jars.
 * <p>
 * We construct this as a {@link BuildService} at initialisation time, allowing us to share it across projects. This
 * isn't strictly speaking needed (our tasks should support multiple instances running at once), but helps avoid doing
 * duplicate work.
 */
public abstract class GlobalMinecraftProvider implements BuildService<GlobalMinecraftProvider.Parameters>, OperationCompletionListener {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalMinecraftProvider.class);

    public static final String NAME = "VanillaExtract:MinecraftProvider";

    public static final String MAVEN_DIRECTORY = "maven";

    public interface Parameters extends BuildServiceParameters {
        DirectoryProperty getGlobalGradleCache();

        DirectoryProperty getLocalCache();

        Property<Boolean> getIsOffline();

        Property<Boolean> getRefresh();
    }

    private final Path globalGradleCache;
    private final Path globalPluginCache;
    private final Path localMavenPath;
    private final boolean refresh;

    private final @GuardedBy("minecraftVersions") Map<String, MinecraftVersion> minecraftVersions = new HashMap<>();
    private final @GuardedBy("minecraftVersions") MinecraftVersionProvider minecraftVersionProvider;

    private final MinecraftProvider minecraftProvider;
    private final MappingsFileProvider mappingsFileProvider;
    private final TransformedMinecraftProvider transformedMinecraftProvider;

    public GlobalMinecraftProvider() {
        var params = getParameters();

        globalGradleCache = params.getGlobalGradleCache().get().getAsFile().toPath();
        globalPluginCache = globalGradleCache.resolve("VanillaExtract");
        var localCache = params.getLocalCache().get().getAsFile().toPath();
        localMavenPath = localCache.resolve(MAVEN_DIRECTORY);
        refresh = params.getRefresh().get();

        var downloader = new BasicFileDownloader();
        minecraftVersionProvider = new MinecraftVersionProvider(globalPluginCache, downloader);
        mappingsFileProvider = new MappingsFileProvider(globalPluginCache);
        minecraftProvider = new MinecraftProvider(downloader);
        transformedMinecraftProvider = new TransformedMinecraftProvider(localMavenPath);
    }

    public MinecraftVersion getVersion(String version) throws IOException {
        synchronized (minecraftVersions) {
            var versionInfo = minecraftVersions.get(version);
            if (versionInfo != null) return versionInfo;

            minecraftVersions.put(version, versionInfo = minecraftVersionProvider.getVersion(version, refresh));
            return versionInfo;
        }
    }

    public Everything provide(String version, MappingProvider mappings, List<Path> accessWideners, boolean refresh) throws IOException {
        long start = System.nanoTime();
        Everything result = provideVanilla(version, mappings, accessWideners, refresh);
        LOG.info("Set up Minecraft {} in {}.", version, Timing.formatSince(start));
        return result;
    }

    private Everything provideVanilla(String version, MappingProvider mappings, List<Path> accessWideners, boolean refresh) throws IOException {
        MinecraftProvider.SplitArtifacts minecraft;
        synchronized (minecraftProvider) {
            var folder = globalPluginCache.resolve(version);
            var versionInfo = getVersion(version);
            var raw = minecraftProvider.provideRaw(folder, versionInfo.downloads(), versionInfo.libraries());
            minecraft = minecraftProvider.provideSplit(folder, raw, refresh);
        }

        var resolvedMappings = mappings.resolve(new MappingProvider.Context(minecraft.mappings(), this::fingerprint));
        var mappingPath = mappingsFileProvider.saveMappings(version, resolvedMappings);

        List<FileFingerprint> fingerprintedAccessWideners = new ArrayList<>(accessWideners.size());
        for (var accessWidener : accessWideners) fingerprintedAccessWideners.add(fingerprint(accessWidener));
        fingerprintedAccessWideners.sort(Comparator.comparing(FileFingerprint::path));

        TransformedMinecraftProvider.TransformedJars jars;
        synchronized (transformedMinecraftProvider) {
            jars = transformedMinecraftProvider.provide(version, minecraft, mappingPath, fingerprintedAccessWideners, refresh || this.refresh);
        }
        return new Everything(jars, mappingPath.path());
    }

    public record Everything(
        TransformedMinecraftProvider.TransformedJars jars, Path mappings
    ) {
    }

    private FileFingerprint fingerprint(Path path) throws IOException {
        if (path.startsWith(globalGradleCache)) {
            LOG.info("{} is in the global cache, using file metadata.", path);
            return FileFingerprint.createImmutable(path);
        } else {
            LOG.info("{} is elsewhere, caching normally.", path);
            return FileFingerprint.createDefault(path);
        }
    }

    @Override
    public void onFinish(FinishEvent event) {
    }
}
