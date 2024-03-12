package cc.tweaked.vanillaextract.decompile;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A {@link IResultSaver} which writes sources to one jar, and remapped classes to another jar.
 */
final class VineflowerOutput implements IResultSaver {
    private final Path outputSourcesPath;
    private final Path outputClassesPath;

    private final FileSystem inputJar;
    private final Map<String, List<String>> sourceFiles = new HashMap<>();
    private @Nullable ZipOutputStream outputSources;
    private @Nullable ZipOutputStream outputClasses;

    private final byte[] buffer = new byte[8192];

    VineflowerOutput(Path inputJar, Path outputSources, Path outputClasses) throws IOException {
        this.inputJar = FileSystems.newFileSystem(toJarUri(inputJar), Map.of());
        this.outputSourcesPath = outputSources;
        this.outputClassesPath = outputClasses;

        // Build a mapping of source file name to the set of output class files (simply done on a prefix search of $).
        // We use this when copying line-mapped classes.
        Files.walkFileTree(this.inputJar.getPath(""), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                var fileName = file.toString();
                if (!fileName.endsWith(".class")) return FileVisitResult.CONTINUE;

                var dollarIdx = fileName.indexOf('$');
                var baseName = dollarIdx < 0
                    ? fileName.substring(0, fileName.length() - 5) + "java"
                    : fileName.substring(0, dollarIdx) + ".java";
                sourceFiles.computeIfAbsent(baseName, k -> new ArrayList<>(2)).add(fileName);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void createArchive(String path, String archiveName, Manifest manifest) {
        try {
            outputSources = new JarOutputStream(Files.newOutputStream(outputSourcesPath), manifest);
            outputClasses = new JarOutputStream(Files.newOutputStream(outputClassesPath), manifest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(VineflowerOutput.class);

    @Override
    public void copyEntry(String source, String path, String archiveName, String entry) {
        if (outputSources == null || outputClasses == null) throw new IllegalStateException("Archive not created yet");

        try (var stream = Files.newInputStream(inputJar.getPath(entry))) {
            outputSources.putNextEntry(new ZipEntry(entry));
            outputClasses.putNextEntry(new ZipEntry(entry));

            while (true) {
                int bytes = stream.read(buffer);
                if (bytes == -1) break;
                outputSources.write(buffer, 0, bytes);
                outputClasses.write(buffer, 0, bytes);
            }
        } catch (IOException e) {
            LOG.error("Failed to find {} in {}", path, inputJar, e);
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String sourcesName, String content, int[] mapping) {
        if (outputSources == null || outputClasses == null) throw new IllegalStateException("Archive not created yet");

        try {
            outputSources.putNextEntry(new ZipEntry(sourcesName));
            // This seems horribly inefficient, but using a OutputStreamWriter (or print stream) seems worse.
            outputSources.write(content.getBytes(StandardCharsets.UTF_8));

            for (var className : sourceFiles.get(sourcesName)) {
                outputClasses.putNextEntry(new ZipEntry(className));
                outputClasses.write(LineNumberMapper.remapClass(Files.readAllBytes(inputJar.getPath(className)), mapping));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void closeArchive(String path, String archiveName) {
    }

    @Override
    public void close() throws IOException {
        inputJar.close();
        if (outputSources != null) outputSources.close();
        if (outputClasses != null) outputClasses.close();
    }

    @Override
    public void saveDirEntry(String path, String archiveName, String entryName) {
        // Skip directories
    }

    // Operations which operate on non-zips

    @Override
    public void saveFolder(String path) {
    }

    @Override
    public void copyFile(String source, String path, String entryName) {
        throw new UnsupportedOperationException("Cannot copy file");
    }

    @Override
    public void saveClassFile(String path, String qualifiedName, String entryName, String content, int[] mapping) {
        throw new UnsupportedOperationException("Cannot save class file");
    }

    @Override
    public void saveClassEntry(String path, String archiveName, String qualifiedName, String entryName, String content) {
        throw new IllegalStateException("Cannot save class file without mappings");
    }

    private static URI toJarUri(Path path) {
        URI uri = path.toUri();

        try {
            return new URI("jar:" + uri.getScheme(), uri.getHost(), uri.getPath(), uri.getFragment());
        } catch (URISyntaxException e) {
            throw new RuntimeException("can't convert path " + path + " to uri", e);
        }
    }
}
