package com.niranjankagri.healing.control;

import com.niranjankagri.healing.control.ReviewGate.Status;
import com.niranjankagri.healing.control.ReviewedHeals.Entry;
import com.niranjankagri.healing.control.ReviewedHeals.Verdict;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Step 0: the healing-control rules without a browser or the Healenium backend: the guard's order of rules, the
 * clear-winner boundary, the reviewed-heals file and the review gate.
 */
public class HealingControlTest {

    private static final HealKey EMAIL = new HealKey("LoginPage#submit", "By.id: email", "By.cssSelector: input#user-email");
    private static final HealingPolicy POLICY = new HealingPolicy(0.05, true, Path.of("unused.json"));

    private static ReviewedHeals reviewed(Verdict verdict) {
        return new ReviewedHeals(List.of(new Entry(EMAIL.pageObject(), EMAIL.locator(), EMAIL.healedTo(), verdict, "checked")));
    }

    private static HealRecord used(HealKey key, String test) {
        return new HealRecord(test, key.pageObject(), key.locator(), key.healedTo(), 0.857, 0.777,
                HealDecision.USED, "Clear winner", List.of(), null);
    }

    // ---- HealGuard -------------------------------------------------------------------------------------------

    @Test(description = "A tie (gap 0) is stopped for a person")
    public void tieIsStopped() {
        var verdict = new HealGuard(POLICY, ReviewedHeals.none()).decide(EMAIL, 0.897, 0.897);

        assertThat(verdict.decision()).isEqualTo(HealDecision.STOPPED_AMBIGUOUS);
        assertThat(verdict.reason()).contains("best 0.897, runner-up 0.897 (gap 0.000, needs 0.05)");
    }

    @Test(description = "The margin is a minimum: a gap just below stops, a gap at the margin is used")
    public void marginBoundary() {
        HealGuard guard = new HealGuard(POLICY, ReviewedHeals.none());

        assertThat(guard.decide(EMAIL, 0.85, 0.801).decision()).isEqualTo(HealDecision.STOPPED_AMBIGUOUS);
        assertThat(guard.decide(EMAIL, 0.85, 0.80).decision()).isEqualTo(HealDecision.USED);
    }

    @Test(description = "A single candidate (no runner-up) is a clear winner")
    public void onlyCandidateIsUsed() {
        var verdict = new HealGuard(POLICY, ReviewedHeals.none()).decide(EMAIL, 0.694, null);

        assertThat(verdict.decision()).isEqualTo(HealDecision.USED);
        assertThat(verdict.reason()).isEqualTo("Clear winner (only candidate), not reviewed yet");
    }

    @Test(description = "Rejected in review stops even a clear winner")
    public void rejectedBeatsClearWinner() {
        var verdict = new HealGuard(POLICY, reviewed(Verdict.rejected)).decide(EMAIL, 0.99, 0.10);

        assertThat(verdict.decision()).isEqualTo(HealDecision.STOPPED_REJECTED);
        assertThat(verdict.reason()).isEqualTo("Rejected in review: checked");
    }

    @Test(description = "Approved in review is used even when it was a tie: a person picked it")
    public void approvedBeatsTie() {
        var verdict = new HealGuard(POLICY, reviewed(Verdict.approved)).decide(EMAIL, 0.897, 0.897);

        assertThat(verdict.decision()).isEqualTo(HealDecision.USED);
    }

    @Test(description = "A verdict covers one exact heal: the same locator healed elsewhere is not approved")
    public void verdictIsPerExactHeal() {
        HealKey elsewhere = new HealKey(EMAIL.pageObject(), EMAIL.locator(), "By.cssSelector: input#user-password");

        var verdict = new HealGuard(POLICY, reviewed(Verdict.approved)).decide(elsewhere, 0.9, null);

        assertThat(verdict.reason()).endsWith("not reviewed yet");
    }

