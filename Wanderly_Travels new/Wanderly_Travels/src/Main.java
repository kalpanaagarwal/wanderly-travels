import data.DBInitializer;
import ui.MainFrame;

import javax.swing.*;
import java.sql.SQLException;

/**
 * Entry point of the Wanderly Travels desktop application.
 *
 * On startup this class:
 *   1. Connects to MySQL and makes sure the wanderly_db database and all
 *      required tables exist (creating and seeding them on first run).
 *   2. Opens the main Swing window.
 *
 * If the database connection fails (for example, MySQL is not running, or
 * the username/password in data/DBConfig.java is wrong), a clear error
 * dialog is shown instead of letting the application crash silently.
 */
public class Main {

    public static void main(String[] args) {
        // Use the operating system's native look and feel as a base;
        // our own custom-painted components then sit on top of it.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // If this fails, Swing simply falls back to its default look and feel.
        }

        SwingUtilities.invokeLater(() -> {
            try {
                DBInitializer.initialize();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to MySQL.\n\n" +
                        "Please check that:\n" +
                        "  1. MySQL Server is installed and running\n" +
                        "  2. The username & password in src/data/DBConfig.java are correct\n" +
                        "  3. The MySQL Connector/J .jar file is added to the project (see README.md)\n\n" +
                        "Technical details:\n" + e.getMessage(),
                        "Database Connection Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
