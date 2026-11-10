package com.niranjankagri.healing.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The cart ({@code cart.html}): one row per product, the total and the checkout button. */
public class CartPage extends BasePage {

    private static final By TOTAL = By.id("cart-total");
    private static final By CHECKOUT = By.id("checkout");

    public CartPage(WebDriver driver) {
        super(driver);
    }

    /** Waits for the cart page. */
    public CartPage waitUntilShown() {
        waitForPage("cart.html");
        return this;
    }

    /** Name in the given cart row, counting from 1. */
    public String itemName(int row) {
        return driver.findElement(By.cssSelector("#cart-items tr.cart-row:nth-of-type(" + row + ") td.item-name")).getText();
    }

    /** Total as printed, for example {@code €84.50}. */
    public String total() {
        return driver.findElement(TOTAL).getText();
    }

    /** Places the order. */
    public ConfirmationPage checkout() {
        driver.findElement(CHECKOUT).click();
        return new ConfirmationPage(driver).waitUntilShown();
    }
}
