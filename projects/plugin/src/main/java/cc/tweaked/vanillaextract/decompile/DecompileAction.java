package cc.tweaked.vanillaextract.decompile;

import cc.tweaked.vanillaextract.core.mappings.MappingsFileProvider;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A work action to run our decompiler.
 * <p>
 * The actual action is run in an isolated VM, with our decompiler dependencies on the classpath.
 *
 * @see Decompile Setting up the dependencies.
 * @see DecompileTask The task for starting this action.
 */
public abstract class DecompileAction implements WorkAction<DecompileAction.Parameters> {
    public interface Parameters extends WorkParameters {
        RegularFileProperty getOriginalInput();

        RegularFileProperty getUnpickedInput();

        RegularFileProperty getOutputSources();

        RegularFileProperty getOutputClasses();

        ConfigurableFileCollection getClasspath();

        Property<Integer> getThreadCount();

        RegularFileProperty getMappings();
    }

    @Override
    public void execute() {
        var parameters = getParameters();

        try {
            var mappingTree = new MemoryMappingTree();
            MappingsFileProvider.readMappings(getParameters().getMappings().get().getAsFile().toPath(), mappingTree);

            VineflowerDecompiler.get().decompile(new Decompiler.Parameters(
                parameters.getOriginalInput().get().getAsFile().toPath(),
                parameters.getUnpickedInput().get().getAsFile().toPath(),
                parameters.getOutputSources().get().getAsFile().toPath(),
                parameters.getOutputClasses().get().getAsFile().toPath(),
                parameters.getClasspath().getFiles().stream().map(File::toPath).toList(),
                parameters.getThreadCount().get(),
                new MappingsJavadocProvider(mappingTree)
            ));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
