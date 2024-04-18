package cc.tweaked.vanillaextract.decompile;

import net.fabricmc.fernflower.api.IFabricJavadocProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Decompiles a jar using Vineflower
 *
 * @see <a href="https://vineflower.org/">Vineflower</a>
 */
public class VineflowerDecompiler implements Decompiler {
    private static final Logger LOG = LoggerFactory.getLogger(VineflowerDecompiler.class);
    private static final Decompiler instance = new VineflowerDecompiler();

    private VineflowerDecompiler() {
    }

    public static Decompiler get() {
        return instance;
    }

    @Override
    public void decompile(Parameters args) throws IOException {
        var saver = new VineflowerOutput(args.originalInput(), args.outputSources(), args.outputClasses());
        var severity = args.log() ? getSeverity() : IFernflowerLogger.Severity.ERROR;
        var decompiler = new Fernflower(saver, Map.of(
            IFernflowerPreferences.BYTECODE_SOURCE_MAPPING, "1",
            IFernflowerPreferences.INDENT_STRING, "    ",
            IFernflowerPreferences.LOG_LEVEL, severity.name(),
            IFernflowerPreferences.THREADS, Integer.toString(args.threads()),
            IFabricJavadocProvider.PROPERTY_NAME, new JavadocAdaptor(args.javadoc())
        ), new LogAdaptor());

        for (var classpath : args.classpath()) decompiler.addLibrary(classpath.toFile());
        decompiler.addSource(args.unpickedInput().toFile());

        try {
            decompiler.decompileContext();
        } catch (UncheckedIOException e) {
            throw e.getCause();
        } finally {
            decompiler.clearContext();
            System.gc();
        }
    }

    public static IFernflowerLogger.Severity getSeverity() {
        if (LOG.isTraceEnabled()) return IFernflowerLogger.Severity.TRACE;
        if (LOG.isInfoEnabled()) return IFernflowerLogger.Severity.INFO;
        if (LOG.isWarnEnabled()) return IFernflowerLogger.Severity.WARN;
        return IFernflowerLogger.Severity.ERROR;
    }

    private static final class LogAdaptor extends IFernflowerLogger {
        @Override
        public void writeMessage(String message, Severity severity) {
            if (!accepts(severity)) return;
            switch (severity) {
                case TRACE -> LOG.trace(message);
                case INFO -> LOG.info(message);
                case WARN -> LOG.warn(message);
                case ERROR -> LOG.error(message);
            }
        }

        @Override
        public void writeMessage(String message, Severity severity, Throwable t) {
            if (!accepts(severity)) return;
            switch (severity) {
                case TRACE -> LOG.trace(message, t);
                case INFO -> LOG.info(message, t);
                case WARN -> LOG.warn(message, t);
                case ERROR -> LOG.error(message, t);
            }
        }
    }

    private record JavadocAdaptor(JavadocProvider provider) implements IFabricJavadocProvider {
        @Override
        public @Nullable String getClassDoc(StructClass klass) {
            // TODO: Check for records
            return provider.getClassDoc(klass.qualifiedName);
        }

        @Override
        public @Nullable String getFieldDoc(StructClass owner, StructField member) {
            // TODO: Check for records
            return provider.getFieldDocs(owner.qualifiedName, member.getName(), member.getDescriptor());
        }

        @Override
        public @Nullable String getMethodDoc(StructClass owner, StructMethod member) {
            return provider.getMethodDoc(owner.qualifiedName, member.getClassQualifiedName(), member.getDescriptor());
        }
    }
}
