package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The landing page of the application: a hero banner, a short "why choose us"
 * strip, and EVERY destination as a photo card with a short description.
 * "View Details" opens the Explore Tours page on that exact destination.
 */
public class HomePanel extends JPanel {

    private static final int CARD_W = 270;
    private static final int CARD_H = 350;
    private static final int PHOTO_H = 190;

    private final MainFrame frame;
    private final JPanel featuredGrid = new JPanel(new WrapLayout(FlowLayout.CENTER, 22, 22));
    private JScrollPane scroll;
    private JPanel destinationsSection;

    // ---------------------------------------------------------------------
    //  Short description shown on each destination card.
    //  (If a city is not listed here, the description from the database is used.)
    // ---------------------------------------------------------------------
    private static final Map<String, String> SHORT_DESC = new HashMap<>();
    static {
        SHORT_DESC.put("jaipur",      "The Pink City - royal palaces, Amer Fort, Hawa Mahal and colourful bazaars.");
        SHORT_DESC.put("udaipur",     "The City of Lakes - romantic palaces, boat rides and unforgettable sunsets.");
        SHORT_DESC.put("jodhpur",     "The Blue City - the mighty Mehrangarh Fort and blue-washed old town lanes.");
        SHORT_DESC.put("jaisalmer",   "The Golden City - a living sandstone fort, Sam sand dunes and desert camping.");
        SHORT_DESC.put("pushkar",     "A sacred lake town of ghats and temples, famous for its annual camel fair.");
        SHORT_DESC.put("mountabu",    "Rajasthan's only hill station - cool air, Nakki Lake and the Dilwara Jain Temples.");
        SHORT_DESC.put("bikaner",     "A desert city of Junagarh Fort, Karni Mata Temple and crunchy Bikaneri bhujia.");
        SHORT_DESC.put("ranthambore", "Tiger country - jeep safaris through a national park with an ancient hilltop fort.");
        SHORT_DESC.put("chittorgarh", "India's grandest hill fort - towers, temples and palaces full of tales of valour.");
        SHORT_DESC.put("agra",        "Home of the Taj Mahal, with Agra Fort and Fatehpur Sikri close by.");
        // spelling variants
        SHORT_DESC.put("mtabu",        SHORT_DESC.get("mountabu"));
        SHORT_DESC.put("chittaurgarh", SHORT_DESC.get("chittorgarh"));
        SHORT_DESC.put("ranthambhore", SHORT_DESC.get("ranthambore"));
    }

