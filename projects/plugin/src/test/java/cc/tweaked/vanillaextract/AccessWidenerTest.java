package cc.tweaked.vanillaextract;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.nio.file.Files;

import static cc.tweaked.vanillaextract.MoreAssertions.assertContains;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test building with access wideners.
 */
public class AccessWidenerTest {
    @RegisterExtension
    private final GradleProject project = GradleProject.create("accesswidener");

    /**
     * Changing an access widener causes Minecraft to be re-evaluated.
     */
    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    public void Changing_access_widener_causes_rebuild(TestSupport.ConfigurationCacheMode configurationCache) throws IOException {
        var accessWidener = project.projectDir().resolve("src/main/resources/widener.accesswidener");

        // Attempt to compile with our original access widener. This should succeed.
        {
            var result = project.build(configurationCache, "assemble");
            assertEquals(TaskOutcome.SUCCESS, result.task(":assemble").getOutcome());
            assertContains("widener.accesswidener", result.getOutput());
        }

        project.getMinecraftJars();

        // Rebuild once, and assert that the classes are up-to-date.
        {
            var result = project.build(configurationCache, "assemble");
            assertEquals(TaskOutcome.UP_TO_DATE, result.task(":assemble").getOutcome());
        }

        // Change the access widener, and assert that Minecraft is rebuilt.
        Files.writeString(accessWidener, """
            accessWidener v1 named
            """);
        {
            var result = project.builder(configurationCache, "assemble").buildAndFail();
            assertEquals(TaskOutcome.FAILED, result.task(":compileJava").getOutcome());
        }
    }
}
