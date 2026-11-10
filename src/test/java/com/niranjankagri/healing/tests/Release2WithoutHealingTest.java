package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.app.Release;
import com.niranjankagri.healing.app.ShopSession;
import com.niranjankagri.healing.data.Product;
import com.niranjankagri.healing.data.User;
import com.niranjankagri.healing.driver.DriverFactory;
import com.niranjankagri.healing.pages.LoginPage;
import com.niranjankagri.healing.pages.ProductsPage;
import org.openqa.selenium.NoSuchElementException;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The control group: release 2 with a plain ChromeDriver. The same page objects fail at the first out-of-date
 * locator, which is what the healing tests would do without Healenium. If one of these ever passes, release 2 no
 * longer breaks that locator and the healing tests prove nothing for it.
 */
public class Release2WithoutHealingTest extends BaseTest {

    @Override
    protected Release release() {
        return Release.V2;
    }

    @Override
    protected DriverFactory.Kind driverKind() {
        return DriverFactory.Kind.PLAIN;
    }

    @Test(description = "Without healing, sign-in fails at the renamed email field")
    public void tc07_signInBreaksWithoutHealing() {
        LoginPage login = openShop();

        assertThatThrownBy(() -> login.signInAs(User.customer()))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("#email");
    }

    @Test(description = "Without healing, the error check fails at the renamed error box")
    public void tc08_errorTextBreaksWithoutHealing() {
        LoginPage login = openShop();

        assertThatThrownBy(login::errorText)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(".error-message");
    }

    @Test(description = "Without healing, the product cards are not found once signed in")
    public void tc09_productCardBreaksWithoutHealing() {
        // The sign-in form itself is broken without healing (tc07), so enter through the shop's session instead
        openShop();
        ShopSession.signInWithoutForm(driver, User.customer());
        ProductsPage products = new ProductsPage(driver).waitUntilShown();

        assertThatThrownBy(() -> products.card(Product.TORQUE_WRENCH).priceLabel())
                .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> products.header().cartCount())
                .isInstanceOf(NoSuchElementException.class);
    }
}
