package cc.tweaked.vanillaextract.core.util;

import net.fabricmc.tinyremapper.FileSystemReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Filter the contents of a jar, either splitting or merging them.
 */
public class JarContentsFilter {
    /**
     * Split the Minecraft jars.
     *
     * @param serverJar  The server jar.
     * @param clientJar  The client jar.
     * @param common     The output jar, which will contain files that appear in the server jar.
     * @param clientOnly The output jar, which will contain files only in the client jar.
     * @throws IOException If we could not split the jar.
     */
    public static void split(Path serverJar, Path clientJar, Path common, Path clientOnly) throws IOException {
        try (var serverRoot = FileSystemReference.openJar(serverJar);
             var clientRoot = FileSystemReference.openJar(clientJar)) {

            var serverEntries = getFiles(serverRoot.getPath("/"));
            var clientEntries = getFiles(clientRoot.getPath("/"));
            checkConsistent(serverEntries, clientEntries);

            Set<String> clientOnlyEntries = new HashSet<>(clientEntries.keySet());
            clientOnlyEntries.removeAll(serverEntries.keySet());

            copyEntries(serverRoot, common, serverEntries.keySet());
            copyEntries(clientRoot, clientOnly, clientOnlyEntries);
        }

        MoreFiles.updateSha(common);
        MoreFiles.updateSha(clientOnly);
    }

    /**
     * Merge the Minecraft jars.
     *
     * @param serverJar The server jar.
     * @param clientJar The client jar.
     * @param merged    The output jar, which will contain files that appear in both jars.
     * @throws IOException If we could not merge the jar.
     */
    public static void merge(Path serverJar, Path clientJar, Path merged) throws IOException {
        try (var serverRoot = FileSystemReference.openJar(serverJar);
             var clientRoot = FileSystemReference.openJar(clientJar)) {

            var serverEntries = getFiles(serverRoot.getPath("/"));
            var clientEntries = getFiles(clientRoot.getPath("/"));
            checkConsistent(serverEntries, clientEntries);

            Set<String> allEntries = new HashSet<>(clientEntries.keySet());
            allEntries.addAll(serverEntries.keySet());

            copyEntries(clientRoot, merged, allEntries);
        }

        MoreFiles.updateSha(merged);
    }

    private static void checkConsistent(Map<String, String> server, Map<String, String> client) {
        for (var clientEntry : client.entrySet()) {
            var path = clientEntry.getKey();
            var clientHash = clientEntry.getValue();

            var serverHash = server.get(path);
            if (serverHash != null && !clientHash.equals(serverHash)) {
                throw new IllegalStateException("Client and server have different contents for " + path);
            }
        }

        for (var path : server.keySet()) {
            if (!client.containsKey(path)) {
                throw new IllegalStateException(path + " appears in the server jar, but not the client jar");
            }
        }
    }

    private static Map<String, String> getFiles(Path root) throws IOException {
        Map<String, String> paths = new HashMap<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.startsWith("/META-INF")) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                paths.put(file.toString(), MoreFiles.computeMd5(file));
                return FileVisitResult.CONTINUE;
            }
        });
        return Collections.unmodifiableMap(paths);
    }

    private static void copyEntries(FileSystemReference source, Path destination, Collection<String> files) throws IOException {
        List<String> sortedFiles = new ArrayList<String>(files);
        sortedFiles.sort(Comparator.naturalOrder());

        try (var scratch = MoreFiles.scratchZip(destination)) {
            try (var output = FileSystemReference.openJar(scratch.path())) {
                // Write the manifest.
                var manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                manifest.write(out);

                Files.createDirectories(output.getPath("META-INF"));
                Files.write(output.getPath("META-INF/MANIFEST.MF"), out.toByteArray());

                // Write each file within the jar
                for (var file : sortedFiles) {
                    var sourceFile = source.getPath(file);
                    var outputFile = output.getPath(file);

                    var parent = outputFile.getParent();
                    if (parent != null) Files.createDirectories(parent);

                    Files.copy(sourceFile, outputFile, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }

            scratch.commit();
        }
    }
}
