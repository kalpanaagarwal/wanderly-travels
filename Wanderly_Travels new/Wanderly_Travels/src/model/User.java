package model;

/**
 * Simple data holder (POJO) representing one row of the "users" table.
 * Demonstrates encapsulation: fields are private, accessed via getters/setters.
 */
public class User {
    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String phone;

    public User() {
    }

    public User(int userId, String username, String fullName, String email, String phone) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return fullName != null && !fullName.isBlank() ? fullName : username;
    }
}
