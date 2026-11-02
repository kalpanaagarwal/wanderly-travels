package ui;

import model.Destination;
import model.TourPackage;

import javax.swing.*;
import java.awt.*;

/**
 * Builds the reusable "card" components for destinations and tour packages,
 * updated to redirect directly to the new combined ExploreToursPanel.
 */
public final class CardFactory {

    private CardFactory() {
    }

    public static final int CARD_W = 280;
    public static final int IMG_H = 160;

    public static RoundedPanel buildDestinationCard(Destination d, MainFrame frame) {
        RoundedPanel card = new RoundedPanel();
        card.setPreferredSize(new Dimension(CARD_W, 300));
        card.enableHoverEffect();
        card.setLayout(new BorderLayout());

        JLabel image = new JLabel(
                ImageUtil.getScaledIcon(d.getImagePath(), CARD_W - 4, IMG_H)
        );
        image.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(image, BorderLayout.NORTH);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));

        JLabel name = new JLabel(d.getName());
        name.setFont(Theme.heading(17));
        name.setForeground(Theme.NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel loc = new JLabel(d.getLocation());
        loc.setFont(Theme.body(12));
        loc.setForeground(Theme.TEXT_MUTED);
        loc.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea desc = new JTextArea(shorten(d.getDescription(), 90));
        desc.setFont(Theme.body(12));
        desc.setForeground(Theme.TEXT_DARK);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));

        RoundedButton detailsBtn = new RoundedButton("View Details", Theme.PRIMARY, Color.WHITE);
        detailsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Click Action: Naye combined explore tours page par le jayega
        detailsBtn.addActionListener(e -> frame.navigateTo(MainFrame.EXPLORE_TOURS));

        textBox.add(name);
        textBox.add(loc);
        textBox.add(desc);
        textBox.add(detailsBtn);

        card.add(textBox, BorderLayout.CENTER);
        return card;
    }

    public static RoundedPanel buildPackageCard(TourPackage p, MainFrame frame) {
        RoundedPanel card = new RoundedPanel();
        card.setPreferredSize(new Dimension(CARD_W, 340));
        card.enableHoverEffect();
        card.setLayout(new BorderLayout());

        JLabel image = new JLabel(
                ImageUtil.getScaledIcon(p.getImagePath(), CARD_W - 4, IMG_H)
        );
        image.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(image, BorderLayout.NORTH);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));

        JLabel name = new JLabel(p.getPackageName());
        name.setFont(Theme.heading(15));
        name.setForeground(Theme.NAVY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel meta = new JLabel(p.getDuration() + "  |  " + p.getDestinationName());
        meta.setFont(Theme.body(12));
        meta.setForeground(Theme.TEXT_MUTED);
        meta.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea desc = new JTextArea(shorten(p.getDescription(), 80));
        desc.setFont(Theme.body(12));
        desc.setForeground(Theme.TEXT_DARK);
        desc.setLineWrap(true);
        desc.setWrapStyleWord(true);
        desc.setEditable(false);
        desc.setOpaque(false);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        JLabel price = new JLabel("Rs. " + p.getPrice() + " / person");
        price.setFont(Theme.heading(16));
        price.setForeground(Theme.PRIMARY_DARK);
        price.setAlignmentX(Component.LEFT_ALIGNMENT);
        price.setBorder(BorderFactory.createEmptyBorder(2, 0, 8, 0));

        RoundedButton bookBtn = new RoundedButton("Book Now", Theme.GOLD_ACCENT, Theme.NAVY);
        bookBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Click Action: Naye combined explore tours page par le jayega
        bookBtn.addActionListener(e -> frame.navigateTo(MainFrame.EXPLORE_TOURS));

        textBox.add(name);
        textBox.add(meta);
        textBox.add(desc);
        textBox.add(price);
        textBox.add(bookBtn);

        card.add(textBox, BorderLayout.CENTER);
        return card;
    }

    private static String shorten(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max) + "...";
    }
}
