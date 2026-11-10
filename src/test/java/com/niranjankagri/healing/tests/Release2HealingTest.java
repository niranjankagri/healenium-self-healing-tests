package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.data.Product;
import com.niranjankagri.healing.data.User;
import com.niranjankagri.healing.driver.DriverFactory;
import com.niranjankagri.healing.pages.LoginPage;
import com.niranjankagri.healing.pages.ProductsPage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 2: the same journeys, same page objects, on release 2, where every locator in the page objects is out of
 * date. Each failed lookup is sent to Healenium, which picks the element most like the saved reference, and the
 * test carries on. These tests pass only because of the healing: {@link Release2WithoutHealingTest} proves it.
 *
 * <p><b>Known limits.</b> Three journeys are overridden below because Healenium heals them to the WRONG element.
 * They assert what Healenium really returns (the same on every run: scoring is deterministic), so the suite stays
 * honest: if a Healenium upgrade changes the outcome, these tests fail and the README's "Limits" table needs an
 * update. In each case the journey's own assertion would have caught the wrong element; nothing passes silently.
 */
public class Release2HealingTest extends ShopJourneys {

    @Override
    protected Release release() {
        return Release.V2;
    }

    @Override
    protected DriverFactory.Kind driverKind() {
        return DriverFactory.Kind.SELF_HEALING;
    }

    /**
     * LIMIT 1, look-alike siblings. The badge heals correctly, but the label lookup for the torque wrench's button
     * heals to the spirit level's button: the three buttons score the same, and the healed locator is positional
     * ({@code article:nth-child(3) > button}). The click in {@code addToCart()} healed to the right button, because
     * Healenium keeps one reference per locator AND calling method.
     */
    @Override
    @Test(description = "Release 2 LIMIT: the button label lookup heals to a look-alike button on another card")
    public void tc02_addToCartUpdatesBadge() {
        ProductsPage products = openShop().signInAs(User.customer())
                .add(Product.TORQUE_WRENCH, Product.SOCKET_SET);

        assertThat(products.header().cartCount()).isEqualTo(2);
        // Expected "In basket" (the torque wrench was added); healed to the spirit level's untouched button instead
        assertThat(products.card(Product.TORQUE_WRENCH).buttonLabel()).isEqualTo("Add to basket");
    }

    /**
     * LIMIT 2, checks with healing switched off. {@code isErrorShown()} carries {@code @DisableHealing} so that an
     * absence check can never be "healed" into some other element. The price: with an out-of-date locator it can
     * not see the error either, before or after the refused sign-in.
     */
    @Override
    @Test(description = "Release 2 LIMIT: a @DisableHealing check with an out-of-date locator never sees the error")
    public void tc04_lockedAccountIsRefused() {
        LoginPage login = openShop();
        assertThat(login.isErrorShown()).as("no error before submitting").isFalse();

        login.signInExpectingError(User.locked());

        // The page does show "This account is locked." (see the screenshots in docs/), but the check cannot find it
        assertThat(login.isErrorShown()).isFalse();
    }

    /**
     * LIMIT 3, an element that moved into a new wrapper. Release 2 wraps the form in {@code div.card} and renames the
     * error box to {@code div.form-alert}. Healenium's score leans on the element's path in the page, and the new
     * wrapper sits where the old error box's path pointed, so {@code .error-message} heals to the wrapper (score 0.8)
     * and the "error text" is the whole sign-in card.
     */
    @Override
    @Test(description = "Release 2 LIMIT: the error box heals to the new wrapper around the form")
    public void tc05_wrongPasswordIsRefused() {
        LoginPage login = openShop().signInExpectingError(User.wrongPassword());

        assertThat(login.errorText())
                .startsWith("Welcome back")
                .contains("Email or password is wrong.");
    }
}
