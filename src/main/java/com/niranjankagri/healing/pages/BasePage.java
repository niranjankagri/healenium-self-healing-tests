package com.niranjankagri.healing.pages;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Shared plumbing of every page object: the driver and waiting for a page.
 *
 * <p>Page objects call {@code driver.findElement(locator)} themselves, on purpose, instead of a shared
 * {@code find()} helper here. With a SelfHealingDriver that call is where Healenium steps in: on a good run it saves
 * the element it found, on a later run where the locator no longer matches it returns the most similar element.
 * Healenium files each saved element (and each healing in its report) under the class and method that called
 * {@code findElement}. A helper here would file everything under {@code BasePage.find} and the report would no longer
 * say which page healed.
 */
public abstract class BasePage {

    private static final Duration PAGE_LOAD = Duration.ofSeconds(10);

    protected final WebDriver driver;

    protected BasePage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Waits until the browser shows the page with this file name and has run its scripts.
     * The shop builds lists with inline scripts at the end of the body, so "complete" means the list is there.
     */
    protected void waitForPage(String fileName) {
        new WebDriverWait(driver, PAGE_LOAD).until(d -> d.getCurrentUrl().endsWith("/" + fileName)
                && "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }
}
