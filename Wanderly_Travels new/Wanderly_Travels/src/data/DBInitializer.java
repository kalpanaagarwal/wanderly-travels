package data;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Runs once when the application starts.
 * 1) Creates the wanderly_db database if it does not exist yet.
 * 2) Creates every required table if it does not exist yet.
 * 3) Inserts starter destinations and tour packages if the tables are empty.
 *
 * This means a brand-new student setup only needs a working MySQL server;
 * no manual SQL needs to be typed (schema.sql in /sql is kept as a backup
 * / reference only, as required by the project brief).
 */
public class DBInitializer {

    public static void initialize() throws SQLException {
        createDatabaseIfMissing();
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement()) {

            createTables(st);
            seedDestinationsAndPackagesIfEmpty(conn);
        }
    }

    private static void createDatabaseIfMissing() throws SQLException {
        try (Connection conn = DBConnection.getServerConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DBConfig.DATABASE_NAME
                    + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private static void createTables(Statement st) throws SQLException {
        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS users (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password_hash VARCHAR(256) NOT NULL,
                full_name VARCHAR(100),
                email VARCHAR(100),
                phone VARCHAR(20),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
        """);

        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS destinations (
                destination_id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                location VARCHAR(150),
                description TEXT,
                tourist_info TEXT,
                image_path VARCHAR(255)
            ) ENGINE=InnoDB
        """);

        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS tour_packages (
                package_id INT AUTO_INCREMENT PRIMARY KEY,
                package_name VARCHAR(150) NOT NULL,
                destination_id INT,
                duration VARCHAR(50),
                price DECIMAL(10,2) NOT NULL,
                description TEXT,
                image_path VARCHAR(255),
                CONSTRAINT fk_package_destination FOREIGN KEY (destination_id)
                    REFERENCES destinations(destination_id) ON DELETE SET NULL
            ) ENGINE=InnoDB
        """);

        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                package_id INT NOT NULL,
                travel_date DATE NOT NULL,
                num_people INT NOT NULL,
                total_amount DECIMAL(10,2) NOT NULL,
                contact_phone VARCHAR(20),
                contact_email VARCHAR(100),
                status VARCHAR(20) DEFAULT 'CONFIRMED',
                booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_booking_user FOREIGN KEY (user_id)
                    REFERENCES users(user_id) ON DELETE CASCADE,
                CONSTRAINT fk_booking_package FOREIGN KEY (package_id)
                    REFERENCES tour_packages(package_id) ON DELETE CASCADE
            ) ENGINE=InnoDB
        """);

        st.executeUpdate("""
            CREATE TABLE IF NOT EXISTS contact_messages (
                message_id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100),
                email VARCHAR(100),
                subject VARCHAR(150),
                message TEXT,
                sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB
        """);
    }

    private static void seedDestinationsAndPackagesIfEmpty(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM destinations");
            rs.next();
            if (rs.getInt("c") > 0) {
                return; // already seeded, do nothing
            }
        }

        String[][] destinations = {
            // name, location, description, tourist_info, image
            {"Jaipur", "Rajasthan, India",
             "The Pink City, famous for its royal forts, palaces and vibrant bazaars.",
             "Best time: Oct-Mar. Must see: Amber Fort, City Palace, Hawa Mahal, Jantar Mantar.",
             "images/jaipur.jpg"},
            {"Udaipur", "Rajasthan, India",
             "The City of Lakes, known for its romantic lake palaces and Rajput architecture.",
             "Best time: Sep-Mar. Must see: Lake Pichola, City Palace, Jag Mandir.",
             "images/udaipur.jpg"},
            {"Jodhpur", "Rajasthan, India",
             "The Blue City, home to the majestic Mehrangarh Fort overlooking the old town.",
             "Best time: Oct-Mar. Must see: Mehrangarh Fort, Umaid Bhawan Palace, Jaswant Thada.",
             "images/jodhpur.jpg"},
            {"Jaisalmer", "Rajasthan, India",
             "The Golden City, rising out of the Thar Desert with a living sandstone fort.",
             "Best time: Nov-Feb. Must see: Jaisalmer Fort, Sam Sand Dunes, Patwon Ki Haveli.",
             "images/jaisalmer.jpg"},
            {"Pushkar", "Rajasthan, India",
             "A serene lake town famous for its ghats and the rare Brahma Temple.",
             "Best time: Oct-Mar. Must see: Pushkar Lake, Brahma Temple, Pushkar Camel Fair.",
             "images/pushkar.jpg"},
            {"Mount Abu", "Rajasthan, India",
             "Rajasthan's only hill station, known for cool weather and the Dilwara Jain temples.",
             "Best time: Mar-Jun, Sep-Nov. Must see: Dilwara Temples, Nakki Lake, Guru Shikhar.",
             "images/mountabu.jpg"},
            {"Bikaner", "Rajasthan, India",
             "A desert city famous for Junagarh Fort, havelis and the unique Karni Mata Temple.",
             "Best time: Oct-Mar. Must see: Junagarh Fort, Karni Mata Temple, Camel Research Farm.",
             "images/bikaner.jpg"},
            {"Ranthambore", "Rajasthan, India",
             "One of India's finest tiger reserves, set around a dramatic ancient fort.",
             "Best time: Oct-Jun. Must see: Ranthambore National Park, Ranthambore Fort.",
             "images/ranthambore.jpg"},
            {"Chittorgarh", "Rajasthan, India",
             "Home to India's largest fort complex, steeped in Rajput history and legends.",
             "Best time: Oct-Mar. Must see: Chittorgarh Fort, Vijay Stambh, Rani Padmini's Palace.",
             "images/chittorgarh.jpg"},
            {"Agra", "Uttar Pradesh, India",
             "Home to the iconic Taj Mahal, a short trip from Rajasthan and a must on every itinerary.",
             "Best time: Oct-Mar. Must see: Taj Mahal, Agra Fort, Fatehpur Sikri.",
             "images/agra.jpg"}
        };

        String insertDest = "INSERT INTO destinations (name, location, description, tourist_info, image_path) VALUES (?,?,?,?,?)";
        int[] destIds = new int[destinations.length];
        try (var ps = conn.prepareStatement(insertDest, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < destinations.length; i++) {
                String[] d = destinations[i];
                ps.setString(1, d[0]);
                ps.setString(2, d[1]);
                ps.setString(3, d[2]);
                ps.setString(4, d[3]);
                ps.setString(5, d[4]);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        destIds[i] = keys.getInt(1);
                    }
                }
            }
        }

        // index into destIds/destinations array: 0 Jaipur,1 Udaipur,2 Jodhpur,3 Jaisalmer,
        // 4 Pushkar,5 Mount Abu,6 Bikaner,7 Ranthambore,8 Chittorgarh,9 Agra
        Object[][] packages = {
            {"Royal Jaipur Escape", destIds[0], "3 Days / 2 Nights", "8999",
             "Explore the Pink City's forts, palaces and bazaars with a private guide.", "images/jaipur.jpg"},
            {"Lake City Udaipur Retreat", destIds[1], "4 Days / 3 Nights", "12999",
             "A relaxing stay by Lake Pichola with palace and boat-ride experiences.", "images/udaipur.jpg"},
            {"Blue City Jodhpur Adventure", destIds[2], "3 Days / 2 Nights", "9499",
             "Discover Mehrangarh Fort and the winding blue lanes of the old city.", "images/jodhpur.jpg"},
            {"Golden Jaisalmer Desert Safari", destIds[3], "4 Days / 3 Nights", "14999",
             "Camel safari and overnight desert camp under the stars at Sam Dunes.", "images/jaisalmer.jpg"},
            {"Pushkar Spiritual Getaway", destIds[4], "2 Days / 1 Night", "5999",
             "A peaceful short trip to the ghats and temples of holy Pushkar.", "images/pushkar.jpg"},
            {"Wildlife Ranthambore Trail", destIds[7], "3 Days / 2 Nights", "11999",
             "Jungle safaris in search of the Bengal tiger, with fort views included.", "images/ranthambore.jpg"},
            {"Rajasthan Grand Circuit", destIds[0], "7 Days / 6 Nights", "24999",
             "The complete royal tour: Jaipur, Jodhpur and Udaipur in one grand trip.", "images/udaipur.jpg"},
            {"Taj & Pink City Combo", destIds[9], "5 Days / 4 Nights", "15999",
             "See the Taj Mahal in Agra and the Pink City of Jaipur back to back.", "images/agra.jpg"}
        };

        String insertPkg = "INSERT INTO tour_packages (package_name, destination_id, duration, price, description, image_path) VALUES (?,?,?,?,?,?)";
        try (var ps = conn.prepareStatement(insertPkg)) {
            for (Object[] p : packages) {
                ps.setString(1, (String) p[0]);
                ps.setInt(2, (Integer) p[1]);
                ps.setString(3, (String) p[2]);
                ps.setBigDecimal(4, new java.math.BigDecimal((String) p[3]));
                ps.setString(5, (String) p[4]);
                ps.setString(6, (String) p[5]);
                ps.executeUpdate();
            }
        }
    }
}
