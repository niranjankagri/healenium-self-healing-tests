package com.niranjankagri.healing.pages.components;

import com.niranjankagri.healing.data.Product;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * One product tile on the product page, found by the product's name.
 *
 * <p>Each card is located with its own XPath (the card that contains this product's name), so Healenium saves a
 * separate reference element per product and heals each one on its own.
 */
public class ProductCard {

    private final WebDriver driver;
    private final By price;
    private final By addButton;

    public ProductCard(WebDriver driver, Product product) {
        this.driver = driver;
        // Release 1 markup: <div class="product-card"><h3 class="product-name">NAME</h3> ... </div>
        String card = "//div[@class='product-card'][h3[@class='product-name' and normalize-space()='" + product.name() + "']]";
        this.price = By.xpath(card + "/p[@class='price']");
        this.addButton = By.xpath(card + "/button[contains(@class,'add-to-cart')]");
    }

    /** Price as printed on the card. */
    public String priceLabel() {
        return driver.findElement(price).getText();
    }

    /** Clicks "Add to cart". */
    public void addToCart() {
        driver.findElement(addButton).click();
    }

    /** Label of the add button: "Add to cart" before the click, "In cart" after it. */
    public String buttonLabel() {
        return driver.findElement(addButton).getText();
    }
}
