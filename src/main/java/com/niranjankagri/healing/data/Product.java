package com.niranjankagri.healing.data;

import java.math.BigDecimal;

/**
 * A product of the demo shop, as the tests expect to see it.
 *
 * @param name  name shown on the product card and in the cart
 * @param price price in euro
 */
public record Product(String name, BigDecimal price) {

    public static final Product TORQUE_WRENCH = new Product("Torque wrench", new BigDecimal("49.00"));
    public static final Product SOCKET_SET = new Product("Socket set", new BigDecimal("35.50"));
    public static final Product SPIRIT_LEVEL = new Product("Spirit level", new BigDecimal("12.25"));

    /** The price as the shop prints it, for example {@code €49.00}. */
    public String priceLabel() {
        return "€" + price.setScale(2);
    }
}
