package cc.tweaked.vanillaextract.decompile;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VineflowerDecompilerTest {
    private static final String CLASS_PATH = "cc/tweaked/vanillaextract/decompile/ExampleClass.class";
    private static final String CLASS_NESTED_PATH = "cc/tweaked/vanillaextract/decompile/ExampleClass$Nested.class";
    private static final String SOURCE_PATH = "cc/tweaked/vanillaextract/decompile/ExampleClass.java";

    @TempDir
    private Path path;

    @BeforeEach
    public void setup() throws IOException {
        try (var fileOutput = Files.newOutputStream(path.resolve("input.jar"));
             var zipOutput = new JarOutputStream(fileOutput, new Manifest())) {
            addEntry(zipOutput, CLASS_PATH);
            addEntry(zipOutput, CLASS_NESTED_PATH);
        }
    }

    private static void addEntry(JarOutputStream zipOutput, String path) throws IOException {
        zipOutput.putNextEntry(new ZipEntry(path));
        try (var inputFile = VineflowerDecompilerTest.class.getClassLoader().getResourceAsStream(path)) {
            inputFile.transferTo(zipOutput);
        }
    }

    @Test
    public void testDecompileSources() throws IOException {
        VineflowerDecompiler.get().decompile(new Decompiler.Parameters(
            path.resolve("input.jar"),
            path.resolve("input.jar"),
            path.resolve("output-sources.jar"),
            path.resolve("output-classes.jar"),
            List.of(),
            1,
            JavadocProvider.none(),
            false
        ));

        String contents;
        try (var zipInput = new ZipFile(path.resolve("output-sources.jar").toFile())) {
            assertEquals(
                List.of("META-INF/MANIFEST.MF", SOURCE_PATH),
                zipInput.stream().map(ZipEntry::getName).toList()
            );
            contents = new String(zipInput.getInputStream(zipInput.getEntry(SOURCE_PATH)).readAllBytes(), StandardCharsets.UTF_8);
        }

        @Language("java")
        var expected = """
            package cc.tweaked.vanillaextract.decompile;

            public class ExampleClass {
                public static void main(String[] args) {
                    System.out.println("Hello, world");
                }

                public static class Nested {
                }
            }
            """;
        assertEquals(expected, contents);
    }

    @Test
    public void testDecompileClasses() throws IOException {
        VineflowerDecompiler.get().decompile(new Decompiler.Parameters(
            path.resolve("input.jar"),
            path.resolve("input.jar"),
            path.resolve("output-sources.jar"),
            path.resolve("output-classes.jar"),
            List.of(),
            1,
            JavadocProvider.none(),
            false
        ));

        var originalNode = new ClassNode();
        try (var inputFile = getClass().getClassLoader().getResourceAsStream(CLASS_PATH)) {
            new ClassReader(inputFile).accept(originalNode, 0);
        }

        {
            var main = originalNode.methods.get(1);
            assertEquals("main", main.name);

            var instructions = new ArrayList<>();
            for (var insn : main.instructions) instructions.add(insn);
            assertEquals(
                List.of(5, 8),
                instructions.stream().filter(LineNumberNode.class::isInstance).map(LineNumberNode.class::cast).map(x -> x.line).toList(),
                "Original line numbers are as-expected."
            );
        }

        var remappedNode = new ClassNode();
        try (var zipInput = new ZipFile(path.resolve("output-classes.jar").toFile())) {
            assertEquals(
                Set.of("META-INF/MANIFEST.MF", CLASS_PATH, CLASS_NESTED_PATH),
                zipInput.stream().map(ZipEntry::getName).collect(Collectors.toUnmodifiableSet())
            );

            new ClassReader(zipInput.getInputStream(zipInput.getEntry(CLASS_PATH))).accept(remappedNode, 0);
        }

        {
            var main = remappedNode.methods.get(1);
            assertEquals("main", main.name);

            var instructions = new ArrayList<>();
            for (var insn : main.instructions) instructions.add(insn);
            assertEquals(
                List.of(5, 5),
                instructions.stream().filter(LineNumberNode.class::isInstance).map(LineNumberNode.class::cast).map(x -> x.line).toList(),
                "Remapped line numbers are as expected."
            );
        }
    }
}
