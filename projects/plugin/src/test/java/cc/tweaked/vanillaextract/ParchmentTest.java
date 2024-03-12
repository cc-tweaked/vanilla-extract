package cc.tweaked.vanillaextract;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Test building with Parchment.
 */
public class ParchmentTest {
    @RegisterExtension
    private final GradleProject project = GradleProject.create("parchment");

    @ParameterizedTest
    @EnumSource(TestSupport.ConfigurationCacheMode.class)
    public void test(TestSupport.ConfigurationCacheMode configurationCache) {
        var result = project.build(configurationCache, "assemble");
        assertNotEquals(TaskOutcome.FAILED, result.task(":assemble").getOutcome());
    }
}
