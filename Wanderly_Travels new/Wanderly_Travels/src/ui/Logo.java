package ui;

import javax.swing.*;
import java.awt.*;

/**
 * Displays the Wanderly Travels logo mark (images/logo_small.png) next to a
 * styled wordmark. Used consistently in the navigation bar and on the
 * Login / Registration screens so the brand is recognisable everywhere.
 */
public class Logo extends JPanel {

    public Logo(int iconSize, int fontSize, Color textColor) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 0));

        JLabel icon = new JLabel(ImageUtil.getScaledIcon("images/logo_small.png", iconSize, iconSize));
        add(icon);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Wanderly Travels");
        title.setFont(Theme.heading(fontSize));
        title.setForeground(textColor);

        JLabel tagline = new JLabel("Explore Incredible India");
        tagline.setFont(Theme.body(Math.max(10, fontSize - 10)));
        tagline.setForeground(textColor == Color.WHITE ? new Color(210, 225, 245) : Theme.TEXT_MUTED);

        textPanel.add(title);
        if (fontSize >= 18) {
            textPanel.add(tagline);
        }
        add(textPanel);
    }
}
