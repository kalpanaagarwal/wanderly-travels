package ui;

import data.UserDAO;
import model.User;
import util.PasswordUtil;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class RegisterPanel extends JPanel {

    private final MainFrame frame;
    private final JTextField usernameField = new JTextField();
    private final JTextField fullNameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public RegisterPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridLayout(1, 2));

        add(new DecorativeSidePanel("Start Your Journey With Us",
                "Create a free account to book tour packages and keep track of your trips."));
        add(buildFormSide());
    }

    private JScrollPane buildFormSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(Color.WHITE);

        RoundedPanel formCard = new RoundedPanel();
        formCard.setPreferredSize(new Dimension(400, 560));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));

        JLabel title = new JLabel("Create Your Account");
        title.setFont(Theme.heading(20));
        title.setForeground(Theme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(title);
        formCard.add(Box.createVerticalStrut(16));

        formCard.add(fieldBlock("Full Name", fullNameField));
        formCard.add(fieldBlock("Username", usernameField));
        formCard.add(fieldBlock("Email", emailField));
        formCard.add(fieldBlock("Phone Number", phoneField));
        formCard.add(fieldBlock("Password", passwordField));
        formCard.add(fieldBlock("Confirm Password", confirmPasswordField));

        formCard.add(Box.createVerticalStrut(10));
        RoundedButton registerBtn = new RoundedButton("Register", Theme.GOLD_ACCENT, Theme.NAVY);
        registerBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        registerBtn.addActionListener(e -> doRegister());
        formCard.add(registerBtn);

        formCard.add(Box.createVerticalStrut(14));
        JPanel loginRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        loginRow.setOpaque(false);
        JLabel prompt = new JLabel("Already have an account?");
        prompt.setFont(Theme.body(12));
        JButton loginLink = new JButton("Login");
        loginLink.setFont(Theme.bodyBold(12));
        loginLink.setForeground(Theme.PRIMARY_DARK);
        loginLink.setBorderPainted(false);
        loginLink.setContentAreaFilled(false);
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.addActionListener(e -> frame.navigateTo(MainFrame.LOGIN));
        loginRow.add(prompt);
        loginRow.add(loginLink);
        loginRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(loginRow);

        side.add(formCard);

        JScrollPane scroll = new JScrollPane(side);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JPanel fieldBlock(String labelText, JTextField field) {
        JPanel block = new JPanel();
        block.setOpaque(false);
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        block.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.bodyBold(12));
        label.setForeground(Theme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        field.setFont(Theme.body(14));
        field.setMaximumSize(new Dimension(400, 36));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SOFT, 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        block.add(label);
        block.add(field);
        return block;
    }

    /** Clears the form each time the page is shown. */
    public void resetForm() {
        usernameField.setText("");
        fullNameField.setText("");
        emailField.setText("");
        phoneField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmPasswordField.getPassword());

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in your name, username and password.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (username.length() < 3) {
            JOptionPane.showMessageDialog(this, "Username must be at least 3 characters long.",
                    "Invalid Username", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length() < 4) {
            JOptionPane.showMessageDialog(this, "Password must be at least 4 characters long.",
                    "Weak Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Password and Confirm Password do not match.",
                    "Password Mismatch", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            User created = new UserDAO().register(username, PasswordUtil.hash(password), fullName, email, phone);
            if (created == null) {
                JOptionPane.showMessageDialog(this,
                        "Username already exists. Please choose another username.",
                        "Registration Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this,
                    "Registration successful! You can now login with your new account.",
                    "Welcome to Wanderly Travels", JOptionPane.INFORMATION_MESSAGE);
            resetForm();
            frame.navigateTo(MainFrame.LOGIN);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error while registering:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
