package com.niranjankagri.healing.control;

import java.util.List;

/**
 * Evidence for one heal Healenium proposed during a run, whether the guard let the test use it or not. This is
 * what the review gate checks and what a reviewer reads: what changed, why this element, what else was close.
 *
 * @param test        the test method that was running
 * @param pageObject  page object and method that looked the element up, e.g. {@code LoginPage#submit}
 * @param locator     the out-of-date locator from the page object ({@code By.toString()})
 * @param healedTo    the locator Healenium built for the chosen element
 * @param score       the chosen element's similarity score
 * @param runnerUp    the second-best score, or {@code null} when only one element scored above {@code score-cap}
 * @param decision    what the guard did
 * @param reason      one sentence a person can act on
 * @param candidates  the best candidates, best first (at most {@link #MAX_CANDIDATES})
 * @param screenshot  page screenshot at the moment of the decision, relative to {@code target/healing}
 */
public record HealRecord(
        String test,
        String pageObject,
        String locator,
        String healedTo,
        double score,
        Double runnerUp,
        HealDecision decision,
        String reason,
        List<Candidate> candidates,
        String screenshot) {

    /** Candidates kept per heal: enough to see a tie, short enough to read. */
    public static final int MAX_CANDIDATES = 5;

    public HealRecord {
        candidates = List.copyOf(candidates);
    }

    /** Gap between the chosen element and the runner-up (1.0 when there was no runner-up). */
    public double margin() {
        return runnerUp == null ? 1.0 : score - runnerUp;
    }

    /** The key a reviewer's verdict is stored under: the same heal in the same page-object method. */
    public HealKey key() {
        return new HealKey(pageObject, locator, healedTo);
    }
}
