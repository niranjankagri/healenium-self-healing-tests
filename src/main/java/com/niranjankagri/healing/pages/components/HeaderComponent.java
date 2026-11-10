package com.niranjankagri.healing.pages.components;

import com.niranjankagri.healing.pages.CartPage;
import com.niranjankagri.healing.pages.LoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** The dark bar at the top of the product page: cart link with its counter, and log out. */
public class HeaderComponent {

    private static final By CART_LINK = By.cssSelector("a.cart-link");
    private static final By CART_COUNT = By.cssSelector(".cart-count");
    private static final By LOG_OUT = By.id("logout");

    private final WebDriver driver;

    public HeaderComponent(WebDriver driver) {
        this.driver = driver;
    }

    /** Number shown in the cart badge. */
    public int cartCount() {
        return Integer.parseInt(driver.findElement(CART_COUNT).getText().trim());
    }

    /** Opens the cart. */
    public CartPage openCart() {
        driver.findElement(CART_LINK).click();
        return new CartPage(driver).waitUntilShown();
    }

    /** Logs out and returns to the sign-in page. */
    public LoginPage logOut() {
        driver.findElement(LOG_OUT).click();
        return new LoginPage(driver);
    }
}
