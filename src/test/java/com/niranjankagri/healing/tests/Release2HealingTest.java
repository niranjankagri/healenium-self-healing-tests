package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.control.HealDecision;
import com.niranjankagri.healing.control.HealingStoppedException;
import com.niranjankagri.healing.data.Product;
import com.niranjankagri.healing.data.User;
import com.niranjankagri.healing.driver.DriverFactory;
import com.niranjankagri.healing.pages.LoginPage;
import com.niranjankagri.healing.pages.ProductsPage;
import org.testng.annotations.Test;

import static com.niranjankagri.healing.tests.Release2FirstReviewTest.assertStopped;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Step 3, "after review": the same journeys on release 2, where every locator in the page objects is out of date,
 * now with the reviewed heals from {@code src/test/resources/healing/approved-heals.json}. Approved heals are
 * used (also the look-alike buttons a person checked); rejected heals are stopped. The review gate
 * ({@link HealReviewGateTest}) passes this run: every heal a test used was approved.
 *
 * <p>These tests pass only because of the healing: {@link Release2WithoutHealingTest} proves it.
 *
 * <p><b>Known limits.</b> Three journeys are overridden. Two hit heals that plain Healenium gets WRONG (the same on
 * every run: scoring is deterministic); here they are rejected in review, so the lookup stops instead of returning
 * the wrong element. The third is a {@code @DisableHealing} check, which no healing can help. If a Healenium
 * upgrade changes an outcome, these tests fail and the README needs an update.
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
     * LIMIT 1, look-alike siblings. Adding to the cart uses the approved heals. But the label lookup for the torque
     * wrench's button heals to the spirit level's button: after two clicks only that button still reads "Add to
     * basket" like the saved reference, so it wins clearly (0.897 against 0.781) and no clear-winner rule can catch
     * it. A reviewer rejected it, so the lookup stops instead of reading the wrong card.
     */
    @Override
    @Test(description = "Release 2 LIMIT: the button label heal (wrong card, clear winner) is rejected in review and stops")
    public void tc02_addToCartUpdatesBadge() {
        ProductsPage products = openShop().signInAs(User.customer())
                .add(Product.TORQUE_WRENCH, Product.SOCKET_SET);
        assertThat(products.header().cartCount()).isEqualTo(2);

        HealingStoppedException stop = catchThrowableOfType(HealingStoppedException.class,
                () -> products.card(Product.TORQUE_WRENCH).buttonLabel());

        assertStopped(stop, HealDecision.STOPPED_REJECTED, "ProductCard#buttonLabel");
    }

    /** LIMIT 2: see {@link #lockedAccountCheckNeverSeesTheError(LoginPage)}. */
    @Override
    @Test(description = "Release 2 LIMIT: a @DisableHealing check with an out-of-date locator never sees the error")
    public void tc04_lockedAccountIsRefused() {
        lockedAccountCheckNeverSeesTheError(openShop());
    }

    /**
     * {@code isErrorShown()} carries {@code @DisableHealing} so that an absence check can never be "healed" into
     * some other element. The price: with an out-of-date locator it cannot see the error either, before or after
     * the refused sign-in. Healing control does not change this: the lookup never reaches the healing service.
     */
    static void lockedAccountCheckNeverSeesTheError(LoginPage login) {
        assertThat(login.isErrorShown()).as("no error before submitting").isFalse();

        login.signInExpectingError(User.locked());

        // The page does show "This account is locked." (see the screenshots in docs/), but the check cannot find it
        assertThat(login.isErrorShown()).isFalse();
    }

    /**
     * LIMIT 3, an element that moved into a new wrapper. Release 2 wraps the form in {@code div.card} and renames
     * the error box; plain Healenium heals {@code .error-message} to the wrapper, so the "error text" would be the
     * whole sign-in card. Rejected in review: the lookup stops.
     */
    @Override
    @Test(description = "Release 2 LIMIT: the error box heal (the new wrapper) is rejected in review and stops")
    public void tc05_wrongPasswordIsRefused() {
        LoginPage login = openShop().signInExpectingError(User.wrongPassword());

        HealingStoppedException stop = catchThrowableOfType(HealingStoppedException.class, login::errorText);

        assertStopped(stop, HealDecision.STOPPED_REJECTED, "LoginPage#errorText");
    }
}
