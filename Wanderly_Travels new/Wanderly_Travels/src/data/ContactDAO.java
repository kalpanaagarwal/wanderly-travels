package data;

import model.ContactMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ContactDAO {

    public void saveMessage(ContactMessage m) throws SQLException {
        String sql = "INSERT INTO contact_messages (name, email, subject, message) VALUES (?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getEmail());
            ps.setString(3, m.getSubject());
            ps.setString(4, m.getMessage());
            ps.executeUpdate();
        }
    }
}
