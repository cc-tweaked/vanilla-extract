package cc.tweaked.vanillaextract.core.util;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MoreFilesTest {
    @Test
    public void addSuffix_adds_a_suffix() {
        assertEquals(Path.of("foo/bar-suffix.txt"), MoreFiles.addSuffix(Path.of("foo/bar.txt"), "-suffix"));
    }

    @Test
    public void addSuffix_fails_on_no_extension() {
        assertThrows(IllegalArgumentException.class, () -> MoreFiles.addSuffix(Path.of("foo/bar"), "-suffix"));
    }
}
