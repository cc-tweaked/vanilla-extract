package cc.tweaked.vanillaextract.core.inputs;

/**
 * An input to some processing step.
 *
 * @see InputCollector
 * @see FileFingerprint
 */
public interface BuildInput {
    /**
     * Add our inputs to the {@link InputCollector}.
     * <p>
     * This should not be called directly - use {@link InputCollector#addInput(BuildInput)} instead.
     *
     * @param collector The {@link InputCollector} to add our dependencies to.
     */
    void addInputs(InputCollector collector);
}
