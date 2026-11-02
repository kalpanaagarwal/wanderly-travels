package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A JButton subclass that paints itself as a rounded, flat, modern button
 * with a hover-lighten effect and a press-darken effect, instead of the
 * default grey Swing button. Used for every clickable action in the app so
 * the whole interface feels consistent.
 */
public class RoundedButton extends JButton {

    private Color baseColor;
    private Color hoverColor;
    private Color pressColor;
    private Color textColor;
    private boolean hovering = false;
    private boolean pressing = false;
    private int arc = 24;

    public RoundedButton(String text) {
        this(text, Theme.PRIMARY, Color.WHITE);
    }

    public RoundedButton(String text, Color baseColor, Color textColor) {
        super(text);
        this.baseColor = baseColor;
        this.textColor = textColor;
        this.hoverColor = baseColor.brighter();
        this.pressColor = baseColor.darker();

        setOpaque(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(textColor);
        setFont(Theme.bodyBold(14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                pressing = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressing = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressing = false;
                repaint();
            }
        });
    }

    public void setArc(int arc) {
        this.arc = arc;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill = pressing ? pressColor : (hovering ? hoverColor : baseColor);

        // subtle drop shadow
        if (!pressing) {
            g2.setColor(new Color(0, 0, 0, 35));
            g2.fillRoundRect(2, 4, getWidth() - 4, getHeight() - 4, arc, arc);
        }

        g2.setColor(fill);
        g2.fillRoundRect(0, pressing ? 2 : 0, getWidth() - 2, getHeight() - 4, arc, arc);
        g2.dispose();

        super.paintComponent(g);
    }
}
