package com.niranjankagri.healing.control;

/**
 * Identifies one heal for review: this out-of-date locator, in this page-object method, healed to this new
 * locator. The method is part of the key because Healenium keeps one reference element per locator AND calling
 * method: the same XPath can heal correctly in {@code addToCart()} and wrongly in {@code buttonLabel()}.
 *
 * @param pageObject e.g. {@code ProductCard#buttonLabel}
 * @param locator    e.g. {@code By.id: email}
 * @param healedTo   e.g. {@code By.cssSelector: input#user-email}
 */
public record HealKey(String pageObject, String locator, String healedTo) {
}
