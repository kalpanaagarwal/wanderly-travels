package data;

import model.TourPackage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PackageDAO {

    public List<TourPackage> getAllPackages() throws SQLException {
        List<TourPackage> list = new ArrayList<>();
        String sql = "SELECT p.package_id, p.package_name, p.destination_id, d.name AS destination_name, " +
                     "p.duration, p.price, p.description, p.image_path " +
                     "FROM tour_packages p LEFT JOIN destinations d ON p.destination_id = d.destination_id " +
                     "ORDER BY p.package_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public TourPackage getPackageById(int packageId) throws SQLException {
        String sql = "SELECT p.package_id, p.package_name, p.destination_id, d.name AS destination_name, " +
                     "p.duration, p.price, p.description, p.image_path " +
                     "FROM tour_packages p LEFT JOIN destinations d ON p.destination_id = d.destination_id " +
                     "WHERE p.package_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, packageId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    private TourPackage mapRow(ResultSet rs) throws SQLException {
        TourPackage p = new TourPackage();
        p.setPackageId(rs.getInt("package_id"));
        p.setPackageName(rs.getString("package_name"));
        p.setDestinationId(rs.getInt("destination_id"));
        p.setDestinationName(rs.getString("destination_name"));
        p.setDuration(rs.getString("duration"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setDescription(rs.getString("description"));
        p.setImagePath(rs.getString("image_path"));
        return p;
    }
}
