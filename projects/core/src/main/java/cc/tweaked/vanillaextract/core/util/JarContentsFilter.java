package cc.tweaked.vanillaextract.core.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Filter the contents of a jar, either splitting or merging them.
 */
public class JarContentsFilter {
    private static final Logger LOG = LoggerFactory.getLogger(JarContentsFilter.class);

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
        try (var serverRoot = ZipFile.builder().setPath(serverJar).get();
             var clientRoot = ZipFile.builder().setPath(clientJar).get()) {

            var serverEntries = getFiles(serverRoot);
            var clientEntries = getFiles(clientRoot);
            checkConsistent(serverEntries, clientEntries);

            var clientOnlyEntries = new HashMap<>(clientEntries);
            clientOnlyEntries.keySet().removeAll(serverEntries.keySet());

            copyEntries(serverRoot, common, serverEntries);
            copyEntries(clientRoot, clientOnly, clientOnlyEntries);
        }
    }

    private static void checkConsistent(Map<String, ZipEntry> server, Map<String, ZipEntry> client) {
        for (var clientEntry : client.entrySet()) {
            var path = clientEntry.getKey();
            var clientHash = clientEntry.getValue().digest();

            var serverEntry = server.get(path);
            if (serverEntry != null && !clientHash.equals(serverEntry.digest())) {
                // This *should* be an error, but Minecraft 1.21.9/1.21.10 has different visibility on one method in
                // MinecraftServer. For now, just do a warning.
                LOG.warn("Client and server have different contents for {}", path);
            }
        }

        for (var path : server.keySet()) {
            if (!client.containsKey(path)) {
                throw new IllegalStateException(path + " appears in the server jar, but not the client jar");
            }
        }
    }

    private record ZipEntry(ZipArchiveEntry entry, String digest) {
    }

    private static Map<String, ZipEntry> getFiles(ZipFile file) throws IOException {
        Map<String, ZipEntry> paths = new HashMap<>();

        var entries = file.getEntries();
        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            if (entry.isDirectory()) continue;
            if (entry.getName().startsWith("META-INF")) continue;

            var digest = MoreDigests.createMd5();
            try (var stream = file.getInputStream(entry)) {
                MoreDigests.digestStream(digest, stream);
            }

            var existing = paths.putIfAbsent(entry.getName(), new ZipEntry(entry, MoreDigests.toHexString(digest)));
            if (existing != null) throw new IllegalStateException("Duplicate zip entry " + entry.getName());
        }

        return Collections.unmodifiableMap(paths);
    }

    private static void copyEntries(ZipFile source, Path destination, Map<String, ZipEntry> files) throws IOException {
        List<ZipEntry> sortedFiles = new ArrayList<>(files.values());
        sortedFiles.sort(Comparator.comparing(a -> a.entry().getName()));

        try (var scratch = MoreFiles.scratchZip(destination)) {
            try (var output = new ZipArchiveOutputStream(scratch.path())) {
                // Write the manifest.
                var manifest = new Manifest();
                manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

                var manifestEntry = new ZipArchiveEntry("META-INF/MANIFEST.MF");
                manifestEntry.setTime(0);

                output.putArchiveEntry(manifestEntry);
                manifest.write(output);
                output.closeArchiveEntry();

                // Write each file within the jar
                for (var file : sortedFiles) {
                    output.addRawArchiveEntry(file.entry(), source.getRawInputStream(file.entry()));
                }
            }

            scratch.commit();
        }
    }
}
