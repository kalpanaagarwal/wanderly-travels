package data;

import model.Booking;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BookingDAO {

    /** Inserts a new booking row and returns the generated booking_id, or -1 on failure. */
    public int createBooking(Booking b) throws SQLException {
        String sql = "INSERT INTO bookings (user_id, package_id, travel_date, num_people, total_amount, " +
                     "contact_phone, contact_email, status) VALUES (?,?,?,?,?,?,?,?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getUserId());
            ps.setInt(2, b.getPackageId());
            ps.setDate(3, b.getTravelDate());
            ps.setInt(4, b.getNumPeople());
            ps.setBigDecimal(5, b.getTotalAmount());
            ps.setString(6, b.getContactPhone());
            ps.setString(7, b.getContactEmail());
            ps.setString(8, b.getStatus() == null ? "CONFIRMED" : b.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    /**
     * Returns only the bookings belonging to the given user_id.
     * This is what guarantees that Chavi's bookings and Rahul's bookings
     * never mix - every query is filtered by the logged-in user's numeric id.
     */
    public List<Booking> getBookingsForUser(int userId) throws SQLException {
        List<Booking> list = new ArrayList<>();
        String sql = "SELECT b.booking_id, b.user_id, b.package_id, p.package_name, b.travel_date, " +
                     "b.num_people, b.total_amount, b.contact_phone, b.contact_email, b.status, b.booking_date " +
                     "FROM bookings b JOIN tour_packages p ON b.package_id = p.package_id " +
                     "WHERE b.user_id = ? ORDER BY b.booking_date DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Booking b = new Booking();
                    b.setBookingId(rs.getInt("booking_id"));
                    b.setUserId(rs.getInt("user_id"));
                    b.setPackageId(rs.getInt("package_id"));
                    b.setPackageName(rs.getString("package_name"));
                    b.setTravelDate(rs.getDate("travel_date"));
                    b.setNumPeople(rs.getInt("num_people"));
                    b.setTotalAmount(rs.getBigDecimal("total_amount"));
                    b.setContactPhone(rs.getString("contact_phone"));
                    b.setContactEmail(rs.getString("contact_email"));
                    b.setStatus(rs.getString("status"));
                    b.setBookingDate(rs.getTimestamp("booking_date"));
                    list.add(b);
                }
            }
        }
        return list;
    }
}
