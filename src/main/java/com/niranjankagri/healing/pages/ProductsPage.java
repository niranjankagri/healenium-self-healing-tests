package com.niranjankagri.healing.pages;

import com.niranjankagri.healing.data.Product;
import com.niranjankagri.healing.pages.components.HeaderComponent;
import com.niranjankagri.healing.pages.components.ProductCard;
import org.openqa.selenium.WebDriver;

/** The product list ({@code products.html}). */
public class ProductsPage extends BasePage {

    private final HeaderComponent header;

    public ProductsPage(WebDriver driver) {
        super(driver);
        this.header = new HeaderComponent(driver);
    }

    /** Waits for the page after sign-in. */
    public ProductsPage waitUntilShown() {
        waitForPage("products.html");
        return this;
    }

    /** The header bar (cart link, counter, log out). */
    public HeaderComponent header() {
        return header;
    }

    /** The card of one product. */
    public ProductCard card(Product product) {
        return new ProductCard(driver, product);
    }

    /** Adds products one after the other and stays on this page. */
    public ProductsPage add(Product... products) {
        for (Product product : products) {
            card(product).addToCart();
        }
        return this;
    }
}
