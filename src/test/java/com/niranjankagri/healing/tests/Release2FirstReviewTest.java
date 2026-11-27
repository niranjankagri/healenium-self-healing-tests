package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.control.HealDecision;
import com.niranjankagri.healing.control.HealingStoppedException;
import com.niranjankagri.healing.control.ReviewedHeals;
import com.niranjankagri.healing.data.Product;
import com.niranjankagri.healing.data.User;
import com.niranjankagri.healing.driver.DriverFactory;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Step 2, "day one": release 2 with controlled healing and NOTHING reviewed yet ({@link ReviewedHeals#none()}),
 * what a team sees the first time the redesign hits the suite.
 *
 * <p>Heals with a clear winner are used, so most journeys pass, but none of them is reviewed: the review gate
 * ({@link HealReviewGateTest}) fails this run and lists each one for a person. Heals without a clear winner are
 * stopped at once; the three journeys below assert exactly where.
 */
public class Release2FirstReviewTest extends ShopJourneys {

    @Override
    protected Release release() {
        return Release.V2;
    }

    @Override
    protected DriverFactory.Kind driverKind() {
        return DriverFactory.Kind.SELF_HEALING;
    }

    @Override
    protected ReviewedHeals reviewedHeals() {
        return ReviewedHeals.none();
    }

    /**
     * The three "Add to basket" buttons look the same: all three score 0.897 against the saved reference. Plain
     * Healenium clicks the first one in its list, which happens to be right. Controlled healing will not guess.
     */
    @Override
    @Test(description = "Day one: adding to the cart stops, the three look-alike buttons tie (0.897 each)")
    public void tc02_addToCartUpdatesBadge() {
        HealingStoppedException stop = catchThrowableOfType(HealingStoppedException.class,
                () -> openShop().signInAs(User.customer()).add(Product.TORQUE_WRENCH));

        assertStopped(stop, HealDecision.STOPPED_AMBIGUOUS, "ProductCard#addToCart");
    }

    /** Same tie as tc02: the checkout journey cannot even fill the cart. */
    @Override
    @Test(description = "Day one: the checkout journey stops at the same tie, before the cart")
    public void tc03_checkoutConfirmsOrder() {
        HealingStoppedException stop = catchThrowableOfType(HealingStoppedException.class,
                () -> openShop().signInAs(User.customer()).add(Product.TORQUE_WRENCH, Product.SOCKET_SET));

        assertStopped(stop, HealDecision.STOPPED_AMBIGUOUS, "ProductCard#addToCart");
    }

    /** Release 1's {@code @DisableHealing} limit does not change with controlled healing: see Release2HealingTest. */
    @Override
    @Test(description = "Day one: the @DisableHealing check with an out-of-date locator still never sees the error")
    public void tc04_lockedAccountIsRefused() {
        Release2HealingTest.lockedAccountCheckNeverSeesTheError(openShop());
    }

    /**
     * Plain Healenium heals the error box to the new wrapper {@code div.card} (wrong). Controlled healing sees
     * that the wrapper ties with other candidates (0.800 each) and stops: the wrong text never reaches the test.
     */
    @Override
    @Test(description = "Day one: the error box lookup stops, no clear winner (0.800 tie), instead of a wrong heal")
    public void tc05_wrongPasswordIsRefused() {
        var login = openShop().signInExpectingError(User.wrongPassword());

        HealingStoppedException stop = catchThrowableOfType(HealingStoppedException.class, login::errorText);

        assertStopped(stop, HealDecision.STOPPED_AMBIGUOUS, "LoginPage#errorText");
    }

    static void assertStopped(HealingStoppedException stop, HealDecision expected, String pageObject) {
        assertThat(stop).as("the heal must be stopped").isNotNull();
        assertThat(stop.decision()).isEqualTo(expected);
        assertThat(stop.getMessage()).contains("in " + pageObject + ":");
    }
}
