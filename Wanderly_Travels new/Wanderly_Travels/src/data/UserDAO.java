package data;

import model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;

/**
 * All database access related to the "users" table lives here.
 * Every query uses PreparedStatement (never string concatenation), which
 * both protects against SQL injection and lets MySQL enforce the UNIQUE
 * constraint safely.
 */
public class UserDAO {

    /** Application-level check used before attempting to insert. */
    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Attempts to register a new user.
     * Returns the created User with its generated user_id on success,
     * or null if the username is already taken.
     *
     * The UNIQUE constraint on the username column (see DBInitializer /
     * schema.sql) is the final, database-level guarantee that two accounts
     * can never share a username - even the tiny race condition between
     * usernameExists() and this insert is caught here.
     */
    public User register(String username, String passwordHash, String fullName, String email, String phone)
            throws SQLException {

        if (usernameExists(username)) {
            return null;
        }

        String sql = "INSERT INTO users (username, password_hash, full_name, email, phone) VALUES (?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.setString(3, fullName);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    User u = new User(keys.getInt(1), username, fullName, email, phone);
                    return u;
                }
            }
            return null;
        } catch (SQLIntegrityConstraintViolationException dup) {
            // Safety net: even if two users somehow raced past usernameExists(),
            // the database UNIQUE constraint rejects the second insert here.
            return null;
        }
    }

    /**
     * Verifies login credentials.
     * Returns the matching User if username + password hash are correct,
     * or null if the username does not exist or the password is wrong.
     */
    public User login(String username, String passwordHash) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, full_name, email, phone FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null; // no such username
                }
                String storedHash = rs.getString("password_hash");
                if (!storedHash.equals(passwordHash)) {
                    return null; // wrong password
                }
                User u = new User();
                u.setUserId(rs.getInt("user_id"));
                u.setUsername(rs.getString("username"));
                u.setFullName(rs.getString("full_name"));
                u.setEmail(rs.getString("email"));
                u.setPhone(rs.getString("phone"));
                return u;
            }
        }
    }
}
