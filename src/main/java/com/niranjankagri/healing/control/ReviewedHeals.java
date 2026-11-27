package com.niranjankagri.healing.control;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The approved locator baseline: every heal a person has looked at, with their verdict. Lives in the repository
 * ({@code src/test/resources/healing/approved-heals.json}), so a verdict is a reviewed commit like any other change.
 *
 * <p>Jackson comes with healenium-web (2.15), so no extra dependency is needed.
 */
public final class ReviewedHeals {

    /** A reviewer's decision on one heal. */
    public enum Verdict {
        /** The healed element is the right one: the heal may be used. */
        approved,
        /** The healed element is wrong: the guard stops this heal from now on. */
        rejected
    }

    /**
     * One reviewed heal as stored in the file.
     *
     * @param pageObject page object and method, e.g. {@code LoginPage#submit}
     * @param locator    the out-of-date locator
     * @param healedTo   the locator Healenium built
     * @param verdict    approved or rejected
     * @param note       why: what the reviewer checked, or what is wrong with the healed element
     */
    public record Entry(String pageObject, String locator, String healedTo, Verdict verdict, String note) {

        public Entry {
            if (pageObject == null || locator == null || healedTo == null || verdict == null) {
                throw new IllegalArgumentException("pageObject, locator, healedTo and verdict are required: " + this);
            }
        }

        HealKey key() {
            return new HealKey(pageObject, locator, healedTo);
        }
    }

    /** File layout: {@code {"reviewed": [ ... ]}}. */
    record FileContent(List<Entry> reviewed) {
    }

    static final ObjectMapper JSON = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            // A typo in a field name must fail loudly, not silently drop a verdict
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final List<Entry> entries;
    private final Map<HealKey, Entry> byKey = new HashMap<>();

    public ReviewedHeals(List<Entry> entries) {
        this.entries = List.copyOf(entries);
        for (Entry entry : this.entries) {
            if (byKey.put(entry.key(), entry) != null) {
                throw new IllegalArgumentException("Heal reviewed twice: " + entry.key());
            }
        }
    }

    /** No heal reviewed yet: every heal counts as new. */
    public static ReviewedHeals none() {
        return new ReviewedHeals(List.of());
    }

    /** Reads the file; a missing file means nothing has been reviewed yet. */
    public static ReviewedHeals load(Path file) {
        if (!Files.exists(file)) {
            return none();
        }
        try {
            FileContent content = JSON.readValue(file.toFile(), FileContent.class);
            return new ReviewedHeals(content.reviewed() == null ? List.of() : content.reviewed());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the reviewed heals in " + file, e);
        }
    }

    /** The verdict on exactly this heal, if someone gave one. */
    public Optional<Entry> verdictOn(HealKey key) {
        return Optional.ofNullable(byKey.get(key));
    }

    /**
     * Was this out-of-date locator approved to heal to something ELSE before? Then a different heal now means the
     * page changed again, and the new target needs a fresh look.
     */
    public Optional<Entry> earlierApproval(HealKey key) {
        return entries.stream()
                .filter(e -> e.verdict() == Verdict.approved)
                .filter(e -> e.pageObject().equals(key.pageObject()) && e.locator().equals(key.locator()))
                .filter(e -> !e.healedTo().equals(key.healedTo()))
                .findFirst();
    }

    public List<Entry> entries() {
        return entries;
    }
}
