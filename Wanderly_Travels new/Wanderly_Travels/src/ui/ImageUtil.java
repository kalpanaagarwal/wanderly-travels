package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Loads images from the local src/images folder and caches them, so every
 * destination/package card can request an image by relative path (for
 * example "images/jaipur.jpg") without worrying about broken paths or
 * repeated disk reads. If a file is ever missing, a clean placeholder
 * graphic is drawn instead of a broken-image icon.
 */
public final class ImageUtil {

    private static final Map<String, BufferedImage> CACHE = new HashMap<>();

    private ImageUtil() {
    }

    private static BufferedImage load(String relativePath) {
        return CACHE.computeIfAbsent(relativePath, p -> {
            try {
                // Works whether the app is run from the "src" folder (VS Code / javac)
                // or from a packaged jar with the images folder alongside it.
                File file = new File(p);
                if (file.exists()) {
                    return ImageIO.read(file);
                }
                var stream = ImageUtil.class.getClassLoader().getResourceAsStream(p);
                if (stream != null) {
                    return ImageIO.read(stream);
                }
            } catch (IOException ignored) {
                // fall through to placeholder
            }
            return null;
        });
    }

    /** Returns a scaled ImageIcon for the given relative image path, or a placeholder if missing. */
    public static ImageIcon getScaledIcon(String relativePath, int width, int height) {
        BufferedImage img = load(relativePath);
        if (img == null) {
            return new ImageIcon(placeholder(width, height));
        }
        Image scaled = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static BufferedImage placeholder(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, Theme.SKY_LIGHT, 0, height, Theme.ACCENT_BLUE));
        g2.fillRect(0, 0, width, height);
        g2.setColor(Color.WHITE);
        g2.setFont(Theme.body(12));
        g2.drawString("Wanderly Travels", 10, height / 2);
        g2.dispose();
        return img;
    }
}
