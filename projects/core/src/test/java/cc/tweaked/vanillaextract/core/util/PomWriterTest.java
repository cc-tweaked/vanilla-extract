package cc.tweaked.vanillaextract.core.util;

import cc.tweaked.vanillaextract.core.MavenArtifact;
import cc.tweaked.vanillaextract.core.MavenRelease;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PomWriterTest {
    private static String build(Consumer<PomWriter> configure) {
        var pom = new PomWriter("net.minecraft", "minecraft", "1.20.4");
        configure.accept(pom);

        var stringWriter = new StringWriter();
        try (var w = stringWriter) {
            pom.write(w);
        } catch (XMLStreamException | IOException e) {
            throw new IllegalStateException(e);
        }
        return stringWriter.toString();
    }

    @Test
    public void testBasicContents() {
        @Language("xml")
        var expected = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.minecraft</groupId>
                <artifactId>minecraft</artifactId>
                <version>1.20.4</version>
            </project>
            """;
        assertEquals(expected, build(w -> {
        }));
    }

    @Test
    public void testWithDependencies() {
        @Language("xml")
        var expected = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>net.minecraft</groupId>
                <artifactId>minecraft</artifactId>
                <version>1.20.4</version>
                <dependencies>
                    <dependency>
                        <groupId>net.netty</groupId>
                        <artifactId>netty-codec-socks</artifactId>
                        <version>4.1.82.Final</version>
                        <scope>runtime</scope>
                        <exclusions><exclusion><groupId>*</groupId><artifactId>*</artifactId></exclusion></exclusions>
                    </dependency>
                    <dependency>
                        <groupId>com.google.errorprone</groupId>
                        <artifactId>error_prone_annotations</artifactId>
                        <version>2.23.0</version>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        assertEquals(expected, build(w -> w
            .addDependency(MavenArtifact.main(new MavenRelease("net.netty", "netty-codec-socks", "4.1.82.Final")), "runtime", false)
            .addDependency("com.google.errorprone", "error_prone_annotations", "2.23.0", "compile")));
    }

}
