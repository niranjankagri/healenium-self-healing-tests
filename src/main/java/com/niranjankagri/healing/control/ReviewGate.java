package com.niranjankagri.healing.control;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * The heal review gate: a run passes only if every heal a test USED was approved by a person. A heal nobody has
 * reviewed fails the gate, with the evidence a reviewer needs and a ready-to-fill entry for
 * {@code approved-heals.json}. Stopped heals do not fail the gate (no test used them), but they are listed: each
 * one is a lookup a person has to fix.
 */
public final class ReviewGate {

    /** Where one group of identical heals stands after the review check. */
    public enum Status {
        /** Used, and a reviewer approved exactly this heal. */
        APPROVED,
        /** Used, but nobody has reviewed it: fails the gate. */
        NEEDS_REVIEW,
        /** Used although a reviewer rejected it (only possible in observe mode): fails the gate. */
        REJECTED_BUT_USED,
        /** Stopped: no clear winner. */
        STOPPED_AMBIGUOUS,
        /** Stopped: rejected in review. */
        STOPPED_REJECTED
    }

    /**
     * Identical heals (same key, same decision) of one run, grouped.
     *
     * @param first a representative record (scores, candidates, screenshot)
     * @param times how many lookups it covers
     * @param tests which tests hit it
     * @param note  extra context for the reviewer, e.g. an earlier approval of a different target
     */
    public record Row(Status status, HealRecord first, int times, List<String> tests, String note) {
    }

    /** The gate's answer for one run. */
    public record Result(List<Row> rows) {

        public boolean passed() {
            return count(Status.NEEDS_REVIEW) == 0 && count(Status.REJECTED_BUT_USED) == 0;
        }

        public long count(Status status) {
            return rows.stream().filter(r -> r.status() == status).count();
        }

        public List<Row> rows(Status status) {
            return rows.stream().filter(r -> r.status() == status).toList();
        }
    }

    private ReviewGate() {
    }

    /** Checks the heals of a run against the reviewed baseline. */
    public static Result evaluate(List<HealRecord> records, ReviewedHeals reviewed) {
        Map<String, List<HealRecord>> groups = new LinkedHashMap<>();
        for (HealRecord record : records) {
            groups.computeIfAbsent(record.key() + "|" + record.decision(), k -> new ArrayList<>()).add(record);
        }
        List<Row> rows = new ArrayList<>();
        for (List<HealRecord> group : groups.values()) {
            HealRecord first = group.get(0);
            List<String> tests = List.copyOf(new TreeSet<>(group.stream().map(HealRecord::test).toList()));
            String note = reviewed.earlierApproval(first.key())
                    .map(e -> "Earlier approved target: " + e.healedTo() + ". The page changed again")
                    .orElse("");
            rows.add(new Row(status(first, reviewed), first, group.size(), tests, note));
        }
        return new Result(rows);
    }

    private static Status status(HealRecord record, ReviewedHeals reviewed) {
        return switch (record.decision()) {
            case STOPPED_AMBIGUOUS -> Status.STOPPED_AMBIGUOUS;
            case STOPPED_REJECTED -> Status.STOPPED_REJECTED;
            case USED -> reviewed.verdictOn(record.key())
                    .map(e -> e.verdict() == ReviewedHeals.Verdict.approved ? Status.APPROVED : Status.REJECTED_BUT_USED)
                    .orElse(Status.NEEDS_REVIEW);
        };
    }

    /**
     * Writes the reviewer's report ({@code review.md}, also used as the CI job summary) and, when something needs a
     * verdict, {@code proposed-heals.json}: entries in the format of {@code approved-heals.json} with the verdict
     * left for a person to fill in.
     *
     * @param folder usually {@link HealingJournal#FOLDER}
     * @param title  what this run was, e.g. "Release 2 after review"
     */
    public static void writeReport(Result result, Path folder, String title) {
        try {
            Files.createDirectories(folder);
            Files.writeString(folder.resolve(fileName(title, ".md")), markdown(result, title));
            List<Map<String, Object>> proposals = new ArrayList<>();
            for (Row row : result.rows()) {
                if (row.status() == Status.NEEDS_REVIEW || row.status() == Status.STOPPED_AMBIGUOUS) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("pageObject", row.first().pageObject());
                    entry.put("locator", row.first().locator());
                    entry.put("healedTo", row.first().healedTo());
                    entry.put("verdict", "TODO: approved or rejected");
                    entry.put("note", "TODO: what you checked. Evidence: " + row.first().screenshot());
                    proposals.add(entry);
                }
            }
            if (!proposals.isEmpty()) {
                ReviewedHeals.JSON.writeValue(folder.resolve(fileName(title, "-proposed.json")).toFile(),
                        Map.of("reviewed", proposals));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the review report", e);
        }
    }

    static String fileName(String title, String suffix) {
        return "review-" + title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "") + suffix;
    }

    /** Markdown for people: verdict, counts, then one line per heal. */
    static String markdown(Result result, String title) {
        StringBuilder md = new StringBuilder();
        md.append("## Heal review gate: ").append(title).append(": ").append(result.passed() ? "PASSED" : "FAILED")
                .append("\n\n");
        md.append("| Approved | Needs review | Rejected but used | Stopped: no clear winner | Stopped: rejected |\n");
        md.append("|---:|---:|---:|---:|---:|\n");
        md.append("| %d | %d | %d | %d | %d |%n%n".formatted(result.count(Status.APPROVED),
                result.count(Status.NEEDS_REVIEW), result.count(Status.REJECTED_BUT_USED),
                result.count(Status.STOPPED_AMBIGUOUS), result.count(Status.STOPPED_REJECTED)));
        if (result.rows().isEmpty()) {
            return md.append("No locator was healed in this run.\n").toString();
        }
        md.append("| Status | Page object | Locator | Healed to | Score | Runner-up | Times | Why | Evidence |\n");
        md.append("|---|---|---|---|---:|---:|---:|---|---|\n");
        for (Row row : result.rows()) {
            HealRecord r = row.first();
            md.append("| ").append(row.status())
                    .append(" | `").append(r.pageObject())
                    .append("` | `").append(cell(r.locator()))
                    .append("` | `").append(cell(r.healedTo()))
                    .append("` | ").append(String.format(Locale.ROOT, "%.3f", r.score()))
                    .append(" | ").append(r.runnerUp() == null ? "-" : String.format(Locale.ROOT, "%.3f", r.runnerUp()))
                    .append(" | ").append(row.times())
                    .append(" | ").append(cell(r.reason() + (row.note().isEmpty() ? "" : ". " + row.note())))
                    .append(" | ").append(r.screenshot() == null ? "-" : r.screenshot())
                    .append(" |\n");
        }
        return md.toString();
    }

    /** Pipes inside XPath would break the table. */
    private static String cell(String text) {
        return text.replace("|", "\\|");
    }
}
