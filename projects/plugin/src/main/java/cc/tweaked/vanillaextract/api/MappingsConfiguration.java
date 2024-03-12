package cc.tweaked.vanillaextract.api;

import org.gradle.api.Action;

/**
 * Configure mappings for a project.
 * <p>
 * Exactly one function should be called from this class (e.g. {@code mappings { official() }}).
 *
 * @see VanillaMinecraftExtension#mappings(Action)
 */
public interface MappingsConfiguration {
    /**
     * Use the official mappings bundled with Minecraft.
     */
    void official();

    /**
     * Use parchment mappings.
     *
     * @param mcVersion The Minecraft version these mappings were created for.
     * @param version   The date these mappings were created.
     */
    void parchment(String mcVersion, String version);
}
