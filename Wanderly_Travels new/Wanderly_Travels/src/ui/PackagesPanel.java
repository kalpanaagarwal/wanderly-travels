package ui;

import data.PackageDAO;
import model.TourPackage;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class PackagesPanel extends JPanel {

    private final MainFrame frame;
    private final JPanel grid = new JPanel(new GridLayout(0, 3, 20, 20));
    public PackagesPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel header = new JPanel();
        header.setBackground(Theme.SKY_LIGHT);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(BorderFactory.createEmptyBorder(26, 40, 20, 40));

        JLabel title = new JLabel("Tour Packages");
        title.setFont(Theme.heading(26));
        title.setForeground(Theme.NAVY);

        JLabel subtitle = new JLabel("Ready-made itineraries across Rajasthan and Agra - just pick and book.");
        subtitle.setFont(Theme.body(14));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        add(header, BorderLayout.NORTH);

        grid.setBackground(Color.WHITE);
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    /** Reloads the package list from the database each time this page is shown. */
    public void refresh() {
        grid.removeAll();
        try {
            List<TourPackage> packages = new PackageDAO().getAllPackages();
            if (packages.isEmpty()) {
                grid.add(infoLabel("No tour packages found yet."));
            }
            for (TourPackage p : packages) {
                grid.add(CardFactory.buildPackageCard(p, frame));
            }
        } catch (SQLException ex) {
            grid.add(infoLabel("Could not load packages. Please check your database connection in DBConfig.java."));
        }
        grid.revalidate();
        grid.repaint();
    }

    private JLabel infoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.body(14));
        label.setForeground(Theme.ERROR);
        return label;
    }
}
