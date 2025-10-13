package cc.tweaked.vanillaextract.core;

import cc.tweaked.vanillaextract.core.download.FileDownloader;
import cc.tweaked.vanillaextract.core.minecraft.MinecraftProvider;
import cc.tweaked.vanillaextract.core.minecraft.manifest.MinecraftVersion.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class TestData {
    public static final Downloads MC_1_20_4 = new Downloads(
        new Download("fd19469fed4a4b4c15b2d5133985f0e3e7816a8a", 24445539, "https://piston-data.mojang.com/v1/objects/fd19469fed4a4b4c15b2d5133985f0e3e7816a8a/client.jar"),
        new Download("be76ecc174ea25580bdc9bf335481a5192d9f3b7", 8897012, "https://piston-data.mojang.com/v1/objects/be76ecc174ea25580bdc9bf335481a5192d9f3b7/client.txt"),
        new Download("8dd1a28015f51b1803213892b50b7b4fc76e594d", 49150256, "https://piston-data.mojang.com/v1/objects/8dd1a28015f51b1803213892b50b7b4fc76e594d/server.jar"),
        new Download("c1cafe916dd8b58ed1fe0564fc8f786885224e62", 6797462, "https://piston-data.mojang.com/v1/objects/c1cafe916dd8b58ed1fe0564fc8f786885224e62/server.txt")
    );

    public static final List<Library> MC_1_20_4_CLIENT_LIBRARIES = List.of(
        new Library(new LibraryDownloads(new LibraryArtifact("", "1227f9e0666314f9de41477e3ec277e542ed7f7b", 1330045, "https://libraries.minecraft.net/ca/weblite/java-objc-bridge/1.1/java-objc-bridge-1.1.jar")), "ca.weblite:java-objc-bridge:1.1", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "943ba26de047eb6b28fff47f5ee939a34eb5fc8e", 970546, "https://libraries.minecraft.net/com/github/oshi/oshi-core/6.4.5/oshi-core-6.4.5.jar")), "com.github.oshi:oshi-core:6.4.5", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "b3add478d4382b78ea20b1671390a858002feb6c", 283367, "https://libraries.minecraft.net/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar")), "com.google.code.gson:gson:2.10.1", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "1dcf1de382a0bf95a3d8b0849546c88bac1292c9", 4617, "https://libraries.minecraft.net/com/google/guava/failureaccess/1.0.1/failureaccess-1.0.1.jar")), "com.google.guava:failureaccess:1.0.1", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "5e64ec7e056456bef3a4bc4c6fdaef71e8ab6318", 3041591, "https://libraries.minecraft.net/com/google/guava/guava/32.1.2-jre/guava-32.1.2-jre.jar")), "com.google.guava:guava:32.1.2-jre", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "61ad4ef7f9131fcf6d25c34b817f90d6da06c9e9", 14567819, "https://libraries.minecraft.net/com/ibm/icu/icu4j/73.2/icu4j-73.2.jar")), "com.ibm.icu:icu4j:73.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "65085e2eb921c4a3ebdde3d248637f1776e6d80d", 115471, "https://libraries.minecraft.net/com/mojang/authlib/6.0.52/authlib-6.0.52.jar")), "com.mojang:authlib:6.0.52", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "5c685c5ffa94c4cd39496c7184c1d122e515ecef", 964, "https://libraries.minecraft.net/com/mojang/blocklist/1.0.10/blocklist-1.0.10.jar")), "com.mojang:blocklist:1.0.10", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "73e324f2ee541493a5179abf367237faa782ed21", 79955, "https://libraries.minecraft.net/com/mojang/brigadier/1.2.9/brigadier-1.2.9.jar")), "com.mojang:brigadier:1.2.9", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "3ba4a30557a9b057760af4011f909ba619fc5125", 689960, "https://libraries.minecraft.net/com/mojang/datafixerupper/6.0.8/datafixerupper-6.0.8.jar")), "com.mojang:datafixerupper:6.0.8", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "832b8e6674a9b325a5175a3a6267dfaf34c85139", 15343, "https://libraries.minecraft.net/com/mojang/logging/1.1.1/logging-1.1.1.jar")), "com.mojang:logging:1.1.1", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "da05971b07cbb379d002cf7eaec6a2048211fefc", 4439, "https://libraries.minecraft.net/com/mojang/patchy/2.2.10/patchy-2.2.10.jar")), "com.mojang:patchy:2.2.10", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "3cad216e3a7f0c19b4b394388bc9ffc446f13b14", 12243, "https://libraries.minecraft.net/com/mojang/text2speech/1.17.9/text2speech-1.17.9.jar")), "com.mojang:text2speech:1.17.9", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "4e3eb3d79888d76b54e28b350915b5dc3919c9de", 360738, "https://libraries.minecraft.net/commons-codec/commons-codec/1.16.0/commons-codec-1.16.0.jar")), "commons-codec:commons-codec:1.16.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "8bb2bc9b4df17e2411533a0708a69f983bf5e83b", 483954, "https://libraries.minecraft.net/commons-io/commons-io/2.13.0/commons-io-2.13.0.jar")), "commons-io:commons-io:2.13.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "4bfc12adfe4842bf07b657f0369c4cb522955686", 61829, "https://libraries.minecraft.net/commons-logging/commons-logging/1.2/commons-logging-1.2.jar")), "commons-logging:commons-logging:1.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "f8f3d8644afa5e6e1a40a3a6aeb9d9aa970ecb4f", 306590, "https://libraries.minecraft.net/io/netty/netty-buffer/4.1.97.Final/netty-buffer-4.1.97.Final.jar")), "io.netty:netty-buffer:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "384ba4d75670befbedb45c4d3b497a93639c206d", 345274, "https://libraries.minecraft.net/io/netty/netty-codec/4.1.97.Final/netty-codec-4.1.97.Final.jar")), "io.netty:netty-codec:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "7cceacaf11df8dc63f23d0fb58e9d4640fc88404", 659930, "https://libraries.minecraft.net/io/netty/netty-common/4.1.97.Final/netty-common-4.1.97.Final.jar")), "io.netty:netty-common:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "abb86c6906bf512bf2b797a41cd7d2e8d3cd7c36", 560040, "https://libraries.minecraft.net/io/netty/netty-handler/4.1.97.Final/netty-handler-4.1.97.Final.jar")), "io.netty:netty-handler:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "cec8348108dc76c47cf87c669d514be52c922144", 37792, "https://libraries.minecraft.net/io/netty/netty-resolver/4.1.97.Final/netty-resolver-4.1.97.Final.jar")), "io.netty:netty-resolver:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "795da37ded759e862457a82d9d92c4d39ce8ecee", 147139, "https://libraries.minecraft.net/io/netty/netty-transport-classes-epoll/4.1.97.Final/netty-transport-classes-epoll-4.1.97.Final.jar")), "io.netty:netty-transport-classes-epoll:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "5514744c588190ffda076b35a9b8c9f24946a960", 40427, "https://libraries.minecraft.net/io/netty/netty-transport-native-epoll/4.1.97.Final/netty-transport-native-epoll-4.1.97.Final-linux-aarch_64.jar")), "io.netty:netty-transport-native-epoll:4.1.97.Final:linux-aarch_64", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "54188f271e388e7f313aea995e82f58ce2cdb809", 38954, "https://libraries.minecraft.net/io/netty/netty-transport-native-epoll/4.1.97.Final/netty-transport-native-epoll-4.1.97.Final-linux-x86_64.jar")), "io.netty:netty-transport-native-epoll:4.1.97.Final:linux-x86_64", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "d469d84265ab70095b01b40886cabdd433b6e664", 43897, "https://libraries.minecraft.net/io/netty/netty-transport-native-unix-common/4.1.97.Final/netty-transport-native-unix-common-4.1.97.Final.jar")), "io.netty:netty-transport-native-unix-common:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "f37380d23c9bb079bc702910833b2fd532c9abd0", 489624, "https://libraries.minecraft.net/io/netty/netty-transport/4.1.97.Final/netty-transport-4.1.97.Final.jar")), "io.netty:netty-transport:4.1.97.Final", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "c24946d46824bd528054bface3231d2ecb7e95e8", 23326598, "https://libraries.minecraft.net/it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar")), "it.unimi.dsi:fastutil:8.5.12", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "88e9a306715e9379f3122415ef4ae759a352640d", 1363209, "https://libraries.minecraft.net/net/java/dev/jna/jna-platform/5.13.0/jna-platform-5.13.0.jar")), "net.java.dev.jna:jna-platform:5.13.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "1200e7ebeedbe0d10062093f32925a912020e747", 1879325, "https://libraries.minecraft.net/net/java/dev/jna/jna/5.13.0/jna-5.13.0.jar")), "net.java.dev.jna:jna:5.13.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "4fdac2fbe92dfad86aa6e9301736f6b4342a3f5c", 78146, "https://libraries.minecraft.net/net/sf/jopt-simple/jopt-simple/5.0.4/jopt-simple-5.0.4.jar")), "net.sf.jopt-simple:jopt-simple:5.0.4", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "691a8b4e6cf4248c3bc72c8b719337d5cb7359fa", 1039712, "https://libraries.minecraft.net/org/apache/commons/commons-compress/1.22/commons-compress-1.22.jar")), "org.apache.commons:commons-compress:1.22", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "b7263237aa89c1f99b327197c41d0669707a462e", 632267, "https://libraries.minecraft.net/org/apache/commons/commons-lang3/3.13.0/commons-lang3-3.13.0.jar")), "org.apache.commons:commons-lang3:3.13.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "e5f6cae5ca7ecaac1ec2827a9e2d65ae2869cada", 780321, "https://libraries.minecraft.net/org/apache/httpcomponents/httpclient/4.5.13/httpclient-4.5.13.jar")), "org.apache.httpcomponents:httpclient:4.5.13", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "51cf043c87253c9f58b539c9f7e44c8894223850", 327891, "https://libraries.minecraft.net/org/apache/httpcomponents/httpcore/4.4.16/httpcore-4.4.16.jar")), "org.apache.httpcomponents:httpcore:4.4.16", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "ea1b37f38c327596b216542bc636cfdc0b8036fa", 317566, "https://libraries.minecraft.net/org/apache/logging/log4j/log4j-api/2.19.0/log4j-api-2.19.0.jar")), "org.apache.logging.log4j:log4j-api:2.19.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "3b6eeb4de4c49c0fe38a4ee27188ff5fee44d0bb", 1864386, "https://libraries.minecraft.net/org/apache/logging/log4j/log4j-core/2.19.0/log4j-core-2.19.0.jar")), "org.apache.logging.log4j:log4j-core:2.19.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "5c04bfdd63ce9dceb2e284b81e96b6a70010ee10", 27721, "https://libraries.minecraft.net/org/apache/logging/log4j/log4j-slf4j2-impl/2.19.0/log4j-slf4j2-impl-2.19.0.jar")), "org.apache.logging.log4j:log4j-slf4j2-impl:2.19.0", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "22566d58af70ad3d72308bab63b8339906deb649", 712082, "https://libraries.minecraft.net/org/joml/joml/1.10.5/joml-1.10.5.jar")), "org.joml:joml:1.10.5", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "757920418805fb90bfebb3d46b1d9e7669fca2eb", 135828, "https://libraries.minecraft.net/org/lwjgl/lwjgl-glfw/3.3.2/lwjgl-glfw-3.3.2.jar")), "org.lwjgl:lwjgl-glfw:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "877e17e39ebcd58a9c956dc3b5b777813de0873a", 43233, "https://libraries.minecraft.net/org/lwjgl/lwjgl-jemalloc/3.3.2/lwjgl-jemalloc-3.3.2.jar")), "org.lwjgl:lwjgl-jemalloc:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "ae5357ed6d934546d3533993ea84c0cfb75eed95", 108230, "https://libraries.minecraft.net/org/lwjgl/lwjgl-openal/3.3.2/lwjgl-openal-3.3.2.jar")), "org.lwjgl:lwjgl-openal:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "ee8e95be0b438602038bc1f02dc5e3d011b1b216", 928871, "https://libraries.minecraft.net/org/lwjgl/lwjgl-opengl/3.3.2/lwjgl-opengl-3.3.2.jar")), "org.lwjgl:lwjgl-opengl:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "a2550795014d622b686e9caac50b14baa87d2c70", 118874, "https://libraries.minecraft.net/org/lwjgl/lwjgl-stb/3.3.2/lwjgl-stb-3.3.2.jar")), "org.lwjgl:lwjgl-stb:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "9f65c248dd77934105274fcf8351abb75b34327c", 13404, "https://libraries.minecraft.net/org/lwjgl/lwjgl-tinyfd/3.3.2/lwjgl-tinyfd-3.3.2.jar")), "org.lwjgl:lwjgl-tinyfd:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "4421d94af68e35dcaa31737a6fc59136a1e61b94", 786196, "https://libraries.minecraft.net/org/lwjgl/lwjgl/3.3.2/lwjgl-3.3.2.jar")), "org.lwjgl:lwjgl:3.3.2", null),
        new Library(new LibraryDownloads(new LibraryArtifact("", "41eb7184ea9d556f23e18b5cb99cad1f8581fc00", 63635, "https://libraries.minecraft.net/org/slf4j/slf4j-api/2.0.7/slf4j-api-2.0.7.jar")), "org.slf4j:slf4j-api:2.0.7", null)
    );

    public static final Downloads MC_1_21_10 = new Downloads(
        new Download("d3bdf582a7fa723ce199f3665588dcfe6bf9aca8", 30592168, "https://piston-data.mojang.com/v1/objects/d3bdf582a7fa723ce199f3665588dcfe6bf9aca8/client.jar"),
        new Download("7e62354a697f95cf5e7d5981face0583676a9ef7", 11511143, "https://piston-data.mojang.com/v1/objects/7e62354a697f95cf5e7d5981face0583676a9ef7/client.txt"),
        new Download("95495a7f485eedd84ce928cef5e223b757d2f764", 58642415, "https://piston-data.mojang.com/v1/objects/95495a7f485eedd84ce928cef5e223b757d2f764/server.jar"),
        new Download("c5440743411a6fd7490fa18a4b6c5d8edf36d88b", 8473286, "https://piston-data.mojang.com/v1/objects/c5440743411a6fd7490fa18a4b6c5d8edf36d88b/server.txt")
    );

    private TestData() {
    }

    public static MinecraftProvider.SplitArtifacts setupMinecraft(Path dir, FileDownloader downloader) throws IOException {
        var provider = new MinecraftProvider(downloader);
        var rawArtifacts = provider.provideRaw(dir, MC_1_20_4, MC_1_20_4_CLIENT_LIBRARIES);
        return provider.provideSplit(dir, rawArtifacts, false);
    }
}
