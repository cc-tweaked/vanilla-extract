package cc.tweaked.vanillaextract.core.inputs;

import cc.tweaked.vanillaextract.core.util.MoreDigests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A {@link InputCollector} which accumulates inputs as a hash.
 */
public final class HashingInputCollector implements InputCollector {
    private static final Logger LOG = LoggerFactory.getLogger(HashingInputCollector.class);

    private final String name;
    private final MessageDigest messageDigest = MoreDigests.createMd5();
    private final Deque<DigestTrace> traceStack = new ArrayDeque<>();

    /**
     * Construct a new input collector.
     *
     * @param name The name of the collector, for logging.
     */
    public HashingInputCollector(String name) {
        this.name = name;
        this.traceStack.addLast(new DigestTrace(name));
    }

    @Override
    public void addInput(BuildInput input) {
        LOG.info("Adding {} as input to {}", input, name);

        var trace = new DigestTrace(input.toString());
        traceStack.addLast(trace);

        InputCollector.super.addInput(input);

        if (traceStack.removeLast() != trace) throw new IllegalStateException("Mismatched trace stack");
        traceStack.getLast().children().add(trace);
    }

    @Override
    public void addInputDigest(String digest) {
        traceStack.getLast().digests().add(digest);
        this.messageDigest.update(digest.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String toString() {
        var builder = new StringBuilder();
        traceStack.getFirst().toString(builder, 2);
        return builder.toString();
    }

    /**
     * Get the final digest for this collector.
     *
     * @return The final digest.
     */
    public String getDigest() {
        return MoreDigests.toHexString(messageDigest);
    }

    private record DigestTrace(
        String name,
        List<DigestTrace> children,
        List<String> digests
    ) {
        DigestTrace(String name) {
            this(name, new ArrayList<>(0), new ArrayList<>(0));
        }

        void toString(StringBuilder out, int indent) {
            out.append(" ".repeat(indent)).append(name);
            if (children.isEmpty() && digests.isEmpty()) {
                out.append(" - (empty)\n");
            } else if (children.isEmpty() && digests.size() == 1) {
                out.append(" - ").append(digests.get(0)).append("\n");
            } else {
                out.append(":\n");
                for (var digest : digests) out.append(" ".repeat(indent)).append(" - ").append(digest).append("\n");
                for (var child : children) child.toString(out, indent + 2);
            }
        }
    }
}
