package cc.tweaked.vanillaextract;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Copy files from one directory to another.
 */
public final class Copier extends SimpleFileVisitor<Path> {
    private final Path sourceDir;
    private final Path targetDir;

    private Copier(Path sourceDir, Path targetDir) {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
        Files.copy(file, targetDir.resolve(sourceDir.relativize(file)));
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
        var newDir = targetDir.resolve(sourceDir.relativize(dir));
        Files.createDirectories(newDir);
        return FileVisitResult.CONTINUE;
    }

    public static void copy(Path from, Path to) throws IOException {
        Files.walkFileTree(from, new Copier(from, to));
    }
}
