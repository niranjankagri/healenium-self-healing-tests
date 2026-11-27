package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.control.HealRecord;
import com.niranjankagri.healing.control.HealingJournal;
import com.niranjankagri.healing.control.HealingPolicy;
import com.niranjankagri.healing.control.ReviewGate;
import com.niranjankagri.healing.control.ReviewGate.Status;
import com.niranjankagri.healing.control.ReviewedHeals;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 5: the heal review gate, on the real heals of this run (the journal filled by steps 2 and 3). It runs last
 * in {@code testng.xml}; on its own the journal is empty and these tests are skipped, not passed.
 *
 * <p>Each test writes {@code target/healing/review-*.md} (CI puts it in the job summary) and, for heals that still
 * need a verdict, {@code review-*-proposed.json} in the format of {@code approved-heals.json}.
 */
public class HealReviewGateTest {

    @Test(description = "Day one: the gate FAILS, every heal a test used still needs a person's verdict")
    public void dayOneFailsWithEveryUsedHealToReview() {
        ReviewGate.Result result = ReviewGate.evaluate(heals(Release2FirstReviewTest.class), ReviewedHeals.none());
        ReviewGate.writeReport(result, HealingJournal.FOLDER, "Release 2 day one");

        assertThat(result.passed()).isFalse();
        assertThat(result.count(Status.APPROVED)).isZero();
        assertThat(result.count(Status.NEEDS_REVIEW)).isPositive();
        assertThat(pageObjects(result, Status.STOPPED_AMBIGUOUS))
                .containsExactlyInAnyOrder("ProductCard#addToCart", "LoginPage#errorText");
    }

    @Test(description = "After review: the gate PASSES, every heal a test used was approved; rejected heals stopped")
    public void afterReviewPassesAndRejectedHealsStopped() {
        ReviewedHeals reviewed = ReviewedHeals.load(HealingPolicy.fromSystemProperties().approvalsFile());
        ReviewGate.Result result = ReviewGate.evaluate(heals(Release2HealingTest.class), reviewed);
        ReviewGate.writeReport(result, HealingJournal.FOLDER, "Release 2 after review");

        assertThat(result.rows(Status.NEEDS_REVIEW)).as("heals used without review").isEmpty();
        assertThat(result.rows(Status.REJECTED_BUT_USED)).isEmpty();
        assertThat(result.passed()).isTrue();
        assertThat(pageObjects(result, Status.STOPPED_REJECTED))
                .containsExactlyInAnyOrder("ProductCard#buttonLabel", "LoginPage#errorText");
        assertThat(result.rows(Status.STOPPED_AMBIGUOUS)).as("every tie was decided in review").isEmpty();
    }

    /** The heals recorded while the given test class ran. */
    private static List<HealRecord> heals(Class<?> testClass) {
        List<HealRecord> heals = HealingJournal.run().records().stream()
                .filter(r -> r.test().startsWith(testClass.getSimpleName() + "."))
                .toList();
        if (heals.isEmpty()) {
            throw new SkipException("No heals from " + testClass.getSimpleName() + ": run the whole suite (testng.xml)");
        }
        return heals;
    }

    private static Set<String> pageObjects(ReviewGate.Result result, Status status) {
        return result.rows(status).stream().map(r -> r.first().pageObject()).collect(Collectors.toSet());
    }
}
