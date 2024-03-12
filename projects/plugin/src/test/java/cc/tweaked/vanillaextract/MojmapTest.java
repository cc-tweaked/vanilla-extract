package cc.tweaked.vanillaextract;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;

import static cc.tweaked.vanillaextract.MoreAssertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests using the {@code mojmap} project.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MojmapTest {
    private static final String SET_UP_MINECRAFT_MESSAGE = "Set up Minecraft 1.20.4";

    @RegisterExtension
    private final GradleProject project = GradleProject.create("mojmap");

    /**
     * Attempt to compile the project, asserting that the Minecraft jars are created.
     * <p>
     * This project makes use of multiple source sets, so we also test that the configurations work together.
     */
    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    public void Compile_creates_minecraft_jars(TestSupport.ConfigurationCacheMode configurationCache) throws IOException {
        var result = project.build(configurationCache, "assemble", "testClasses");

        assertEquals(TaskOutcome.SUCCESS, result.task(":assemble").getOutcome());
        assertContains(SET_UP_MINECRAFT_MESSAGE, result.getOutput());

        var versions = project.getMinecraftJars();
        assertFileExists(versions.commonJar(null));
        assertJarContains(versions.commonJar(null), x -> x.endsWith(".class"), "Jar contains no class files");

        assertFileExists(versions.clientJar(null));
        assertJarContains(versions.clientJar(null), x -> x.endsWith(".class"), "Jar contains no class files");
    }

    /**
     * Test that we can decompile the game, and the resulting file contains java files.
     */
    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    @Tag("slow")
    public void Decompile_creates_source_files(TestSupport.ConfigurationCacheMode configurationCache) throws IOException {
        var result = project.build(configurationCache, "decompile");
        assertEquals(TaskOutcome.SUCCESS, result.task(":decompile").getOutcome());

        var versions = project.getMinecraftJars();
        assertFileExists(versions.commonJar("sources"));
        assertJarContains(versions.commonJar("sources"), x -> x.endsWith(".java"), "Jar contains no source files");

        assertFileExists(versions.clientJar("sources"));
        assertJarContains(versions.clientJar("sources"), x -> x.endsWith(".java"), "Jar contains no source files");
    }

    /**
     * Test that we lazily set up Minecraft - it's not something we configure immediately.
     */
    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    public void Minecraft_is_not_set_up_when_not_needed(TestSupport.ConfigurationCacheMode configurationCache) {
        var result = project.build(configurationCache, "projects");

        assertEquals(TaskOutcome.SUCCESS, result.task(":projects").getOutcome());
        assertNotContains(SET_UP_MINECRAFT_MESSAGE, result.getOutput());
    }

    /**
     * Test we reuse the configuration cache in various cases.
     */
    @Test
    public void Configuration_cache_is_reused() throws IOException {
        // Do a fresh compile
        {
            var result = project.build(TestSupport.ConfigurationCacheMode.WITH_CACHE, "assemble");
            assertEquals(TaskOutcome.SUCCESS, result.task(":assemble").getOutcome());
            assertContains(SET_UP_MINECRAFT_MESSAGE, result.getOutput());
        }

        // We sometimes take extra builds to actually populate the configuration cache, so do one more just in case.
        {
            var result = project.build(TestSupport.ConfigurationCacheMode.WITH_CACHE, "assemble");
            assertEquals(TaskOutcome.UP_TO_DATE, result.task(":assemble").getOutcome());
        }

        // Then assert we reuse the cache when nothing changes.
        {
            var result = project.build(TestSupport.ConfigurationCacheMode.WITH_CACHE, "assemble");
            assertEquals(TaskOutcome.UP_TO_DATE, result.task(":assemble").getOutcome());
            assertNotContains(SET_UP_MINECRAFT_MESSAGE, result.getOutput());
            assertContains("Reusing configuration cache.", result.getOutput());
        }

        // Then assert we reuse the cache even when files change.
        {
            Files.writeString(project.projectDir().resolve("src/main/java/cc/tweaked/test/Another.java"), """
                package cc.tweaked.test;
                class Another {}
                """
            );

            var result = project.build(TestSupport.ConfigurationCacheMode.WITH_CACHE, "assemble");
            assertEquals(TaskOutcome.SUCCESS, result.task(":assemble").getOutcome());
            assertNotContains(SET_UP_MINECRAFT_MESSAGE, result.getOutput());
            assertContains("Reusing configuration cache.", result.getOutput());
        }
    }

}
