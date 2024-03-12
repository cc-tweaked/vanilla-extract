package cc.tweaked.vanillaextract.core.inputs;

/**
 * Collects inputs to a processing step.
 *
 * @see HashingInputCollector
 * @see BuildInput
 */
public interface InputCollector {
    /**
     * Add an input to this collector.
     *
     * @param input The input to add.
     */
    default void addInput(BuildInput input) {
        input.addInputs(this);
    }

    /**
     * The input digest to add.
     *
     * @param digest Add an input digest to the collector.
     */
    void addInputDigest(String digest);
}
