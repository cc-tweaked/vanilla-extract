package cc.tweaked.vanillaextract;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test compiling and decompiling with Unpick configurations.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class UnpickTest {
    @RegisterExtension
    private final GradleProject project = GradleProject.create("unpick");

    /**
     * Compile the game with unpick configurations. Unpick should have no impact on the mappings used, so this isn't
     * especially interesting/useful a test.
     */
    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    public void Assemble(TestSupport.ConfigurationCacheMode configurationCache) {
        var result = project.build(configurationCache, "assemble");
        assertNotEquals(TaskOutcome.FAILED, result.task(":assemble").getOutcome());
    }

    /**
     * Decompile the game with unpick configurations.
     */
    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    @Tag("slow")
    public void Decompile(TestSupport.ConfigurationCacheMode configurationCache) {
        var result = project.build(configurationCache, "decompile");
        assertEquals(TaskOutcome.SUCCESS, result.task(":decompile").getOutcome());
    }
}
