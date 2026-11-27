package com.niranjankagri.healing.driver;

import com.epam.healenium.SelfHealingDriver;
import com.niranjankagri.healing.control.GuardedHealingService;
import com.niranjankagri.healing.control.HealingJournal;
import com.niranjankagri.healing.control.HealingPolicy;
import com.niranjankagri.healing.control.ReviewedHeals;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Creates the browsers for the tests. The only difference between the two kinds is one wrapper:
 * {@link SelfHealingDriver#create(WebDriver)} around an ordinary ChromeDriver, with Healenium's healing service
 * replaced by the {@link GuardedHealingService} (clear-winner rule, reviewed heals, heal journal).
 *
 * <p>Settings come from system properties so CI and a laptop run the same code:
 * {@code -Dheadless=false} shows the browser window.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    /** Which driver a test asks for. */
    public enum Kind {
        /** ChromeDriver wrapped by Healenium: broken locators are healed. */
        SELF_HEALING,
        /** Plain ChromeDriver: a broken locator throws NoSuchElementException, as in any Selenium suite. */
        PLAIN
    }

    /**
     * Starts a new Chrome session; a self-healing one checks heals against the reviewed heals in the repository
     * ({@link HealingPolicy#approvalsFile()}).
     *
     * @param kind self-healing or plain
     * @return a driver the caller must quit
     */
    public static WebDriver create(Kind kind) {
        return create(kind, ReviewedHeals.load(HealingPolicy.fromSystemProperties().approvalsFile()));
    }

    /**
     * Starts a new Chrome session with an explicit review baseline, e.g. {@link ReviewedHeals#none()} for "day one,
     * nothing reviewed yet".
     *
     * @param kind     self-healing or plain
     * @param reviewed the heals a person has approved or rejected (ignored for a plain driver)
     * @return a driver the caller must quit
     */
    public static WebDriver create(Kind kind, ReviewedHeals reviewed) {
        // Selenium Manager finds (or downloads) the matching chromedriver: no driver binary in the repo
        WebDriver chrome = new ChromeDriver(options());
        // No implicit wait: page objects wait explicitly, and an implicit wait would slow every healing lookup
        chrome.manage().timeouts().implicitlyWait(Duration.ZERO);
        // Healenium reads its settings from healenium.properties on the classpath (src/test/resources)
        return kind == Kind.SELF_HEALING ? guarded(SelfHealingDriver.create(chrome), reviewed) : chrome;
    }

    /** Swaps in the guarded healing service; Healenium looks the service up on every lookup, so this is enough. */
    private static SelfHealingDriver guarded(SelfHealingDriver driver, ReviewedHeals reviewed) {
        driver.getCurrentEngine().setHealingService(new GuardedHealingService(driver.getCurrentEngine(),
                HealingPolicy.fromSystemProperties(), reviewed, HealingJournal.run()));
        return driver;
    }

    private static ChromeOptions options() {
        ChromeOptions options = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            options.addArguments("--headless=new");
        }
        // Same window size everywhere: element positions are part of what Healenium compares
        options.addArguments("--window-size=1280,900", "--disable-search-engine-choice-screen");
        return options;
    }
}
