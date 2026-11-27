package com.niranjankagri.healing.control;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Every heal of the run, in order, kept in memory and written to {@code target/healing/heals.json}. One journal
 * per JVM: Healenium creates a new engine for every browser, but the review covers the whole run.
 */
public final class HealingJournal {

    /** Where the journal, the screenshots and the review report go. */
    public static final Path FOLDER = Path.of("target", "healing");

    private static final HealingJournal RUN = new HealingJournal();

    private final List<HealRecord> records = new ArrayList<>();
    private final ThreadLocal<String> currentTest = ThreadLocal.withInitial(() -> "(no test)");
    private final AtomicInteger screenshots = new AtomicInteger();

    private HealingJournal() {
    }

    /** The journal of this run. */
    public static HealingJournal run() {
        return RUN;
    }

    /** Called by the test base class, so each heal says which test hit it. */
    public void startTest(String name) {
        currentTest.set(name);
    }

    String currentTest() {
        return currentTest.get();
    }

    synchronized void add(HealRecord record) {
        records.add(record);
    }

    /** All heals so far, in the order they happened. */
    public synchronized List<HealRecord> records() {
        return List.copyOf(records);
    }

    /**
     * Saves a PNG next to the journal and returns its name relative to {@link #FOLDER}, or {@code null} if it could
     * not be written: missing evidence must not turn into a test failure.
     */
    String saveScreenshot(byte[] png) {
        if (png == null || png.length == 0) {
            return null;
        }
        String name = "screenshots/heal-%03d.png".formatted(screenshots.incrementAndGet());
        try {
            Path file = FOLDER.resolve(name);
            Files.createDirectories(file.getParent());
            Files.write(file, png);
            return name;
        } catch (IOException e) {
            System.err.println("No heal screenshot: " + e);
            return null;
        }
    }

    /** Writes {@code heals.json}: the raw evidence behind the review report. */
    public void write() {
        try {
            Files.createDirectories(FOLDER);
            ReviewedHeals.JSON.writeValue(FOLDER.resolve("heals.json").toFile(), records());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write the heal journal", e);
        }
    }
}
