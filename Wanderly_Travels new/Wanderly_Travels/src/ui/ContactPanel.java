package ui;

import data.ContactDAO;
import model.ContactMessage;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.sql.SQLException;

public class ContactPanel extends JPanel {

    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JTextField subjectField = new JTextField();
    private final JTextArea messageArea = new JTextArea();

    public ContactPanel(MainFrame frame) {
        setLayout(new GridLayout(1, 2));

        JPanel infoSide = buildInfoSide();
        add(infoSide);
        add(buildFormSide());
    }

    private JPanel buildInfoSide() {
        DecorativeSidePanel panel = new DecorativeSidePanel("We'd Love to Hear From You",
                "Questions about a destination or package? Send us a message and our travel desk will get back to you.");

        JPanel infoBox = new JPanel();
        infoBox.setOpaque(false);
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBorder(BorderFactory.createEmptyBorder(0, 44, 30, 44));
        infoBox.add(contactLine("Address", "Wanderly Travels, MI Road, Jaipur, Rajasthan, India"));
        infoBox.add(Box.createVerticalStrut(14));
        infoBox.add(contactLine("Phone", "+91 98765 43210"));
        infoBox.add(Box.createVerticalStrut(14));
        infoBox.add(contactLine("Email", "support@wanderlytravels.example"));
        panel.add(infoBox, BorderLayout.CENTER);

        return panel;
    }

    private JPanel contactLine(String label, String value) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        // === NEW: a subtle gold accent bar on the left of each contact
        // block, plus a little breathing room, so this side of the page
        // doesn't look like plain stacked text. ===
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, Theme.GOLD_ACCENT),
                BorderFactory.createEmptyBorder(2, 12, 2, 0)));

        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(Theme.bodyBold(11));
        l.setForeground(new Color(180, 200, 230));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel v = new JLabel(value);
        v.setFont(Theme.body(14));
        v.setForeground(Color.WHITE);
        v.setAlignmentX(Component.LEFT_ALIGNMENT);

        row.add(l);
        row.add(v);
        return row;
    }

    private JPanel buildFormSide() {
        JPanel side = new JPanel(new GridBagLayout());
        side.setBackground(Color.WHITE);

        RoundedPanel formCard = new RoundedPanel();
        formCard.setPreferredSize(new Dimension(400, 520));
        formCard.setLayout(new BoxLayout(formCard, BoxLayout.Y_AXIS));
        formCard.setBorder(BorderFactory.createEmptyBorder(28, 34, 28, 34));

        JLabel title = new JLabel("Send Us a Message");
        title.setFont(Theme.heading(20));
        title.setForeground(Theme.NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(title);

        // === NEW: small gold divider under the title, and a short
        // subtitle line, so the card top doesn't feel bare. ===
        formCard.add(Box.createVerticalStrut(8));
        JPanel divider = new JPanel();
        divider.setBackground(Theme.GOLD_ACCENT);
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);
        divider.setMaximumSize(new Dimension(48, 3));
        divider.setPreferredSize(new Dimension(48, 3));
        formCard.add(divider);
        formCard.add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("Fill in the form below and our travel desk will get back to you.");
        subtitle.setFont(Theme.body(12));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formCard.add(subtitle);
        formCard.add(Box.createVerticalStrut(18));

        formCard.add(fieldBlock("Your Name", nameField));
        formCard.add(fieldBlock("Your Email", emailField));
        formCard.add(fieldBlock("Subject", subjectField));

        JLabel msgLabel = new JLabel("Message");
        msgLabel.setFont(Theme.bodyBold(12));
        msgLabel.setForeground(Theme.TEXT_MUTED);
        msgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        formCard.add(msgLabel);

        messageArea.setFont(Theme.body(14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SOFT, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        addFocusStyle(messageArea);
        JScrollPane msgScroll = new JScrollPane(messageArea);
        msgScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        msgScroll.setMaximumSize(new Dimension(400, 120));
        msgScroll.setPreferredSize(new Dimension(400, 120));
        formCard.add(msgScroll);

        formCard.add(Box.createVerticalStrut(18));
        RoundedButton sendBtn = new RoundedButton("Send Message", Theme.PRIMARY, Color.WHITE);
        sendBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        sendBtn.addActionListener(e -> sendMessage());
        formCard.add(sendBtn);

        side.add(formCard);
        return side;
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
        addFocusStyle(field);

        block.add(label);
        block.add(field);
        return block;
    }

    /**
     * === NEW: gives text fields and the message box a subtle "highlight"
     * effect - the border turns Wanderly blue while the field is focused,
     * and returns to the normal soft-grey border once the user clicks/tabs
     * away. Purely visual; doesn't change validation or behavior. ===
     */
    private void addFocusStyle(JTextComponent field) {
        Border normalBorder = field.getBorder();
        Border focusBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.PRIMARY, 2, true),
                BorderFactory.createEmptyBorder(5, 9, 5, 9));

        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focusBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(normalBorder);
            }
        });
    }

    private void sendMessage() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();

        if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in your name, email and message.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            new ContactDAO().saveMessage(new ContactMessage(name, email, subject, message));
            JOptionPane.showMessageDialog(this,
                    "Thank you, " + name + "! Your message has been received. We'll get back to you soon.",
                    "Message Sent", JOptionPane.INFORMATION_MESSAGE);
            nameField.setText("");
            emailField.setText("");
            subjectField.setText("");
            messageArea.setText("");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error while sending message:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}