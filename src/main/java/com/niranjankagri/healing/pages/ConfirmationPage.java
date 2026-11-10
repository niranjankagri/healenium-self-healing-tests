package com.niranjankagri.healing.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** "Thank you for your order" ({@code confirmation.html}). */
public class ConfirmationPage extends BasePage {

    private static final By HEADING = By.id("order-confirmation");
    private static final By ORDER_NUMBER = By.id("order-number");
    // Same id in both releases: shows that unchanged locators work as usual next to healed ones
    private static final By ORDER_TOTAL = By.id("order-total");

    public ConfirmationPage(WebDriver driver) {
        super(driver);
    }

    /** Waits for the confirmation page. */
    public ConfirmationPage waitUntilShown() {
        waitForPage("confirmation.html");
        return this;
    }

    /** The heading. */
    public String heading() {
        return driver.findElement(HEADING).getText();
    }

    /** Order number, for example {@code HS-1014}. */
    public String orderNumber() {
        return driver.findElement(ORDER_NUMBER).getText();
    }

    /** Amount paid as printed. */
    public String total() {
        return driver.findElement(ORDER_TOTAL).getText();
    }
}
