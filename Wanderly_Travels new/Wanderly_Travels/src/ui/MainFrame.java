package ui;

import model.TourPackage;
import util.Session;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    // Humne saare variables wapas rakh diye hain taaki dusri files mein error na aaye
    public static final String HOME = "HOME";
    public static final String DESTINATIONS = "DESTINATIONS";
    public static final String PACKAGES = "PACKAGES";
    public static final String EXPLORE_TOURS = "EXPLORE_TOURS"; 
    public static final String BOOKING = "BOOKING";
    public static final String LOGIN = "LOGIN";
    public static final String REGISTER = "REGISTER";
    public static final String CONTACT = "CONTACT";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);
    private final NavBar navBar;

    private HomePanel homePanel;
    private ExploreToursPanel exploreToursPanel; 
    private BookingPanel bookingPanel;
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private ContactPanel contactPanel;

    public MainFrame() {
        super("Wanderly Travels - Discover Incredible India");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 680));
        setLocationRelativeTo(null);

        navBar = new NavBar(this);

        homePanel = new HomePanel(this);
        exploreToursPanel = new ExploreToursPanel(); 
        bookingPanel = new BookingPanel(this);
        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);
        contactPanel = new ContactPanel(this);

        contentPanel.add(homePanel, HOME);
        contentPanel.add(exploreToursPanel, EXPLORE_TOURS); 
        contentPanel.add(bookingPanel, BOOKING);
        contentPanel.add(loginPanel, LOGIN);
        contentPanel.add(registerPanel, REGISTER);
        contentPanel.add(contactPanel, CONTACT);

        setLayout(new BorderLayout());
        add(navBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        navigateTo(HOME);
    }

    public void navigateTo(String card) {
        navBar.refreshAuthState();

        // Agar purane panels par click ho toh automatic naye page par le jaye
        if (card.equals(DESTINATIONS) || card.equals(PACKAGES) || card.equals(EXPLORE_TOURS)) {
            card = EXPLORE_TOURS;
        }

        if (card.equals(BOOKING) && !Session.isLoggedIn()) {
            showLoginRequiredDialog();
            card = LOGIN;
        }

        if (card.equals(BOOKING)) bookingPanel.refresh(null);
        if (card.equals(LOGIN)) loginPanel.resetForm();
        if (card.equals(REGISTER)) registerPanel.resetForm();

        cardLayout.show(contentPanel, card);
        navBar.setActive(card);
    }

    public void navigateToBookingWith(TourPackage pkg) {
        if (!Session.isLoggedIn()) {
            showLoginRequiredDialog();
            navigateTo(LOGIN);
            return;
        }
        bookingPanel.refresh(pkg);
        cardLayout.show(contentPanel, BOOKING);
        navBar.setActive(BOOKING);
    }

    private void showLoginRequiredDialog() {
        JDialog dialog = new JDialog(this, "Login Required", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Color.WHITE);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.NAVY);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel headerTitle = new JLabel("LOGIN REQUIRED");
        headerTitle.setFont(Theme.heading(16));
        headerTitle.setForeground(Color.WHITE);
        header.add(headerTitle, BorderLayout.WEST);

        main.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setBackground(Color.WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(22, 24, 18, 24));

        JLabel message = new JLabel("Please login first to book a tour package.");
        message.setFont(Theme.body(14));
        message.setForeground(Theme.TEXT_DARK);
        message.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(message);

        main.add(content, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 12));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 230, 235)));

        RoundedButton okButton = new RoundedButton("OK", Theme.GOLD_ACCENT, Theme.NAVY);
        okButton.setPreferredSize(new Dimension(90, 36));
        okButton.addActionListener(e -> dialog.dispose());
        bottom.add(okButton);

        main.add(bottom, BorderLayout.SOUTH);

        dialog.setContentPane(main);
        dialog.pack();
        dialog.setSize(Math.max(dialog.getWidth(), 380), dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void onLoginSuccess() {
        navBar.refreshAuthState();
        navigateTo(HOME);
    }

    public void onLogout() {
        Session.logout();
        navBar.refreshAuthState();
        navigateTo(HOME);
    }
}
