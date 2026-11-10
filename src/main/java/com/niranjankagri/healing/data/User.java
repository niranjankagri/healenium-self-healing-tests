package com.niranjankagri.healing.data;

/**
 * A shop account used by the tests.
 *
 * @param email    sign-in email
 * @param password sign-in password
 */
public record User(String email, String password) {

    /** Can sign in and order. */
    public static User customer() {
        return new User("alex@example.com", "Healing#2026");
    }

    /** Exists, but the shop refuses the sign-in. */
    public static User locked() {
        return new User("locked@example.com", "Healing#2026");
    }

    /** A known email with the wrong password. */
    public static User wrongPassword() {
        return new User("alex@example.com", "not-the-password");
    }
}
