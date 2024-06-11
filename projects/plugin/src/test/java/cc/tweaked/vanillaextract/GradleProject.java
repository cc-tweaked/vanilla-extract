package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.core.minecraft.TransformedMinecraftProvider;
import cc.tweaked.vanillaextract.core.util.MoreFiles;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GradleProject implements BeforeEachCallback, AfterEachCallback {
    private static final Path testsDir = Path.of("src/test/resources/tests").toAbsolutePath();

    private final Path originalDir;
    private @Nullable Path projectDir;

    private GradleProject(String name) {
        this.originalDir = testsDir.resolve(name);
    }

    public static GradleProject create(String name) {
        return new GradleProject(name);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws IOException {
        // Try to delete the cache directory if it exists
        var projectDir = this.projectDir = Files.createTempDirectory("VanillaExtract-" + originalDir.getFileName().toString());
        MoreFiles.copyRecursively(originalDir, projectDir);
    }

    @Override
    public void afterEach(ExtensionContext context) throws IOException {
        if (projectDir != null) {
            MoreFiles.deleteRecursively(projectDir);
            projectDir = null;
        }
    }

    public GradleRunner builder(TestSupport.ConfigurationCacheMode configurationCache, String... task) {
        return GradleRunner.create()
            .withProjectDir(projectDir().toFile())
            .withArguments(TestSupport.getGradleArguments(configurationCache, task))
            .withPluginClasspath()
            .forwardOutput();
    }

    public BuildResult build(TestSupport.ConfigurationCacheMode configurationCache, String... task) {
        return builder(configurationCache, task).build();
    }

    public Path projectDir() {
        if (projectDir == null) throw new IllegalStateException("Project has not been set up yet");
        return projectDir;
    }

    public Path localCache() {
        return projectDir().resolve(".gradle/caches/VanillaExtract");
    }

    public Path localMaven() {
        return localCache().resolve(GlobalMinecraftProvider.MAVEN_DIRECTORY);
    }

    public MinecraftJars getMinecraftJars() throws IOException {
        var minecraftDir = localMaven().resolve(TransformedMinecraftProvider.GROUP.replace('.', '/'));
        var commonFiles = getSinglePath(minecraftDir.resolve(TransformedMinecraftProvider.COMMON_MODULE));
        var clientFiles = getSinglePath(minecraftDir.resolve(TransformedMinecraftProvider.CLIENT_ONLY_MODULE));
        return new MinecraftJars(commonFiles, clientFiles);
    }

    private static Path getSinglePath(Path dir) throws IOException {
        List<Path> paths;
        try (var stream = Files.list(dir)) {
            paths = stream.toList();
        }

        if (paths.size() != 1) throw new AssertionError("Expected a single file, got " + dir);
        return paths.get(0);
    }

    public record MinecraftJars(Path commonDir, Path clientDir) {
        public Path commonJar(@Nullable String suffix) {
            return getJar(commonDir, TransformedMinecraftProvider.COMMON_MODULE, suffix);
        }

        public Path clientJar(@Nullable String suffix) {
            return getJar(clientDir(), TransformedMinecraftProvider.CLIENT_ONLY_MODULE, suffix);
        }

        private static Path getJar(Path dir, String group, @Nullable String suffix) {
            return dir.resolve(group + "-" + dir.getFileName() + (suffix == null ? "" : "-" + suffix) + ".jar");
        }
    }
}
