package ui;

import util.Session;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The navigation bar shown at the top of every page. Holds the logo on the
 * left, the main page links in the middle (updated with combined Explore Tours),
 * and either "Login / Register" or "Welcome, <name> / Logout" on the right.
 */
public class NavBar extends JPanel {

    private final MainFrame mainFrame;
    private final Map<String, JLabel> navLinks = new LinkedHashMap<>();
    private final JPanel rightPanel = new JPanel();
    private String activeCard = MainFrame.HOME;

    public NavBar(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setPreferredSize(new Dimension(100, 74));
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 24));

        add(new Logo(40, 20, Color.WHITE), BorderLayout.WEST);

        JPanel linksPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 22, 0));
        linksPanel.setOpaque(false);
        
        // Custom combined navigation menu logic
        addLink(linksPanel, "Home", MainFrame.HOME);
        addLink(linksPanel, "Explore Tours", MainFrame.EXPLORE_TOURS); // Combined option added here
        addLink(linksPanel, "Booking", MainFrame.BOOKING);
        addLink(linksPanel, "Contact", MainFrame.CONTACT);
        add(linksPanel, BorderLayout.CENTER);

        rightPanel.setOpaque(false);
        add(rightPanel, BorderLayout.EAST);

        refreshAuthState();
    }

    private void addLink(JPanel parent, String text, String card) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.bodyBold(14));
        label.setForeground(Color.WHITE);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.setBorder(BorderFactory.createEmptyBorder(6, 4, 8, 4));
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.navigateTo(card);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(Theme.GOLD_ACCENT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Color.WHITE);
                if (card.equals(activeCard)) {
                    label.setForeground(Theme.GOLD_ACCENT);
                }
            }
        });
        navLinks.put(card, label);
        parent.add(label);
    }

    /** Highlights the link for whichever page is currently showing. */
    public void setActive(String card) {
        activeCard = card;
        // Navigation normalization for map matching
        String checkCard = (card.equals("DESTINATIONS") || card.equals("PACKAGES")) ? MainFrame.EXPLORE_TOURS : card;
        navLinks.forEach((key, label) -> label.setForeground(key.equals(checkCard) ? Theme.GOLD_ACCENT : Color.WHITE));
    }

    /** Rebuilds the right-hand side: Login/Register when logged out, Welcome/Logout when logged in. */
    public void refreshAuthState() {
        rightPanel.removeAll();
        rightPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        if (Session.isLoggedIn()) {
            JLabel welcome = new JLabel("Hi, " + Session.getCurrentUser().getUsername());
            welcome.setFont(Theme.bodyBold(13));
            welcome.setForeground(Color.WHITE);
            rightPanel.add(welcome);

            RoundedButton logoutBtn = new RoundedButton("Logout", new Color(0xB0413E), Color.WHITE);
            logoutBtn.addActionListener(e -> mainFrame.onLogout());
            rightPanel.add(logoutBtn);
        } else {
            RoundedButton loginBtn = new RoundedButton("Login", Theme.PRIMARY, Color.WHITE);
            loginBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.LOGIN));

            RoundedButton registerBtn = new RoundedButton("Register", Theme.GOLD_ACCENT, Theme.NAVY);
            registerBtn.addActionListener(e -> mainFrame.navigateTo(MainFrame.REGISTER));

            rightPanel.add(loginBtn);
            rightPanel.add(registerBtn);
        }
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme.paintNavyGradient(g2, getWidth(), getHeight());
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRect(0, getHeight() - 2, getWidth(), 2);
        g2.dispose();
        super.paintComponent(g);
    }
}
