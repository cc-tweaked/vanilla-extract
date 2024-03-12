package cc.tweaked.vanillaextract.decompile;

import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;

/**
 * A marker service to prevent multiple decompilations occuring at once.
 */
public abstract class DecompileService implements BuildService<BuildServiceParameters.None> {
    public static final String NAME = "VanillaExtract:Decompile";
}
