package ui;

import data.UserDAO;
import model.User;
import util.PasswordUtil;
import util.Session;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class LoginPanel extends JPanel {

    private final MainFrame frame;
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    public LoginPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridLayout(1, 2));

        add(new DecorativeSidePanel("Welcome Back, Traveller",
                "Login to manage your bookings and plan your next Rajasthan getaway."));
        add(buildFormSide());
    }

    private JPanel buildFormSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(Color.WHITE);

        RoundedPanel formCard = new RoundedPanel();
        formCard.setPreferredSize(new Dimension(360, 340));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(30, 32, 30, 32));

        JLabel title = new JLabel("Login to Your Account");
        title.setFont(Theme.heading(20));
        title.setForeground(Theme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        formCard.add(title);
        formCard.add(Box.createVerticalStrut(20));
        formCard.add(fieldLabel("Username"));
        styleField(usernameField);
        formCard.add(usernameField);
        formCard.add(Box.createVerticalStrut(14));
        formCard.add(fieldLabel("Password"));
        styleField(passwordField);
        formCard.add(passwordField);
        formCard.add(Box.createVerticalStrut(22));

        RoundedButton loginBtn = new RoundedButton("Login", Theme.PRIMARY, Color.WHITE);
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.addActionListener(e -> doLogin());
        formCard.add(loginBtn);

        formCard.add(Box.createVerticalStrut(16));
        JPanel registerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        registerRow.setOpaque(false);
        JLabel prompt = new JLabel("New here?");
        prompt.setFont(Theme.body(12));
        JButton registerLink = new JButton("Create an account");
        registerLink.setFont(Theme.bodyBold(12));
        registerLink.setForeground(Theme.PRIMARY_DARK);
        registerLink.setBorderPainted(false);
        registerLink.setContentAreaFilled(false);
        registerLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerLink.addActionListener(e -> frame.navigateTo(MainFrame.REGISTER));
        registerRow.add(prompt);
        registerRow.add(registerLink);
        registerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(registerRow);

        // Allow pressing Enter in the password field to log in.
        passwordField.addActionListener(e -> doLogin());

        side.add(formCard);
        return side;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyBold(12));
        l.setForeground(Theme.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        return l;
    }

    private void styleField(JTextField field) {
        field.setFont(Theme.body(14));
        field.setMaximumSize(new Dimension(400, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SOFT, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    /** Clears the form each time the page is shown, so old input never lingers. */
    public void resetForm() {
        usernameField.setText("");
        passwordField.setText("");
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User user = new UserDAO().login(username, PasswordUtil.hash(password));
            if (user == null) {
                JOptionPane.showMessageDialog(this, "Invalid username or password.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Session.login(user);
            resetForm();
            frame.onLoginSuccess();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error while logging in:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
