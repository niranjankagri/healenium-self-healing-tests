package com.niranjankagri.healing.tests;

import com.niranjankagri.healing.data.Product;
import com.niranjankagri.healing.data.User;
import com.niranjankagri.healing.pages.CartPage;
import com.niranjankagri.healing.pages.ConfirmationPage;
import com.niranjankagri.healing.pages.LoginPage;
import com.niranjankagri.healing.pages.ProductsPage;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The user journeys. Written once and run on both releases by the subclasses, through the very same page
 * objects: that is the point of the demo. Nothing here knows which release it is looking at.
 */
public abstract class ShopJourneys extends BaseTest {

    @Test(description = "A customer signs in and sees the products with their prices and an empty cart")
    public void tc01_signInShowsProducts() {
        ProductsPage products = openShop().signInAs(User.customer());

        assertThat(products.header().cartCount()).isZero();
        assertThat(products.card(Product.TORQUE_WRENCH).priceLabel()).isEqualTo("€49.00");
        assertThat(products.card(Product.SPIRIT_LEVEL).priceLabel()).isEqualTo("€12.25");
    }

    @Test(description = "Adding two products updates the cart badge and the buttons")
    public void tc02_addToCartUpdatesBadge() {
        ProductsPage products = openShop().signInAs(User.customer())
                .add(Product.TORQUE_WRENCH, Product.SOCKET_SET);

        assertThat(products.header().cartCount()).isEqualTo(2);
        assertThat(products.card(Product.TORQUE_WRENCH).buttonLabel()).startsWith("In ");
        assertThat(products.card(Product.SPIRIT_LEVEL).buttonLabel()).startsWith("Add to ");
    }

    @Test(description = "The cart lists the products in order and adds up the total; checkout confirms the order")
    public void tc03_checkoutConfirmsOrder() {
        CartPage cart = openShop().signInAs(User.customer())
                .add(Product.TORQUE_WRENCH, Product.SOCKET_SET)
                .header().openCart();

        assertThat(cart.itemName(1)).isEqualTo(Product.TORQUE_WRENCH.name());
        assertThat(cart.itemName(2)).isEqualTo(Product.SOCKET_SET.name());
        assertThat(cart.total()).isEqualTo("€84.50");

        ConfirmationPage confirmation = cart.checkout();
        assertThat(confirmation.heading()).isEqualTo("Thank you for your order");
        assertThat(confirmation.orderNumber()).isEqualTo("HS-1014");
        assertThat(confirmation.total()).isEqualTo("€84.50");
    }

    @Test(description = "A locked account is refused with a clear message")
    public void tc04_lockedAccountIsRefused() {
        LoginPage login = openShop();
        assertThat(login.isErrorShown()).as("no error before submitting").isFalse();

        login.signInExpectingError(User.locked());

        assertThat(login.isErrorShown()).isTrue();
        assertThat(login.errorText()).isEqualTo("This account is locked.");
    }

    @Test(description = "A wrong password is refused without saying which part was wrong")
    public void tc05_wrongPasswordIsRefused() {
        LoginPage login = openShop().signInExpectingError(User.wrongPassword());

        assertThat(login.errorText()).isEqualTo("Email or password is wrong.");
    }

    @Test(description = "Logging out returns to the sign-in page")
    public void tc06_logOutReturnsToSignIn() {
        LoginPage login = openShop().signInAs(User.customer()).header().logOut();

        // signInAs only succeeds if the sign-in form is back and working
        assertThat(login.signInAs(User.customer()).header().cartCount()).isZero();
    }
}
