package ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

/**
 * A single place that defines the look of the whole application:
 * colours, fonts and a couple of small drawing helpers. Every panel in the
 * "ui" package pulls its colours from here so the app looks consistent
 * from screen to screen.
 */
public final class Theme {

    private Theme() {
    }

    // ---- Core blue palette ----
    public static final Color NAVY        = new Color(0x0B2E4F);
    public static final Color PRIMARY     = new Color(0x1565C0);
    public static final Color PRIMARY_DARK = new Color(0x0D47A1);
    public static final Color ACCENT_BLUE = new Color(0x2C9AD1);
    public static final Color SKY_LIGHT   = new Color(0xE8F3FC);
    public static final Color CARD_WHITE  = Color.WHITE;
    public static final Color GOLD_ACCENT = new Color(0xF2A93B);

    public static final Color TEXT_DARK   = new Color(0x1B2430);
    public static final Color TEXT_MUTED  = new Color(0x5A6B7B);
    public static final Color BORDER_SOFT = new Color(0xD7E4EF);
    public static final Color SUCCESS     = new Color(0x2E7D32);
    public static final Color ERROR       = new Color(0xC62828);

    // ---- Fonts (Segoe UI is bundled with Windows; falls back gracefully elsewhere) ----
    public static Font heading(int size) {
        return new Font("Segoe UI Semibold", Font.BOLD, size);
    }

    public static Font body(int size) {
        return new Font("Segoe UI", Font.PLAIN, size);
    }

    public static Font bodyBold(int size) {
        return new Font("Segoe UI", Font.BOLD, size);
    }

    /** Paints a smooth vertical navy-to-blue gradient across the given rectangle. */
    public static void paintNavyGradient(Graphics2D g2, int w, int h) {
        GradientPaint gp = new GradientPaint(0, 0, NAVY, 0, h, PRIMARY_DARK);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
    }

    /** Paints a soft light-blue-to-white gradient, used for content backgrounds. */
    public static void paintLightGradient(Graphics2D g2, int w, int h) {
        GradientPaint gp = new GradientPaint(0, 0, SKY_LIGHT, 0, h, Color.WHITE);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
    }
}
