package data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central place that hands out JDBC connections. Keeping this logic in one
 * class (instead of scattering DriverManager.getConnection calls across the
 * GUI) makes the project easier to maintain and to explain in a viva.
 */
public class DBConnection {

    static {
        try {
            // Not strictly required on modern JDBC drivers/JVMs, but kept
            // here so the driver registration step is explicit and easy
            // to explain.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL Connector/J driver not found on the classpath. " +
                    "Make sure mysql-connector-j-x.x.x.jar is added to the project (see README.md).");
        }
    }

    private DBConnection() {
        // utility class, no instances
    }

    /** Connection to the MySQL server without selecting a database (used only to CREATE the database). */
    public static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(DBConfig.getServerUrl(), DBConfig.USERNAME, DBConfig.PASSWORD);
    }

    /** Normal connection to the wanderly_db database, used by every DAO class. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DBConfig.getDbUrl(), DBConfig.USERNAME, DBConfig.PASSWORD);
    }
}
