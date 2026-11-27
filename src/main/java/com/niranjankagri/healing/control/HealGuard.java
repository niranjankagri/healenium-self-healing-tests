package com.niranjankagri.healing.control;

import java.util.Locale;

/**
 * The decision rules, without Selenium or Healenium, so they can be unit tested. In this order:
 *
 * <ol>
 *   <li>Rejected in review: stop. A person already said this element is wrong.</li>
 *   <li>Approved in review: use. A person checked this exact heal, even if its score was a tie.</li>
 *   <li>No clear winner (gap to the runner-up below {@code min-margin}): stop for a person to decide.</li>
 *   <li>Otherwise: use it for this run. It is not reviewed yet, so the review gate fails the build.</li>
 * </ol>
 *
 * <p>Why both rules: on the real runs the clear-winner rule catches look-alike ties, but one wrong heal
 * ({@code ProductCard#buttonLabel}) wins clearly (0.897 against 0.781). Only a review catches that one.
 */
public final class HealGuard {

    /** Scores are doubles: 0.85 - 0.80 is 0.04999..., which must still count as a gap of 0.05. */
    private static final double ROUNDING = 1e-9;

    private final HealingPolicy policy;
    private final ReviewedHeals reviewed;

    public HealGuard(HealingPolicy policy, ReviewedHeals reviewed) {
        this.policy = policy;
        this.reviewed = reviewed;
    }

    /** A decision and the sentence that explains it. */
    public record Verdict(HealDecision decision, String reason) {
    }

    /**
     * @param key      the heal Healenium proposes
     * @param score    the chosen element's score
     * @param runnerUp the second-best score, {@code null} if no other element scored above {@code score-cap}
     */
    public Verdict decide(HealKey key, double score, Double runnerUp) {
        var review = reviewed.verdictOn(key);
        if (review.isPresent()) {
            return review.get().verdict() == ReviewedHeals.Verdict.rejected
                    ? new Verdict(HealDecision.STOPPED_REJECTED, "Rejected in review: " + review.get().note())
                    : new Verdict(HealDecision.USED, "Approved in review: " + review.get().note());
        }
        if (runnerUp != null && score - runnerUp < policy.minMargin() - ROUNDING) {
            return new Verdict(HealDecision.STOPPED_AMBIGUOUS, String.format(Locale.ROOT,
                    "No clear winner: best %.3f, runner-up %.3f (gap %.3f, needs %.2f). A person must pick the element",
                    score, runnerUp, score - runnerUp, policy.minMargin()));
        }
        String gap = runnerUp == null ? "only candidate" : String.format(Locale.ROOT, "gap %.3f", score - runnerUp);
        return new Verdict(HealDecision.USED, "Clear winner (" + gap + "), not reviewed yet");
    }
}
