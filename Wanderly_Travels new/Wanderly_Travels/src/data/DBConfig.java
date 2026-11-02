package data;

/**
 * ============================================================
 *  ONLY FILE YOU NEED TO EDIT TO CONNECT TO YOUR OWN MYSQL
 * ============================================================
 * Change HOST, PORT, USERNAME and PASSWORD to match your local
 * MySQL installation. Everything else in the project will work
 * automatically once these are correct.
 */
public class DBConfig {

    public static final String HOST = "localhost";
    public static final String PORT = "3306";

    // Name of the database this application will create/use.
    public static final String DATABASE_NAME = "wanderly_db";

    // ---- EDIT THESE TWO LINES FOR YOUR OWN MYSQL SETUP ----
    public static final String USERNAME = "root";
    public static final String PASSWORD = "Root@1234";
    // ---------------------------------------------------------

    /** JDBC URL used when a specific database needs to be selected. */
    public static String getDbUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE_NAME
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    /** JDBC URL used before the database exists yet (connects to the MySQL server only). */
    public static String getServerUrl() {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/"
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }
}
