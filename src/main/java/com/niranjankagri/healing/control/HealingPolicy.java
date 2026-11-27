package com.niranjankagri.healing.control;

import java.nio.file.Path;

/**
 * The rules a heal must pass before a test may use it. Healenium alone uses the best-scoring element as soon as it
 * scores at least {@code score-cap}; this policy adds the two checks Healenium does not make.
 *
 * <ol>
 *   <li><b>Clear winner.</b> The best candidate must beat the runner-up by at least {@link #minMargin()}. Look-alike
 *       elements (three identical "Add" buttons) score the same, and then the "best" one is a guess.</li>
 *   <li><b>Reviewed.</b> A heal a person has rejected in {@link #approvalsFile()} is never used again. A heal nobody
 *       has reviewed yet is used for this run (the test result still counts) but the review gate fails the build
 *       until someone approves or rejects it.</li>
 * </ol>
 *
 * <p>All values come from system properties, so CI and a laptop run the same code:
 * {@code -Dhealing.min-margin=0.05}, {@code -Dhealing.guard=observe} (record only, never stop a heal),
 * {@code -Dhealing.approvals=path}.
 *
 * @param minMargin     smallest score gap between the best and the second-best candidate
 * @param enforce       {@code true}: stop ambiguous and rejected heals; {@code false}: only record them
 * @param approvalsFile the reviewed heals (approved or rejected), checked into the repository
 */
public record HealingPolicy(double minMargin, boolean enforce, Path approvalsFile) {

    /** Gap chosen from real runs: see the README, "Clear winner". */
    public static final double DEFAULT_MIN_MARGIN = 0.05;

    /** Where the reviewed heals live unless {@code -Dhealing.approvals} says otherwise. */
    public static final Path DEFAULT_APPROVALS = Path.of("src", "test", "resources", "healing", "approved-heals.json");

    public HealingPolicy {
        if (minMargin < 0 || minMargin >= 1) {
            throw new IllegalArgumentException("healing.min-margin must be in [0, 1), was " + minMargin);
        }
    }

    /** The policy for this JVM, from system properties with the defaults above. */
    public static HealingPolicy fromSystemProperties() {
        double margin = Double.parseDouble(System.getProperty("healing.min-margin", String.valueOf(DEFAULT_MIN_MARGIN)));
        String mode = System.getProperty("healing.guard", "enforce");
        if (!mode.equals("enforce") && !mode.equals("observe")) {
            throw new IllegalArgumentException("healing.guard must be enforce or observe, was " + mode);
        }
        Path approvals = Path.of(System.getProperty("healing.approvals", DEFAULT_APPROVALS.toString()));
        return new HealingPolicy(margin, mode.equals("enforce"), approvals);
    }
}