    public HomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());

        JPanel content = new ScrollableColumn();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);

        content.add(buildHero());
        content.add(buildFeatureStrip());
        destinationsSection = buildFeaturedSection();
        content.add(destinationsSection);

        // the cards wrap onto new rows when the window size changes
        content.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { featuredGrid.revalidate(); }
        });

        scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHero() {
        JPanel hero = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Theme.paintNavyGradient(g2, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(100, 320));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Discover Incredible Rajasthan & India");
        title.setFont(Theme.heading(32));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Royal forts, golden deserts and timeless cities - all planned for you.");
        subtitle.setFont(Theme.body(16));
        subtitle.setForeground(new Color(210, 225, 245));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 24, 0));

        JPanel buttonsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttonsRow.setOpaque(false);

        // 1) scrolls down to the destination cards on this same page
        RoundedButton exploreBtn = new RoundedButton("Explore Destinations", Theme.GOLD_ACCENT, Theme.NAVY);
        exploreBtn.addActionListener(e -> scrollToDestinations());

        // 2) goes straight to the Booking page
        RoundedButton bookBtn = new RoundedButton("Book Your Trip", Theme.ACCENT_BLUE, Color.WHITE);
        bookBtn.addActionListener(e -> frame.navigateTo(MainFrame.BOOKING));

        buttonsRow.add(exploreBtn);
        buttonsRow.add(bookBtn);
        buttonsRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(subtitle);
        textPanel.add(buttonsRow);

        hero.add(textPanel);
        return hero;
    }

    /** Smoothly scrolls the page down to the destination cards. */
    private void scrollToDestinations() {
        final JViewport vp = scroll.getViewport();
        final int target = Math.max(0, destinationsSection.getY() - 10);
        final int start = vp.getViewPosition().y;
        final int steps = 14;
        final int[] step = {0};
        final javax.swing.Timer t = new javax.swing.Timer(15, null);
        t.addActionListener(e -> {
            step[0]++;
            double p = step[0] / (double) steps;
            double eased = 1 - Math.pow(1 - p, 3);
            vp.setViewPosition(new Point(0, (int) (start + (target - start) * eased)));
            if (step[0] >= steps) t.stop();
        });
        t.start();
    }

    private JPanel buildFeatureStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 4, 16, 0));
        strip.setBackground(Theme.SKY_LIGHT);
        strip.setBorder(BorderFactory.createEmptyBorder(28, 40, 28, 40));

        strip.add(featureItem("Handpicked Destinations", "Authentic Rajasthan & Indian heritage spots."));
        strip.add(featureItem("Best Price Guarantee", "Transparent pricing with no hidden costs."));
        strip.add(featureItem("Easy Online Booking", "Book a package in a few clicks."));
        strip.add(featureItem("Trusted by Travellers", "Curated packages, planned end to end."));
        return strip;
    }

    private JPanel featureItem(String title, String desc) {
        JPanel item = new JPanel();
        item.setOpaque(false);
        item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));

        JLabel dot = new JLabel("✦");
        dot.setFont(Theme.heading(20));
        dot.setForeground(Theme.PRIMARY);
        dot.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel t = new JLabel("<html><b>" + title + "</b></html>");
        t.setFont(Theme.bodyBold(14));
        t.setForeground(Theme.NAVY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        t.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));

        JLabel d = new JLabel("<html><div style='width:180px'>" + desc + "</div></html>");
        d.setFont(Theme.body(12));
        d.setForeground(Theme.TEXT_MUTED);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);

        item.add(dot);
        item.add(t);
        item.add(d);
        return item;
    }

    // =====================================================================
    //  All destinations
    // =====================================================================
    private JPanel buildFeaturedSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(BorderFactory.createEmptyBorder(30, 40, 40, 40));

        JLabel heading = new JLabel("Explore Our Destinations");
        heading.setFont(Theme.heading(22));
        heading.setForeground(Theme.NAVY);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Tap View Details to see photos, best time to visit, top attractions and tour packages.");
        sub.setFont(Theme.body(13));
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        top.add(heading);
        top.add(Box.createRigidArea(new Dimension(0, 4)));
        top.add(sub);
        section.add(top, BorderLayout.NORTH);

        featuredGrid.setBackground(Color.WHITE);
        showDestinations(fetchDestinations());
        section.add(featuredGrid, BorderLayout.CENTER);
        return section;
    }

    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/wanderly_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, "root", "Root@1234");
    }

    /** Reads EVERY destination from the database. Each row = {name, image_path, location, description}. */
    private List<String[]> fetchDestinations() {
        List<String[]> rows = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM destinations")) {
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("name"), rs.getString("image_path"),
                        rs.getString("location"), rs.getString("description")});
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;                       // null = could not load
        }
        return rows;
    }

    private void showDestinations(List<String[]> rows) {
        featuredGrid.removeAll();
        featuredGrid.setLayout(new WrapLayout(FlowLayout.CENTER, 22, 22));

        if (rows == null) {
            JLabel error = new JLabel("Could not load destinations. Please check the database connection.");
            error.setForeground(Theme.ERROR);
            featuredGrid.add(error);
        } else if (rows.isEmpty()) {
            featuredGrid.add(new JLabel("No destinations found in the database yet."));
        } else {
            for (String[] r : rows) {
                featuredGrid.add(buildDestinationCard(r[0], r[1], r[2], r[3]));
            }
        }
        featuredGrid.revalidate();
        featuredGrid.repaint();
    }

    private void openDetails(String cityName) {
        ExploreToursPanel.requestCity(cityName);          // remember which destination to open
        frame.navigateTo(MainFrame.DESTINATIONS);         // go to the Explore Tours page
    }

    private static String shortDescription(String cityName, String dbDescription) {
        String s = SHORT_DESC.get(cityName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        if (s != null) return s;
        s = dbDescription == null ? "" : dbDescription.trim();
        if (s.length() > 95) s = s.substring(0, 92).trim() + "...";
        return s;
    }

    private JComponent buildDestinationCard(String name, String imgPath, String location, String description) {
        final String cityName = name == null ? "" : name.trim();
        Image photo = ExploreToursPanel.coverPhoto(cityName, imgPath);

        CardPanel card = new CardPanel();
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(CARD_W, CARD_H));

        PhotoPanel photoPanel = new PhotoPanel(photo, cityName);
        photoPanel.setPreferredSize(new Dimension(CARD_W, PHOTO_H));
        photoPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        photoPanel.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { openDetails(cityName); }
        });
        card.add(photoPanel, BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(14, 16, 4, 16));

        JLabel nameLbl = new JLabel(cityName);
        nameLbl.setFont(Theme.heading(18));
        nameLbl.setForeground(Theme.NAVY);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        String loc = (location == null || location.trim().isEmpty()) ? "" : location.trim();
        JLabel locLbl = new JLabel(loc);
        locLbl.setFont(Theme.body(12));
        locLbl.setForeground(Theme.TEXT_MUTED);
        locLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // wraps by itself at the card width (3 lines max), so it never gets cut at the right edge
        ExploreToursPanel.WrapText descLbl = new ExploreToursPanel.WrapText(
                CARD_W - 46, Theme.body(12), new Color(70, 82, 100), 3);
        descLbl.plain(shortDescription(cityName, description));
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(nameLbl);
        info.add(Box.createRigidArea(new Dimension(0, 2)));
        info.add(locLbl);
        info.add(Box.createRigidArea(new Dimension(0, 8)));
        info.add(descLbl);
        card.add(info, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        RoundedButton detailsBtn = new RoundedButton("View Details", Theme.ACCENT_BLUE, Color.WHITE);
        detailsBtn.addActionListener(e -> openDetails(cityName));
        btnRow.add(detailsBtn);
        card.add(btnRow, BorderLayout.SOUTH);

        return card;
    }

    // =====================================================================
    //  Layout + painted components
    // =====================================================================

    /** Page column that always matches the window width (so the cards wrap instead of scrolling sideways). */
    private static class ScrollableColumn extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
            return o == SwingConstants.VERTICAL ? Math.max(40, r.height - 40) : Math.max(40, r.width - 40);
        }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    /** FlowLayout that wraps onto new rows when the available width is too small. */
    private static class WrapLayout extends FlowLayout {
        WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }

        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension min = layoutSize(target, false);
            min.width -= getHgap() + 1;
            return min;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                Container container = target;
                while (container.getSize().width == 0 && container.getParent() != null) {
                    container = container.getParent();
                }
                int targetWidth = container.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap(), vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;

                for (int i = 0; i < target.getComponentCount(); i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                if (SwingUtilities.getAncestorOfClass(JScrollPane.class, target) != null && target.isValid()) {
                    dim.width -= (hgap + 1);
                }
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }

    /** White rounded card with a soft shadow that lifts slightly on hover. */
    private static class CardPanel extends JPanel {
        private boolean hover = false;

        CardPanel() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint((Component) e.getSource(), e.getPoint(), CardPanel.this);
                    if (!contains(p)) { hover = false; repaint(); }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(15, 45, 85, hover ? 26 : 14));
            g2.fillRoundRect(2, 5, w - 5, h - 6, 24, 24);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - 3, h - 6, 24, 24);
            g2.setColor(hover ? new Color(150, 185, 235) : new Color(222, 229, 242));
            g2.drawRoundRect(0, 0, w - 4, h - 7, 24, 24);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Photo at the top of a card (rounded top corners). Shows a gradient with the name if no photo exists. */
    private static class PhotoPanel extends JPanel {
        private final Image photo;
        private final String name;

        PhotoPanel(Image photo, String name) {
            this.photo = photo;
            this.name = name;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int w = getWidth() - 3, h = getHeight();

            // round only the top corners: clip a tall rounded rect, the bottom is cut off by the panel
            g2.setClip(new RoundRectangle2D.Float(1, 1, w - 2, h + 30, 24, 24));
            g2.clipRect(0, 0, w, h);

            if (photo != null) {
                int iw = photo.getWidth(null), ih = photo.getHeight(null);
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) (iw * scale), sh = (int) (ih * scale);
                g2.drawImage(photo, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(40, 100, 200), w, h, new Color(12, 40, 95)));
                g2.fillRect(0, 0, w, h);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
                g2.setColor(new Color(255, 255, 255, 190));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(name, (w - fm.stringWidth(name)) / 2, h / 2 + fm.getAscent() / 3);
            }
            g2.dispose();
        }
    }
}