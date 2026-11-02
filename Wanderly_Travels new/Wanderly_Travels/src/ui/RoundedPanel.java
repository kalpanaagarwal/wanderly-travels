package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A JPanel subclass that paints a rounded, white "card" with a soft drop
 * shadow and a subtle border. Optionally highlights (slightly raises /
 * changes border colour) on mouse hover, which is what gives destination
 * and package cards their interactive feel.
 */
public class RoundedPanel extends JPanel {

    private int arc = 18;
    private Color background = Theme.CARD_WHITE;
    private Color borderColor = Theme.BORDER_SOFT;
    private boolean hoverEffect = false;
    private boolean hovering = false;

    public RoundedPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
    }

    public void setArc(int arc) {
        this.arc = arc;
    }

    public void setCardBackground(Color c) {
        this.background = c;
    }

    public void setBorderColor(Color c) {
        this.borderColor = c;
    }

    /** Enables a hover-highlight border, useful for clickable cards. */
    public void enableHoverEffect() {
        if (hoverEffect) return;
        hoverEffect = true;
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int shadowOffset = hovering ? 6 : 4;
        g2.setColor(new Color(20, 40, 70, hovering ? 55 : 35));
        g2.fillRoundRect(3, shadowOffset, getWidth() - 6, getHeight() - shadowOffset - 2, arc, arc);

        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - shadowOffset, arc, arc);

        g2.setColor(hovering ? Theme.ACCENT_BLUE : borderColor);
        g2.setStroke(new BasicStroke(hovering ? 2f : 1f));
        g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - shadowOffset - 2, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}
