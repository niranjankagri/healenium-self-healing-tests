package com.niranjankagri.healing.control;

import org.openqa.selenium.NoSuchElementException;

/**
 * Thrown instead of using a heal that the {@link HealingPolicy} does not allow: no clear winner, or a heal that was
 * rejected in review. It is a {@link NoSuchElementException} on purpose: to the test the element is simply not
 * found (as without Healenium), and Healenium passes this exception through unchanged, so the message reaches the
 * test report.
 */
public class HealingStoppedException extends NoSuchElementException {

    private final HealDecision decision;

    public HealingStoppedException(HealDecision decision, String message) {
        super(message);
        this.decision = decision;
    }

    /** Why the heal was stopped. */
    public HealDecision decision() {
        return decision;
    }
}
