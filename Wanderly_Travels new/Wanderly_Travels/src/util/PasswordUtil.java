package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Turns a plain-text password into a SHA-256 hash before it is ever sent to
 * the database, so real passwords are never stored in the users table.
 * This is a simple, easy-to-explain form of security suitable for a college
 * mini project (a real production system would add a random "salt" per
 * user as well).
 */
public class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainText.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            // SHA-256 and UTF-8 are always available on any standard JVM,
            // so this branch should never actually run.
            throw new RuntimeException("Unable to hash password", e);
        }
    }
}
