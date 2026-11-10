package com.niranjankagri.healing.app;

import com.niranjankagri.healing.data.User;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * A back door into the shop's session, for tests that must reach a page without using the sign-in form.
 * The demo shop keeps its session in {@code sessionStorage}, so a signed-in session is two keys.
 *
 * <p>Used by the control tests: with a plain driver the release 2 sign-in form cannot be used (its locators are
 * out of date), but the product page must still be reached to show its locators are out of date too.
 */
public final class ShopSession {

    private ShopSession() {
    }

    /**
     * Signs the user in and opens the product page, with an empty cart. A page of the shop must already be open,
     * because sessionStorage belongs to the page's origin.
     */
    public static void signInWithoutForm(WebDriver driver, User user) {
        ((JavascriptExecutor) driver).executeScript(
                "sessionStorage.setItem('user', arguments[0]);"
                        + "sessionStorage.setItem('cart', '[]');"
                        + "location.href = 'products.html';",
                user.email());
    }
}
