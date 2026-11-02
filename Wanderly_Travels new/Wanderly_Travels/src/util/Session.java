package util;

import model.User;

/**
 * Holds the currently logged-in user for the whole running application.
 * Because Java Swing is a single desktop process (unlike a website with many
 * simultaneous visitors), one simple static holder is enough to know "who is
 * using the app right now" - every screen checks Session.getCurrentUser()
 * to decide what to show and which user_id to attach to new bookings.
 */
public class Session {

    private static User currentUser;

    private Session() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
