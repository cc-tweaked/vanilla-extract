package cc.tweaked.vanillaextract.decompile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Decompiles classes files to sources.
 *
 * @see VineflowerDecompiler
 */
public interface Decompiler {
    /**
     * The arguments to {@link #decompile(Parameters)}.
     *
     * @param unpickedInput {@link #originalInput()} with constants un-inlined. This should be used as the input to the
     *                      decompiler.
     * @param originalInput The "original" input jar. This should be used as the input to line mapping.
     * @param outputSources The jar to write the decompiled sources from.
     * @param outputClasses The jar to write the line-mapped classes to.
     * @param classpath     The classpath for this decompile job.
     * @param threads       The maximum number of threads to use for the decompiler.
     * @param javadoc       A {@link JavadocProvider} to provide additional javadoc to the compiler.
     */
    record Parameters(
        Path originalInput,
        Path unpickedInput,
        Path outputSources,
        Path outputClasses,
        List<Path> classpath,
        int threads,
        JavadocProvider javadoc
    ) {
    }

    /**
     * Decompile our jar. This should:
     * <ul>
     *     <li>Decompile {@link Parameters#unpickedInput()}, writing it to {@link Parameters#outputSources()}</li>
     *     <li>Apply line mappings to {@link Parameters#originalInput()}, writing them to {@link Parameters#outputClasses()}</li>
     * </ul>
     *
     * @param args The input and output files for the decompiler.
     * @throws IOException When writing to the underlying file.
     */
    void decompile(Parameters args) throws IOException;
}
