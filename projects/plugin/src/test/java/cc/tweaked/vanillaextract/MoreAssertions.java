package cc.tweaked.vanillaextract;

import cc.tweaked.vanillaextract.core.util.MoreFiles;
import org.junit.jupiter.api.AssertionFailureBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MoreAssertions {
    public static void assertFileExists(Path path) {
        if (!MoreFiles.exists(path)) throw new AssertionError("Expected " + path + " to exist");
    }

    public static void assertJarContains(Path path, Predicate<String> filter, String reason) throws IOException {
        List<String> files = new ArrayList<>();
        try (var fs = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = fs.getNextEntry()) != null) {
                if (filter.test(entry.getName())) return;
                files.add(entry.getName());
            }
        }

        files.sort(Comparator.naturalOrder());

        throw new AssertionError(
            reason + " - No files matched, jar contains:\n" +
            files.stream().map(x -> " - " + x).collect(Collectors.joining("\n"))
        );
    }

    public static void assertContains(String expected, String text) {
        if (text.contains(expected)) return;

        AssertionFailureBuilder.assertionFailure()
            .reason("Expected message to contain " + expected)
            .actual(text)
            .buildAndThrow();
    }

    public static void assertNotContains(String expected, String text) {
        if (!text.contains(expected)) return;

        AssertionFailureBuilder.assertionFailure()
            .reason("Expected message to not contain " + expected)
            .actual(text)
            .buildAndThrow();
    }
}
