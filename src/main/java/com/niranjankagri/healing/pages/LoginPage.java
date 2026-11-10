package com.niranjankagri.healing.pages;

import com.epam.healenium.annotation.DisableHealing;
import com.niranjankagri.healing.data.User;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

/** The sign-in page ({@code index.html}). Locators are the release 1 ones. */
public class LoginPage extends BasePage {

    private static final By EMAIL = By.id("email");
    private static final By PASSWORD = By.id("password");
    private static final By LOG_IN = By.id("login-button");
    private static final By ERROR = By.cssSelector(".error-message");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /** Opens the shop's start page and waits for it. */
    public LoginPage open(String baseUrl) {
        driver.get(baseUrl + "index.html");
        waitForPage("index.html");
        return this;
    }

    /** Signs in with an account the shop accepts and lands on the product list. */
    public ProductsPage signInAs(User user) {
        submit(user);
        return new ProductsPage(driver).waitUntilShown();
    }

    /** Submits an account the shop refuses; stays on this page with an error. */
    public LoginPage signInExpectingError(User user) {
        submit(user);
        return this;
    }

    /** Text of the error under the form. */
    public String errorText() {
        return driver.findElement(ERROR).getText();
    }

    /**
     * True when an error is visible. A check that something is NOT there must not be healed: Healenium would
     * otherwise look for the "most similar" element and could report a different element as the error.
     */
    @DisableHealing
    public boolean isErrorShown() {
        List<WebElement> errors = driver.findElements(ERROR);
        return !errors.isEmpty() && errors.get(0).isDisplayed();
    }

    private void submit(User user) {
        WebElement email = driver.findElement(EMAIL);
        email.clear();
        email.sendKeys(user.email());
        WebElement password = driver.findElement(PASSWORD);
        password.clear();
        password.sendKeys(user.password());
        driver.findElement(LOG_IN).click();
    }
}
