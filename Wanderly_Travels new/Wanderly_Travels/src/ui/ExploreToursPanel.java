package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExploreToursPanel extends JPanel {

    private static final Color NAVY   = new Color(15, 45, 85);
    private static final Color ORANGE = new Color(228, 105, 15);
    private static final Color BORDER = new Color(222, 229, 242);
    private static final String EMOJI_FONT = "Segoe UI Emoji";
    private static final int CONTENT_W = 780;
    private static final int MAX_GALLERY = 3;   // photos shown per destination

    private JPanel leftCityPanel;
    private JPanel rightDetailsPanel;
    private JScrollPane rightScroll;
    private final List<CityButton> cityButtons = new ArrayList<>();

    public ExploreToursPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(Color.WHITE);

        // ---------- Left sidebar ----------
        leftCityPanel = new JPanel();
        leftCityPanel.setLayout(new BoxLayout(leftCityPanel, BoxLayout.Y_AXIS));
        leftCityPanel.setBackground(new Color(245, 248, 253));
        leftCityPanel.setBorder(BorderFactory.createEmptyBorder(20, 12, 15, 12));

        JLabel sideTitle = new JLabel("EXPLORE DESTINATIONS");
        sideTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sideTitle.setForeground(new Color(120, 135, 165));
        sideTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftCityPanel.add(sideTitle);
        leftCityPanel.add(Box.createRigidArea(new Dimension(0, 6)));

        JScrollPane leftScroll = new JScrollPane(leftCityPanel);
        leftScroll.setPreferredSize(new Dimension(250, 600));
        leftScroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 226, 238)));
        leftScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(leftScroll, BorderLayout.WEST);

        // ---------- Right content ----------
        rightDetailsPanel = new JPanel();
        rightDetailsPanel.setLayout(new BoxLayout(rightDetailsPanel, BoxLayout.Y_AXIS));
        rightDetailsPanel.setBackground(Color.WHITE);
        rightDetailsPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 30, 30));

        rightScroll = new JScrollPane(rightDetailsPanel);
        rightScroll.setBorder(null);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(rightScroll, BorderLayout.CENTER);

        ensureDefaultPackages();
        loadDestinations();

        // When the Home page asked for a particular destination, open it as soon as this page is shown
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                applyPendingCity();
            }
        });
        lastInstance = this;
        applyPendingCity();
    }

    // =====================================================================
    //  Open a particular destination from outside (Home page "View Details")
    //      ExploreToursPanel.requestCity("Jaipur");
    //      frame.navigateTo(MainFrame.DESTINATIONS);
    // =====================================================================
    private static String pendingCity = null;
    private static ExploreToursPanel lastInstance = null;
    private final Map<String, Runnable> cityOpeners = new HashMap<>();
    private final Map<String, CityButton> cityButtonByKey = new HashMap<>();

    public static void requestCity(String cityName) {
        pendingCity = cityName;
        if (lastInstance != null && lastInstance.isShowing()) {
            lastInstance.applyPendingCity();
        }
    }

    private void applyPendingCity() {
        if (pendingCity == null) return;
        String key = compact(pendingCity);
        Runnable open = cityOpeners.get(key);
        if (open == null) return;            // destination not loaded (yet) - keep the request
        pendingCity = null;
        open.run();

        final CityButton btn = cityButtonByKey.get(key);
        if (btn != null) {
            SwingUtilities.invokeLater(() -> btn.scrollRectToVisible(
                    new Rectangle(0, -40, btn.getWidth(), btn.getHeight() + 80)));
        }
    }

    /** First photo of a destination (database image_path or images/<city>.jpg ...), or null. Used by the Home page. */
    public static Image coverPhoto(String cityName, String imgPath) {
        refreshImageIndex();
        List<Image> list = loadGallery(cityName, imgPath);
        return list.isEmpty() ? null : list.get(0);
    }

    // =====================================================================
    //  Database helpers
    // =====================================================================
    private Connection getConnection() throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String url = "jdbc:mysql://localhost:3306/wanderly_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, "root", "Root@1234");
    }

    private boolean hasColumn(Connection conn, String table, String column) {
        try (ResultSet rs = conn.getMetaData().getColumns(conn.getCatalog(), null, table, column)) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * For every destination that has no packages yet, inserts two default
     * packages. Safe to run on every start: destinations that already have
     * packages are left untouched. Edit prices/durations later in the database.
     */
    private void ensureDefaultPackages() {
        try (Connection conn = getConnection()) {

            String linkCol = null;
            boolean byId = false;
            if (hasColumn(conn, "tour_packages", "destination_id")) {
                linkCol = "destination_id"; byId = true;
            } else if (hasColumn(conn, "tour_packages", "destination_name")) {
                linkCol = "destination_name";
            } else if (hasColumn(conn, "tour_packages", "destination")) {
                linkCol = "destination";
            } else if (hasColumn(conn, "tour_packages", "city")) {
                linkCol = "city";
            }
            if (linkCol == null) return;

            String idCol = hasColumn(conn, "destinations", "id") ? "id"
                         : hasColumn(conn, "destinations", "destination_id") ? "destination_id" : null;
            if (byId && idCol == null) return;

            // Collect destinations first (so no ResultSet stays open while inserting)
            List<Object[]> destinations = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM destinations")) {
                while (rs.next()) {
                    String name = rs.getString("name").trim();
                    Object key = byId ? (Object) rs.getInt(idCol) : name;
                    destinations.add(new Object[]{name, key});
                }
            }

            String countSql = byId
                    ? "SELECT COUNT(*) FROM tour_packages WHERE " + linkCol + " = ?"
                    : "SELECT COUNT(*) FROM tour_packages WHERE LOWER(TRIM(" + linkCol + ")) = LOWER(?)";
            String insertSql = "INSERT INTO tour_packages (" + linkCol
                    + ", package_name, duration, price) VALUES (?, ?, ?, ?)";

            for (Object[] d : destinations) {
                String name = (String) d[0];
                Object key = d[1];

                int count = 0;
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setObject(1, key);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) count = rs.getInt(1);
                    }
                }
                if (count > 0) continue;

                String[][] defaults = {
                    { name + " Heritage Tour",    "2 Days / 1 Night",  "8500"  },
                    { name + " Royal Experience", "3 Days / 2 Nights", "14500" }
                };
                for (String[] p : defaults) {
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setObject(1, key);
                        ps.setString(2, p[0]);
                        ps.setString(3, p[1]);
                        ps.setDouble(4, Double.parseDouble(p[2]));
                        ps.executeUpdate();
                    }
                }
            }
        } catch (Exception ex) {
            // If tour_packages has extra required columns, the reason shows up here
            ex.printStackTrace();
        }
    }

    private void loadDestinations() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM destinations")) {

            String idCol = hasColumn(conn, "destinations", "id") ? "id"
                         : hasColumn(conn, "destinations", "destination_id") ? "destination_id" : null;

            boolean first = true;
            while (rs.next()) {
                final int destId = (idCol != null) ? rs.getInt(idCol) : -1;
                final String cityName = rs.getString("name");
                final String imgPath = rs.getString("image_path");
                final String location = rs.getString("location");
                final String desc = rs.getString("description");

                CityButton cityBtn = new CityButton(cityName);
                final Runnable openThis = () -> {
                    selectCity(cityBtn);
                    showDetails(destId, cityName, imgPath, location, desc);
                };
                cityBtn.addActionListener(e -> openThis.run());
                cityButtons.add(cityBtn);
                cityOpeners.put(compact(cityName), openThis);
                cityButtonByKey.put(compact(cityName), cityBtn);

                leftCityPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                leftCityPanel.add(cityBtn);

                if (first) {
                    selectCity(cityBtn);
                    showDetails(destId, cityName, imgPath, location, desc);
                    first = false;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        leftCityPanel.revalidate();
        leftCityPanel.repaint();
    }

    private void selectCity(CityButton chosen) {
        for (CityButton b : cityButtons) b.setSelected(b == chosen);
    }

    // =====================================================================
    //  Image helpers
    // =====================================================================
    // ---------------------------------------------------------------------
    //  Tolerant photo search: ignores UPPER/lower case, spaces, "-" and "_",
    //  and also fixes names like hawa_mahal.jpg.jpg
    //  images/Jaipur/Hawa Mahal.jpg  ==  images/jaipur/hawa_mahal.jpg
    // ---------------------------------------------------------------------
    private static final Map<String, File> imageIndex = new HashMap<>();
    private static boolean imageDiagnosticsPrinted = false;

    private static String compact(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static String fileKey(File f) {
        try { return f.getCanonicalPath().toLowerCase(); }
        catch (Exception e) { return f.getAbsolutePath().toLowerCase(); }
    }

    /** Scans the image folders and remembers every photo. Prints what it found in the console. */
    private static void refreshImageIndex() {
        imageIndex.clear();
        int total = 0;
        StringBuilder report = new StringBuilder();
        for (String r : new String[]{"images", "src/images", "src/ui/images", "assets", "img"}) {
            File root = new File(r);
            report.append("\n    ").append(root.getAbsolutePath())
                  .append(root.isDirectory() ? "   (found)" : "   (not found)");
            if (root.isDirectory()) total += indexFolder(root, root);
        }
        if (!imageDiagnosticsPrinted) {          // print the folder report only once per run
            System.out.println("[Wanderly] Program runs from: " + System.getProperty("user.dir"));
            System.out.println("[Wanderly] Photo folders checked:" + report);
            System.out.println("[Wanderly] Photos found: " + total);
            imageDiagnosticsPrinted = true;
        }
    }

    private static int indexFolder(File root, File dir) {
        File[] files = dir.listFiles();
        if (files == null) return 0;
        int count = 0;
        for (File f : files) {
            if (f.isDirectory()) {
                count += indexFolder(root, f);
                continue;
            }
            String lower = f.getName().toLowerCase();
            if (lower.matches(".+\\.(webp|avif|heic|tiff?|svg)")) {
                System.out.println("[Wanderly] Skipped (Java cannot show this type, save as JPG or PNG): "
                        + f.getAbsolutePath());
                continue;
            }
            if (!lower.matches(".+\\.(jpg|jpeg|jfif|png|gif|bmp)")) continue;

            String base = f.getName().replaceAll("(?i)(\\.(jpg|jpeg|jfif|png|gif|bmp))+$", "");
            // files directly in images/ -> "hawamahal";  images/Jaipur/x.jpg -> "jaipurx"
            String key = dir.equals(root) ? compact(base) : compact(dir.getName() + base);
            if (!imageIndex.containsKey(key)) {
                imageIndex.put(key, f);
                count++;
            }
        }
        return count;
    }

    private static Image readImage(File f) {
        ImageIcon icon = new ImageIcon(f.getAbsolutePath());
        if (icon.getIconWidth() > 0) return icon.getImage();
        try {
            java.awt.image.BufferedImage bi = ImageIO.read(f);
            if (bi != null) return bi;
        } catch (Exception ignored) { }
        System.out.println("[Wanderly] Could not read this photo (damaged or unsupported colours): "
                + f.getAbsolutePath());
        return null;
    }

    private static Map.Entry<String, Image> fromIndex(String compactKey) {
        File f = imageIndex.get(compactKey);
        if (f == null) return null;
        Image img = readImage(f);
        if (img == null) return null;
        return new AbstractMap.SimpleEntry<>(fileKey(f), img);
    }

    /** "Mount Abu" -> "mount_abu" (used for image file names). */
    private static String slug(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Finds one image on disk / project folders / classpath. Returns {uniqueKey, Image} or null. */
    private static Map.Entry<String, Image> locateImage(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        path = path.trim();

        String[] candidates = { path, "images/" + path, "src/images/" + path, "src/" + path };
        for (String c : candidates) {
            File f = new File(c);
            if (f.exists() && f.isFile()) {
                ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                if (icon.getIconWidth() > 0) {
                    String key;
                    try { key = f.getCanonicalPath(); } catch (Exception e) { key = f.getAbsolutePath(); }
                    return new AbstractMap.SimpleEntry<>(key.toLowerCase(), icon.getImage());
                }
            }
        }
        URL url = ExploreToursPanel.class.getResource("/" + path.replace("\\", "/"));
        if (url != null) {
            ImageIcon icon = new ImageIcon(url);
            if (icon.getIconWidth() > 0) {
                return new AbstractMap.SimpleEntry<>(url.toString().toLowerCase(), icon.getImage());
            }
        }
        return null;
    }

    /**
     * Collects up to MAX_GALLERY photos for a destination:
     *  1) every path in the database image_path (several can be separated by , ; or |)
     *  2) automatically: images/<city>.jpg, <city>_2.jpg, <city>_3.jpg (also .png / .jpeg)
     */
    private static List<Image> loadGallery(String cityName, String imgPath) {
        List<String> candidates = new ArrayList<>();
        if (imgPath != null) {
            for (String p : imgPath.split("[,;|]")) {
                if (!p.trim().isEmpty()) candidates.add(p.trim());
            }
        }
        String s = slug(cityName);
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            candidates.add(s + ext);
            candidates.add(s + "_1" + ext);
            candidates.add(s + "_2" + ext);
            candidates.add(s + "_3" + ext);
        }

        Map<String, Image> found = new LinkedHashMap<>();   // key = real file, so no duplicates
        for (String c : candidates) {
            if (found.size() >= MAX_GALLERY) break;
            Map.Entry<String, Image> hit = locateImage(c);
            if (hit != null && !found.containsKey(hit.getKey())) {
                found.put(hit.getKey(), hit.getValue());
            }
        }

        // tolerant search: jaipur, Jaipur 2, JAIPUR_3 ... in any of the image folders
        String base = compact(cityName);
        for (String k : new String[]{base, base + "1", base + "2", base + "3"}) {
            if (found.size() >= MAX_GALLERY) break;
            Map.Entry<String, Image> hit = fromIndex(k);
            if (hit != null && !found.containsKey(hit.getKey())) {
                found.put(hit.getKey(), hit.getValue());
            }
        }
        return new ArrayList<>(found.values());
    }

    // =====================================================================
    //  Details view
    // =====================================================================
    private void showDetails(int destId, String cityName, String imgPath, String location, String description) {
        rightDetailsPanel.removeAll();
        final String searchName = cityName.trim();
        final String locText = (location == null) ? "" : location;

        // ---- Hero banner ----
        refreshImageIndex();
        List<Image> gallery = loadGallery(searchName, imgPath);
        Image photo = gallery.isEmpty() ? null : gallery.get(0);
        String note = (photo == null)
                ? "No photo found - save it as " + new File("images", slug(searchName) + ".jpg").getAbsolutePath()
                : null;
        DestinationInfo info = DestinationInfo.forCity(searchName);
        String heroLocation = "📍 " + locText + (info.tagline.isEmpty() ? "" : "   •   " + info.tagline);
        HeroPanel hero = new HeroPanel(photo, searchName.toUpperCase(), heroLocation, note);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightDetailsPanel.add(hero);
        if (gallery.size() > 1) {
            rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 12)));
            rightDetailsPanel.add(buildGalleryStrip(hero, gallery));
        }
        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 18)));
        rightDetailsPanel.add(buildGlanceRow(info));
        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        // ---- About card ----
        RoundedPanel aboutCard = new RoundedPanel(new BorderLayout(0, 8), 22, new Color(248, 250, 255), BORDER, true);
        aboutCard.setBorder(BorderFactory.createEmptyBorder(16, 22, 22, 22));
        aboutCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel aboutTitle = new JLabel("About " + searchName);
        aboutTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        aboutTitle.setForeground(NAVY);

        boolean hasDesc = description != null && !description.trim().isEmpty();
        WrapText aboutBody = new WrapText(700, new Font("Segoe UI", Font.PLAIN, 15), new Color(70, 82, 100), 0);
        if (hasDesc) {
            aboutBody.bold(description.trim());
        }
        if (!info.overview.isEmpty()) {
            if (hasDesc) aboutBody.paragraphBreak();
            aboutBody.plain(info.overview);
        }

        aboutCard.add(aboutTitle, BorderLayout.NORTH);
        aboutCard.add(aboutBody, BorderLayout.CENTER);
        aboutCard.setMaximumSize(new Dimension(CONTENT_W, aboutCard.getPreferredSize().height));
        rightDetailsPanel.add(aboutCard);
        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 22)));

        if (!info.highlights.isEmpty()) {
            rightDetailsPanel.add(buildHighlightsCard(searchName, info));
            rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 22)));
        }
        if (!info.gettingThere.isEmpty() || !info.tip.isEmpty()) {
            rightDetailsPanel.add(buildGoodToKnowCard(info));
            rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 22)));
        }
        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // ---- Packages heading ----
        JLabel pkgHeading = new JLabel("Available Packages");
        pkgHeading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        pkgHeading.setForeground(NAVY);
        pkgHeading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel pkgSub = new JLabel("Handpicked experiences for " + searchName + " — pick one and hit Book Now");
        pkgSub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pkgSub.setForeground(Color.GRAY);
        pkgSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightDetailsPanel.add(pkgHeading);
        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        rightDetailsPanel.add(pkgSub);
        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        // ---- Packages (query adapts to the columns your table has) ----
        try (Connection conn = getConnection()) {

            String sql;
            Object param;
            if (destId != -1 && hasColumn(conn, "tour_packages", "destination_id")) {
                sql = "SELECT * FROM tour_packages WHERE destination_id = ?";
                param = destId;
            } else if (hasColumn(conn, "tour_packages", "destination_name")) {
                sql = "SELECT * FROM tour_packages WHERE LOWER(TRIM(destination_name)) = LOWER(?)";
                param = searchName;
            } else if (hasColumn(conn, "tour_packages", "destination")) {
                sql = "SELECT * FROM tour_packages WHERE LOWER(TRIM(destination)) = LOWER(?)";
                param = searchName;
            } else if (hasColumn(conn, "tour_packages", "city")) {
                sql = "SELECT * FROM tour_packages WHERE LOWER(TRIM(city)) = LOWER(?)";
                param = searchName;
            } else {
                sql = "SELECT * FROM tour_packages WHERE LOWER(package_name) LIKE ?";
                param = "%" + searchName.toLowerCase() + "%";
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setObject(1, param);

                try (ResultSet rs = pstmt.executeQuery()) {
                    boolean hasPackages = false;
                    while (rs.next()) {
                        hasPackages = true;
                        rightDetailsPanel.add(buildPackageCard(
                                searchName, locText,
                                rs.getString("package_name"),
                                rs.getString("duration"),
                                rs.getDouble("price")));
                        rightDetailsPanel.add(Box.createRigidArea(new Dimension(0, 16)));
                    }

                    if (!hasPackages) {
                        RoundedPanel empty = new RoundedPanel(new BorderLayout(), 22, new Color(250, 251, 254), BORDER, false);
                        empty.setBorder(BorderFactory.createEmptyBorder(24, 24, 28, 24));
                        empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                        empty.setMaximumSize(new Dimension(CONTENT_W, 90));
                        JLabel noPkg = new JLabel("No packages available for this destination yet.");
                        noPkg.setFont(new Font("Segoe UI", Font.ITALIC, 15));
                        noPkg.setForeground(Color.GRAY);
                        empty.add(noPkg, BorderLayout.CENTER);
                        rightDetailsPanel.add(empty);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JLabel errLabel = new JLabel("Could not load packages: " + ex.getMessage());
            errLabel.setForeground(Color.RED);
            errLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rightDetailsPanel.add(errLabel);
        }

        rightDetailsPanel.revalidate();
        rightDetailsPanel.repaint();
        SwingUtilities.invokeLater(() -> rightScroll.getVerticalScrollBar().setValue(0));
    }

    // ---------------------------------------------------------------------
    //  Destination guide sections
    // ---------------------------------------------------------------------
    /** Row of thumbnails under the hero; clicking one shows it in the big banner. */
    private JComponent buildGalleryStrip(HeroPanel hero, List<Image> gallery) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        final List<ThumbPanel> thumbs = new ArrayList<>();
        for (int i = 0; i < gallery.size(); i++) {
            final Image img = gallery.get(i);
            final ThumbPanel t = new ThumbPanel(img, i == 0);
            thumbs.add(t);
            t.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    hero.setPhoto(img);
                    for (ThumbPanel other : thumbs) other.setSelected(other == t);
                }
            });
            row.add(t);
            row.add(Box.createRigidArea(new Dimension(12, 0)));
        }
        row.setMaximumSize(new Dimension(CONTENT_W, 100));
        return row;
    }

    private static String html(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private JComponent buildGlanceRow(DestinationInfo info) {
        JPanel row = new JPanel(new GridLayout(1, 4, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(infoTile("📅", "BEST TIME", info.bestTime));
        row.add(infoTile("⏳", "IDEAL DURATION", info.idealDuration));
        row.add(infoTile("🎯", "BEST FOR", info.bestFor));
        row.add(infoTile("🍴", "LOCAL FLAVOURS", info.localFlavours));
        row.setMaximumSize(new Dimension(CONTENT_W, row.getPreferredSize().height));
        return row;
    }

    private JComponent infoTile(String icon, String caption, String value) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(0, 6), 18, new Color(248, 250, 255), BORDER, true);
        tile.setBorder(BorderFactory.createEmptyBorder(12, 16, 18, 12));

        WrapText cap = new WrapText(150, new Font("Segoe UI", Font.BOLD, 11), new Color(120, 135, 165), 1);
        cap.emoji(icon).plain(caption);

        WrapText val = new WrapText(140, new Font("Segoe UI", Font.BOLD, 14), NAVY, 0);
        val.plain(value);

        tile.add(cap, BorderLayout.NORTH);
        tile.add(val, BorderLayout.CENTER);
        return tile;
    }

    private JComponent buildHighlightsCard(String city, DestinationInfo info) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 12), 22, Color.WHITE, BORDER, true);
        card.setBorder(BorderFactory.createEmptyBorder(16, 22, 24, 22));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Top Attractions in " + city);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(NAVY);

        JPanel grid = new JPanel(new GridLayout(0, 3, 24, 12));
        grid.setOpaque(false);
        for (String h : info.highlights) {
            JPanel item = new JPanel(new BorderLayout(10, 0));
            item.setOpaque(false);

            JLabel dot = new JLabel("\u25CF");
            dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            dot.setForeground(ORANGE);

            JLabel name = new JLabel(h);
            name.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            name.setForeground(new Color(45, 58, 80));

            item.add(dot, BorderLayout.WEST);
            item.add(name, BorderLayout.CENTER);
            grid.add(item);
        }

        card.add(title, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(CONTENT_W, card.getPreferredSize().height));
        return card;
    }

    private JComponent buildGoodToKnowCard(DestinationInfo info) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), 22,
                new Color(255, 250, 240), new Color(246, 222, 180), true);
        card.setBorder(BorderFactory.createEmptyBorder(16, 22, 24, 22));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("Good to Know");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(150, 80, 10));

        JPanel lines = new JPanel();
        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setOpaque(false);
        if (!info.gettingThere.isEmpty()) {
            lines.add(infoLine("✈", "Getting there: ", info.gettingThere));
        }
        if (!info.tip.isEmpty()) {
            if (!info.gettingThere.isEmpty()) lines.add(Box.createRigidArea(new Dimension(0, 8)));
            lines.add(infoLine("💡", "Insider tip: ", info.tip));
        }

        card.add(title, BorderLayout.NORTH);
        card.add(lines, BorderLayout.CENTER);
        card.setMaximumSize(new Dimension(CONTENT_W, card.getPreferredSize().height));
        return card;
    }

    private JComponent infoLine(String icon, String label, String text) {
        WrapText l = new WrapText(700, new Font("Segoe UI", Font.PLAIN, 14), new Color(80, 70, 55), 0);
        l.emoji(icon).bold(label.trim()).plain(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JComponent buildPackageCard(String destination, String location,
                                        String packageName, String duration, double price) {

        RoundedPanel card = new RoundedPanel(new BorderLayout(20, 0), 22, Color.WHITE, BORDER, true);
        card.setBorder(BorderFactory.createEmptyBorder(18, 24, 24, 24));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(CONTENT_W, 140));

        // Left: name + chips
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel pName = new JLabel(packageName);
        pName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        pName.setForeground(new Color(20, 30, 50));
        pName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chipRow.setOpaque(false);
        chipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        chipRow.add(makeChip("⏳ " + duration));
        chipRow.add(Box.createRigidArea(new Dimension(8, 0)));
        chipRow.add(makeChip("📍 " + destination));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(pName);
        textPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        textPanel.add(chipRow);
        textPanel.add(Box.createVerticalGlue());

        // Right: price + button
        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        pricePanel.setOpaque(false);

        JLabel priceLbl = new JLabel(String.format("Rs. %,.0f", price));
        priceLbl.setFont(new Font("Segoe UI", Font.BOLD, 26));
        priceLbl.setForeground(ORANGE);
        priceLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel perPerson = new JLabel("per person");
        perPerson.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        perPerson.setForeground(Color.GRAY);
        perPerson.setAlignmentX(Component.CENTER_ALIGNMENT);

        BookButton bookBtn = new BookButton("Book Now  →");
        bookBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Remember exactly what was selected, then go to the booking page
        bookBtn.addActionListener(e -> {
            BookingSelection.set(destination, location, packageName, duration, price);

            Container comp = bookBtn.getParent();
            while (comp != null && !(comp instanceof MainFrame)) {
                comp = comp.getParent();
            }
            if (comp instanceof MainFrame) {
                ((MainFrame) comp).navigateTo(MainFrame.BOOKING);
            }
        });

        pricePanel.add(Box.createVerticalGlue());
        pricePanel.add(priceLbl);
        pricePanel.add(perPerson);
        pricePanel.add(Box.createRigidArea(new Dimension(0, 10)));
        pricePanel.add(bookBtn);
        pricePanel.add(Box.createVerticalGlue());

        card.add(textPanel, BorderLayout.CENTER);
        card.add(pricePanel, BorderLayout.EAST);
        return card;
    }

    private JComponent makeChip(String text) {
        RoundedPanel chip = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 12, 4), 20,
                new Color(235, 243, 255), new Color(205, 222, 250), false);
        JLabel l = new JLabel(text);
        l.setFont(new Font(EMOJI_FONT, Font.PLAIN, 13));
        l.setForeground(new Color(30, 80, 160));
        chip.add(l);
        return chip;
    }

    // =====================================================================
    //  Custom-painted components
    // =====================================================================

    // =====================================================================
    //  Travel-guide details per destination (best time, duration, food...)
    //  To add a city, add one more add(...) call in the static block.
    // =====================================================================
    private static final class DestinationInfo {

        final String tagline;
        final String overview;
        final String bestTime;
        final String idealDuration;
        final String bestFor;
        final String localFlavours;
        final String gettingThere;
        final String tip;
        final List<String> highlights;

        private DestinationInfo(String tagline, String overview, String bestTime, String idealDuration,
                                String bestFor, String localFlavours, String gettingThere, String tip,
                                List<String> highlights) {
            this.tagline = tagline;
            this.overview = overview;
            this.bestTime = bestTime;
            this.idealDuration = idealDuration;
            this.bestFor = bestFor;
            this.localFlavours = localFlavours;
            this.gettingThere = gettingThere;
            this.tip = tip;
            this.highlights = highlights;
        }

        private static final Map<String, DestinationInfo> MAP = new HashMap<>();

        private static void add(String key, String tagline, String overview, String bestTime, String idealDuration,
                                String bestFor, String localFlavours, String gettingThere, String tip,
                                String... highlights) {
            MAP.put(key, new DestinationInfo(tagline, overview, bestTime, idealDuration, bestFor,
                    localFlavours, gettingThere, tip, Arrays.asList(highlights)));
        }

        static {
            add("jaipur", "The Pink City",
                "Capital of Rajasthan and the first stop on the famous Golden Triangle, Jaipur blends royal palaces, "
              + "hilltop forts and buzzing bazaars. Amer Fort and Jantar Mantar are UNESCO World Heritage Sites, and the "
              + "rose-pink old city is best explored on foot. Shop for blue pottery, jewellery and block-printed textiles, "
              + "then end the day over a traditional Rajasthani thali.",
                "Oct - Mar", "2 - 3 days", "Heritage, shopping, families",
                "Dal Baati Churma, Pyaaz Kachori, Ghewar",
                "Jaipur International Airport is close to the city, and Jaipur Junction is a major railway hub on the Delhi route.",
                "Start at Amer Fort early in the morning to beat the crowds; a composite ticket covers several major monuments.",
                "Amer Fort", "City Palace", "Hawa Mahal", "Jantar Mantar", "Nahargarh Fort", "Johari Bazaar");

            add("udaipur", "The City of Lakes",
                "Set around shimmering lakes and ringed by the Aravalli hills, Udaipur is Rajasthan's most romantic city. "
              + "Palaces seem to float on Lake Pichola, while the old town's lanes are lined with miniature-painting studios, "
              + "rooftop cafes and craft shops. A sunset boat ride on the lake is the signature experience.",
                "Oct - Mar", "2 - 3 days", "Couples, honeymoon, luxury",
                "Dal Baati Churma, Gatte ki Sabzi, Malpua",
                "Maharana Pratap Airport (Dabok) is about 22 km from the city; Udaipur City station connects by train.",
                "Book a lakeside sunset boat ride or rooftop dinner in advance; evenings are the most beautiful time here.",
                "City Palace", "Lake Pichola Boat Ride", "Jag Mandir", "Saheliyon-ki-Bari",
                "Sajjangarh (Monsoon Palace)", "Bagore Ki Haveli");

            add("jodhpur", "The Blue City",
                "Dominated by the mighty Mehrangarh Fort, Jodhpur is the Blue City, where indigo-painted houses spread "
              + "below the ramparts. The old city around the Clock Tower is a maze of spice stalls, textile shops and street "
              + "food. Try the zip-line along the fort walls, or watch the sun set over the blue rooftops.",
                "Oct - Mar", "2 days", "Heritage, photography, food",
                "Mirchi Vada, Mawa Kachori, Makhaniya Lassi",
                "Jodhpur has its own airport and Jodhpur Junction railway station, both well connected to Delhi and Jaipur.",
                "Visit Mehrangarh in the morning and end the day with sunset views from the fort or a rooftop cafe.",
                "Mehrangarh Fort", "Jaswant Thada", "Umaid Bhawan Palace", "Clock Tower & Sardar Market",
                "Mandore Gardens", "Blue City Walk");

            add("jaisalmer", "The Golden City",
                "Rising from the Thar Desert like a sandcastle, Jaisalmer's golden sandstone fort is one of the few "
              + "living forts in India, with homes, shops and temples still inside its walls. Beyond the city, the Sam Sand "
              + "Dunes offer camel safaris, folk music and a night under a sky full of stars.",
                "Nov - Feb", "2 - 3 days", "Adventure, desert camping, photography",
                "Ker Sangri, Bajra Roti, Ghotua Ladoo",
                "Jaisalmer has its own railway station; Jodhpur (about 285 km away) is the main airport for the region.",
                "Plan one night at a desert camp near the Sam dunes, and carry a warm jacket because winter nights get cold.",
                "Jaisalmer Fort", "Sam Sand Dunes", "Patwon Ki Haveli", "Gadisar Lake", "Bada Bagh", "Kuldhara Village");

            add("pushkar", "The Sacred Lake Town",
                "One of Hinduism's most sacred towns, Pushkar surrounds a serene lake lined with dozens of ghats and "
              + "temples. The town is vegetarian and relaxed, with colourful markets and rooftop cafes. It comes alive in "
              + "November during the famous Pushkar Camel Fair.",
                "Oct - Mar", "1 - 2 days", "Spiritual, backpackers, culture",
                "Malpua, Rabri, Sattvic thali",
                "Ajmer Junction (about 15 km) is the nearest major railway station; Jaipur airport is around 150 km away.",
                "Dress modestly near the ghats and temples, and remove footwear before entering sacred spaces.",
                "Brahma Temple", "Pushkar Lake & Ghats", "Savitri Mata Temple", "Rangji Temple",
                "Camel Safari", "Pushkar Camel Fair (Nov)");

            add("mount abu", "Rajasthan's Hill Station",
                "Rajasthan's only hill station offers cool air, green forests and a welcome break from the desert heat. "
              + "Its star attraction is the Dilwara Jain Temples, famed for exquisitely carved white marble. Take a boat on "
              + "Nakki Lake and catch the sunset from Sunset Point.",
                "Sep - Mar", "2 days", "Nature, relaxation, families",
                "Rajasthani & Gujarati thalis, Masala chai",
                "Abu Road (about 28 km) is the nearest railway station; Udaipur airport is roughly 165 km away.",
                "Weekends and holidays get crowded, so plan midweek, and check the visiting hours of the Dilwara Temples.",
                "Dilwara Jain Temples", "Nakki Lake", "Sunset Point", "Guru Shikhar", "Achalgarh Fort",
                "Mount Abu Wildlife Sanctuary");

            add("bikaner", "The Camel Country",
                "Bikaner is a desert city of red sandstone havelis and Junagarh, a grand fort built on flat ground "
              + "instead of a hilltop. It is famous for the Karni Mata Temple at Deshnok, home to thousands of holy rats, "
              + "and for its spicy snacks, especially Bikaneri bhujia.",
                "Oct - Mar", "1 - 2 days", "Culture, food, offbeat travel",
                "Bikaneri Bhujia, Rasgulla, Papad",
                "Bikaner Junction is well connected by train to Delhi and Jaipur; the local airport has limited flights.",
                "Try fresh bhujia and sweets in the old city, and visit Karni Mata Temple in the morning.",
                "Junagarh Fort", "Karni Mata Temple", "Lalgarh Palace", "National Research Centre on Camel",
                "Gajner Palace & Sanctuary", "Bhandasar Jain Temple");

            add("ranthambore", "Tiger Territory",
                "Once a royal hunting ground of the Jaipur maharajas, Ranthambore National Park is among the best places "
              + "in India to spot tigers in the wild. Jeep and canter safaris wind past lakes, ruins and the UNESCO-listed "
              + "Ranthambore Fort.",
                "Nov - Apr", "2 - 3 days", "Wildlife, photography, families",
                "Rajasthani thali, Kachori, Dal Baati Churma",
                "Sawai Madhopur railway station is about 11 km away; Jaipur airport is roughly 180 km away.",
                "Book your safari zone and slot well in advance, and remember the park closes during the monsoon months.",
                "Tiger Safari (Jeep / Canter)", "Ranthambore Fort", "Padam Talao", "Jogi Mahal",
                "Trinetra Ganesh Temple", "Surwal Lake");

            add("chittorgarh", "Land of Valour",
                "Home to one of the largest forts in India, a sprawling hilltop citadel and UNESCO World Heritage Site, "
              + "Chittorgarh is a city of valour and legend. Inside the walls stand towering victory pillars, ornate Jain "
              + "and Hindu temples and palaces tied to the stories of Rana Kumbha, Rani Padmini and the poet-saint Meera Bai.",
                "Oct - Mar", "1 - 2 days", "History, culture, heritage lovers",
                "Dal Baati Churma, Kadhi Kachori",
                "Chittorgarh Junction is on the main rail route; Udaipur airport is about 115 km away.",
                "Allow at least half a day for the fort, and wear comfortable shoes and carry water.",
                "Chittorgarh Fort", "Vijay Stambh (Victory Tower)", "Kirti Stambh", "Rani Padmini Palace",
                "Meera Bai Temple", "Kumbha Palace");

            add("agra", "Home of the Taj Mahal",
                "Agra is home to the Taj Mahal, the white-marble monument of love built by the Mughal emperor Shah Jahan. "
              + "The Taj Mahal, Agra Fort and nearby Fatehpur Sikri are all UNESCO World Heritage Sites, and the delicate "
              + "Itimad-ud-Daulah tomb is often called the baby Taj.",
                "Oct - Mar", "1 - 2 days", "History, romance, photography",
                "Petha, Bedai with Aloo Sabzi, Mughlai cuisine",
                "Agra Cantt railway station is well connected to Delhi; the Yamuna Expressway makes it an easy road trip.",
                "The Taj Mahal is closed on Fridays and looks best at sunrise; buy tickets online to skip the queues.",
                "Taj Mahal", "Agra Fort", "Fatehpur Sikri", "Itimad-ud-Daulah", "Mehtab Bagh", "Akbar's Tomb, Sikandra");

            // Alternate spellings
            MAP.put("chittaurgarh", MAP.get("chittorgarh"));
            MAP.put("chittorgarh fort", MAP.get("chittorgarh"));
            MAP.put("mt. abu", MAP.get("mount abu"));
            MAP.put("mt abu", MAP.get("mount abu"));
            MAP.put("mountabu", MAP.get("mount abu"));
            MAP.put("ranthambhore", MAP.get("ranthambore"));
        }

        private static final DestinationInfo DEFAULT = new DestinationInfo(
                "", "", "Oct - Mar", "2 - 3 days", "Sightseeing & culture",
                "Local Rajasthani cuisine", "", "", new ArrayList<String>());

        static DestinationInfo forCity(String cityName) {
            if (cityName == null) return DEFAULT;
            DestinationInfo info = MAP.get(cityName.trim().toLowerCase());
            return info != null ? info : DEFAULT;
        }
    }

    /**
     * Text that wraps by itself at a fixed width (does not depend on Swing HTML, whose pixel
     * widths change from screen to screen). Supports bold and emoji parts.
     * Also used by HomePanel for the short destination descriptions.
     */
    static class WrapText extends JComponent {
        private static final class Tok {
            final String s; final Font f; final boolean nl;
            Tok(String s, Font f, boolean nl) { this.s = s; this.f = f; this.nl = nl; }
        }

        private final List<Tok> toks = new ArrayList<>();
        private final Font base;
        private final int wrapWidth, maxLines;      // maxLines 0 = no limit
        private List<List<Tok>> lines;
        private int lineH;

        WrapText(int wrapWidth, Font base, Color color, int maxLines) {
            this.wrapWidth = wrapWidth;
            this.base = base;
            this.maxLines = maxLines;
            setFont(base);
            setForeground(color);
            setOpaque(false);
        }

        WrapText add(String text, Font f) {
            for (String w : text.split(" ")) if (!w.isEmpty()) toks.add(new Tok(w, f, false));
            lines = null;
            return this;
        }
        WrapText plain(String t)  { return add(t, base); }
        WrapText bold(String t)   { return add(t, base.deriveFont(Font.BOLD)); }
        WrapText emoji(String t)  { return add(t, new Font(EMOJI_FONT, Font.PLAIN, base.getSize())); }
        WrapText newline()        { toks.add(new Tok("", base, true)); lines = null; return this; }
        WrapText paragraphBreak() { newline(); newline(); return this; }

        private int lineWidth(List<Tok> line) {
            int x = 0;
            for (Tok t : line) {
                FontMetrics fm = getFontMetrics(t.f);
                if (x > 0) x += fm.charWidth(' ');
                x += fm.stringWidth(t.s);
            }
            return x;
        }

        private void layoutLines() {
            if (lines != null) return;
            lines = new ArrayList<>();
            lineH = getFontMetrics(base).getHeight();
            List<Tok> cur = new ArrayList<>();
            int x = 0;
            for (Tok t : toks) {
                if (t.nl) { lines.add(cur); cur = new ArrayList<>(); x = 0; continue; }
                FontMetrics fm = getFontMetrics(t.f);
                int w = fm.stringWidth(t.s);
                int need = (x == 0 ? 0 : fm.charWidth(' ')) + w;
                if (x > 0 && x + need > wrapWidth) {
                    lines.add(cur);
                    cur = new ArrayList<>();
                    x = 0;
                    need = w;
                }
                cur.add(t);
                x += need;
            }
            lines.add(cur);

            if (maxLines > 0 && lines.size() > maxLines) {          // too long: cut and add "..."
                lines = new ArrayList<>(lines.subList(0, maxLines));
                List<Tok> last = new ArrayList<>(lines.get(maxLines - 1));
                int ell = getFontMetrics(base).stringWidth("...");
                while (last.size() > 1 && lineWidth(last) + ell > wrapWidth) last.remove(last.size() - 1);
                Tok lastTok = last.remove(last.size() - 1);
                last.add(new Tok(lastTok.s + "...", lastTok.f, false));
                lines.set(maxLines - 1, last);
            }
        }

        @Override public Dimension getPreferredSize() {
            layoutLines();
            return new Dimension(wrapWidth, lines.size() * lineH + 2);
        }
        @Override public Dimension getMinimumSize() { return getPreferredSize(); }
        @Override public Dimension getMaximumSize() { return getPreferredSize(); }

        @Override
        protected void paintComponent(Graphics g) {
            layoutLines();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(getForeground());
            int ascent = getFontMetrics(base).getAscent();
            int y = 0;
            for (List<Tok> line : lines) {
                int x = 0;
                for (Tok t : line) {
                    FontMetrics fm = getFontMetrics(t.f);
                    if (x > 0) x += fm.charWidth(' ');
                    g2.setFont(t.f);
                    g2.drawString(t.s, x, y + ascent);
                    x += fm.stringWidth(t.s);
                }
                y += lineH;
            }
            g2.dispose();
        }
    }

    /** Rounded card with a soft shadow. */
    private static class RoundedPanel extends JPanel {
        private final int arc;
        private final Color bg, border;
        private final boolean shadow;

        RoundedPanel(LayoutManager lm, int arc, Color bg, Color border, boolean shadow) {
            super(lm);
            this.arc = arc; this.bg = bg; this.border = border; this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int bodyH = shadow ? h - 6 : h - 1;
            if (shadow) {
                g2.setColor(new Color(15, 45, 85, 14));
                g2.fillRoundRect(1, 4, w - 3, bodyH, arc, arc);
                g2.setColor(new Color(15, 45, 85, 10));
                g2.fillRoundRect(2, 5, w - 5, bodyH, arc, arc);
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, w - 3, bodyH, arc, arc);
            g2.setColor(border);
            g2.drawRoundRect(0, 0, w - 4, bodyH - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Big banner: photo (or gradient) + dark fade + title and location. */
    private static class HeroPanel extends JPanel {
        private Image photo;
        private final String title, location, note;

        void setPhoto(Image p) {
            this.photo = p;
            repaint();
        }

        HeroPanel(Image photo, String title, String location, String note) {
            this.photo = photo; this.title = title; this.location = location; this.note = note;
            setOpaque(false);
            setPreferredSize(new Dimension(CONTENT_W, 340));
            setMaximumSize(new Dimension(CONTENT_W, 340));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int w = getWidth(), h = getHeight();

            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 28, 28));

            if (photo != null) {
                int iw = photo.getWidth(null), ih = photo.getHeight(null);
                double scale = Math.max((double) w / iw, (double) h / ih);
                int sw = (int) (iw * scale), sh = (int) (ih * scale);
                g2.drawImage(photo, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
            } else {
                g2.setPaint(new GradientPaint(0, 0, new Color(30, 90, 190), w, h, new Color(10, 35, 85)));
                g2.fillRect(0, 0, w, h);
                if (note != null) {
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g2.setColor(new Color(255, 255, 255, 140));
                    g2.drawString(note, 24, 28);
                }
            }

            // dark fade at the bottom so text is always readable
            g2.setPaint(new GradientPaint(0, h * 0.35f, new Color(0, 0, 0, 0), 0, h, new Color(0, 0, 0, 195)));
            g2.fillRect(0, 0, w, h);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
            g2.drawString(title, 30, h - 62);

            g2.setColor(new Color(228, 235, 248));
            g2.setFont(new Font(EMOJI_FONT, Font.PLAIN, 17));
            g2.drawString(location, 30, h - 30);

            g2.dispose();
        }
    }

    /** Small clickable photo preview with a highlighted border when selected. */
    private static class ThumbPanel extends JPanel {
        private final Image img;
        private boolean selected;
        private boolean hover = false;

        ThumbPanel(Image img, boolean selected) {
            this.img = img;
            this.selected = selected;
            setOpaque(false);
            Dimension d = new Dimension(150, 96);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        void setSelected(boolean s) {
            this.selected = s;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            int w = getWidth(), h = getHeight();

            g2.setClip(new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18));
            int iw = img.getWidth(null), ih = img.getHeight(null);
            double scale = Math.max((double) (w - 4) / iw, (double) (h - 4) / ih);
            int sw = (int) (iw * scale), sh = (int) (ih * scale);
            g2.drawImage(img, (w - sw) / 2, (h - sh) / 2, sw, sh, null);

            if (!selected && !hover) {                    // dim the photos that are not selected
                g2.setColor(new Color(0, 0, 0, 70));
                g2.fillRect(0, 0, w, h);
            }
            g2.setClip(null);

            if (selected) {
                g2.setColor(ORANGE);
                g2.setStroke(new BasicStroke(3f));
            } else {
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(2, 2, w - 5, h - 5, 18, 18);
            g2.dispose();
        }
    }

    /** Sidebar destination button with hover + selected state. */
    private static class CityButton extends JButton {
        private boolean hover = false;

        CityButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setMaximumSize(new Dimension(225, 46));
            setPreferredSize(new Dimension(225, 46));
            setAlignmentX(Component.CENTER_ALIGNMENT);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (isSelected()) {
                g2.setPaint(new GradientPaint(0, 0, new Color(28, 95, 195), w, h, new Color(12, 52, 120)));
                g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
            } else {
                g2.setColor(hover ? new Color(232, 240, 252) : Color.WHITE);
                g2.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
            }

            g2.setFont(getFont());
            g2.setColor(isSelected() ? Color.WHITE : NAVY);
            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(getText())) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }

    /** Gradient "Book Now" button with hover glow. */
    private static class BookButton extends JButton {
        private boolean hover = false;

        BookButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            Dimension d = new Dimension(150, 44);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e)  { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            Color top = hover ? new Color(55, 150, 250) : new Color(35, 125, 235);
            Color bottom = hover ? new Color(25, 105, 215) : new Color(14, 82, 190);
            g2.setPaint(new GradientPaint(0, 0, top, 0, h, bottom));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 24, 24);

            g2.setFont(getFont());
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            int x = (w - fm.stringWidth(getText())) / 2;
            int y = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
    }
}