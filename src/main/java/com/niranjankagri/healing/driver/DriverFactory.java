package com.niranjankagri.healing.driver;

import com.epam.healenium.SelfHealingDriver;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

/**
 * Creates the browsers for the tests. The only difference between the two kinds is one wrapper:
 * {@link SelfHealingDriver#create(WebDriver)} around an ordinary ChromeDriver.
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
     * Starts a new Chrome session.
     *
     * @param kind self-healing or plain
     * @return a driver the caller must quit
     */
    public static WebDriver create(Kind kind) {
        // Selenium Manager finds (or downloads) the matching chromedriver: no driver binary in the repo
        WebDriver chrome = new ChromeDriver(options());
        // No implicit wait: page objects wait explicitly, and an implicit wait would slow every healing lookup
        chrome.manage().timeouts().implicitlyWait(Duration.ZERO);
        // Healenium reads its settings from healenium.properties on the classpath (src/test/resources)
        return kind == Kind.SELF_HEALING ? SelfHealingDriver.create(chrome) : chrome;
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
