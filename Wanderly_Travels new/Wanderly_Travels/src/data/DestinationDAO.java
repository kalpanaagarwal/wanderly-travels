package data;

import model.Destination;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DestinationDAO {

    public List<Destination> getAllDestinations() throws SQLException {
        List<Destination> list = new ArrayList<>();
        String sql = "SELECT destination_id, name, location, description, tourist_info, image_path " +
                     "FROM destinations ORDER BY name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Destination d = new Destination();
                d.setDestinationId(rs.getInt("destination_id"));
                d.setName(rs.getString("name"));
                d.setLocation(rs.getString("location"));
                d.setDescription(rs.getString("description"));
                d.setTouristInfo(rs.getString("tourist_info"));
                d.setImagePath(rs.getString("image_path"));
                list.add(d);
            }
        }
        return list;
    }
}
