package com.niranjankagri.healing.control;

/** What the guard did with one heal Healenium proposed. */
public enum HealDecision {
    /** Clear winner, not rejected: the test used the healed element. */
    USED,
    /** The best and second-best candidates scored too close together: stopped for a person to decide. */
    STOPPED_AMBIGUOUS,
    /** A reviewer rejected exactly this heal before: stopped. */
    STOPPED_REJECTED
}
