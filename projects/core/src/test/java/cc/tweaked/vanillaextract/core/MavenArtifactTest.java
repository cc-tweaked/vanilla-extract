package cc.tweaked.vanillaextract.core;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MavenArtifactTest {
    public static List<Arguments> getArguments() {
        return List.of(
            Arguments.of("com.example:project:1.0.0", new MavenArtifact("com.example", "project", "1.0.0", null, null)),
            Arguments.of("com.example:project:1.0.0:all", new MavenArtifact("com.example", "project", "1.0.0", "all", null)),
            Arguments.of("com.example:project:1.0.0@zip", new MavenArtifact("com.example", "project", "1.0.0", null, "zip")),
            Arguments.of("com.example:project:1.0.0:all@zip", new MavenArtifact("com.example", "project", "1.0.0", "all", "zip")),
            Arguments.of("com.example:project@foo:1.0.0:all", new MavenArtifact("com.example", "project@foo", "1.0.0", "all", null))
        );
    }

    @ParameterizedTest
    @MethodSource("getArguments")
    public void Parse(String dependency, MavenArtifact artifact) {
        assertEquals(artifact, MavenArtifact.parse(dependency));
    }


    @ParameterizedTest
    @MethodSource("getArguments")
    public void To_dependency_string(String dependency, MavenArtifact artifact) {
        assertEquals(dependency, artifact.toDependencyString());
    }
}