    @Test(description = "The policy refuses a margin outside [0, 1)")
    public void policyValidatesMargin() {
        assertThatThrownBy(() -> new HealingPolicy(1.0, true, Path.of("x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- ReviewedHeals file ----------------------------------------------------------------------------------

    @Test(description = "A missing file means nothing reviewed yet")
    public void missingFileIsEmpty() {
        assertThat(ReviewedHeals.load(Path.of("target", "does-not-exist.json")).entries()).isEmpty();
    }

    @Test(description = "Reads verdicts from the file layout used in the repository")
    public void readsFile() throws IOException {
        Path file = write("""
                {"reviewed": [{"pageObject": "LoginPage#submit", "locator": "By.id: email",
                  "healedTo": "By.cssSelector: input#user-email", "verdict": "rejected", "note": "n"}]}""");

        assertThat(ReviewedHeals.load(file).verdictOn(EMAIL)).get().extracting(Entry::verdict).isEqualTo(Verdict.rejected);
    }

    @Test(description = "A misspelt field fails loudly instead of dropping the verdict")
    public void unknownFieldFails() throws IOException {
        Path file = write("""
                {"reviewed": [{"pageObject": "LoginPage#submit", "locator": "By.id: email",
                  "healedTo": "x", "verdict": "approved", "notes": "typo"}]}""");

        assertThatThrownBy(() -> ReviewedHeals.load(file)).isInstanceOf(UncheckedIOException.class);
    }

    @Test(description = "A verdict that is not approved or rejected (e.g. the TODO in a proposal) fails")
    public void todoVerdictFails() throws IOException {
        Path file = write("""
                {"reviewed": [{"pageObject": "p", "locator": "l", "healedTo": "h",
                  "verdict": "TODO: approved or rejected", "note": ""}]}""");

        assertThatThrownBy(() -> ReviewedHeals.load(file)).isInstanceOf(UncheckedIOException.class);
    }

    @Test(description = "The same heal reviewed twice is refused: which verdict would count?")
    public void duplicateFails() {
        Entry entry = new Entry("p", "l", "h", Verdict.approved, "");
        Entry other = new Entry("p", "l", "h", Verdict.rejected, "");

        assertThatThrownBy(() -> new ReviewedHeals(List.of(entry, other)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reviewed twice");
    }

    // ---- ReviewGate ------------------------------------------------------------------------------------------

    @Test(description = "Gate: an unreviewed used heal fails the run; repeats of one heal are one row")
    public void unreviewedFails() {
        var result = ReviewGate.evaluate(List.of(used(EMAIL, "A.t1"), used(EMAIL, "B.t2")), ReviewedHeals.none());

        assertThat(result.passed()).isFalse();
        assertThat(result.rows()).singleElement().satisfies(row -> {
            assertThat(row.status()).isEqualTo(Status.NEEDS_REVIEW);
            assertThat(row.times()).isEqualTo(2);
            assertThat(row.tests()).containsExactly("A.t1", "B.t2");
        });
    }

    @Test(description = "Gate: approved heals pass")
    public void approvedPasses() {
        var result = ReviewGate.evaluate(List.of(used(EMAIL, "A.t1")), reviewed(Verdict.approved));

        assertThat(result.passed()).isTrue();
        assertThat(result.count(Status.APPROVED)).isEqualTo(1);
    }

    @Test(description = "Gate: a rejected heal that was used anyway (observe mode) fails")
    public void rejectedButUsedFails() {
        var result = ReviewGate.evaluate(List.of(used(EMAIL, "A.t1")), reviewed(Verdict.rejected));

        assertThat(result.passed()).isFalse();
        assertThat(result.count(Status.REJECTED_BUT_USED)).isEqualTo(1);
    }

    @Test(description = "Gate: stopped heals are listed but do not fail the run (no test used them)")
    public void stoppedDoesNotFail() {
        HealRecord stopped = new HealRecord("A.t1", "p", "l", "h", 0.8, 0.8, HealDecision.STOPPED_AMBIGUOUS, "tie",
                List.of(), null);

        var result = ReviewGate.evaluate(List.of(stopped), ReviewedHeals.none());

        assertThat(result.passed()).isTrue();
        assertThat(result.count(Status.STOPPED_AMBIGUOUS)).isEqualTo(1);
    }

    @Test(description = "Gate: a locator approved to heal elsewhere before gets a 'page changed again' note")
    public void earlierApprovalIsNoted() {
        HealKey moved = new HealKey(EMAIL.pageObject(), EMAIL.locator(), "By.cssSelector: input#login-email");

        var result = ReviewGate.evaluate(List.of(used(moved, "A.t1")), reviewed(Verdict.approved));

        assertThat(result.rows().get(0).status()).isEqualTo(Status.NEEDS_REVIEW);
        assertThat(result.rows().get(0).note()).contains("Earlier approved target: " + EMAIL.healedTo());
    }

    @Test(description = "Report: pipes in an XPath are escaped so the Markdown table keeps its columns")
    public void markdownEscapesPipes() {
        HealKey xpath = new HealKey("P#m", "By.xpath: //a | //b", "By.cssSelector: a");

        String md = ReviewGate.markdown(ReviewGate.evaluate(List.of(used(xpath, "A.t1")), ReviewedHeals.none()), "t");

        assertThat(md).contains("## Heal review gate: t: FAILED").contains("//a \\| //b");
    }

    private static Path write(String json) throws IOException {
        Path file = Files.createTempFile("reviewed-heals", ".json");
        Files.writeString(file, json);
        file.toFile().deleteOnExit();
        return file;
    }
}
