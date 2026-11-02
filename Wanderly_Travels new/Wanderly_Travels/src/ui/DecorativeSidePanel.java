package ui;

import javax.swing.*;
import java.awt.*;

public class DecorativeSidePanel extends JPanel {

    private final String heading;
    private final String subtitle;

    public DecorativeSidePanel(String heading, String subtitle) {
        this.heading = heading;
        this.subtitle = subtitle;
        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 44, 60, 44));

        JLabel title = new JLabel("<html><div style='width:340px'>" + heading + "</div></html>");
        title.setFont(new Font("Arial", Font.BOLD, 26));
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("<html><div style='width:340px'>" + subtitle + "</div></html>");
        sub.setFont(new Font("Arial", Font.PLAIN, 14));
        sub.setForeground(new Color(210, 225, 245));
        sub.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(title);
        textPanel.add(sub);
        add(textPanel, BorderLayout.SOUTH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setPaint(new GradientPaint(0, 0, new Color(20, 40, 80), 0, getHeight(), new Color(40, 80, 140)));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillOval(getWidth() - 160, -60, 260, 260);
        g2.setColor(new Color(255, 255, 255, 12));
        g2.fillOval(-100, getHeight() - 220, 260, 260);

        g2.dispose();
        super.paintComponent(g);
    }
}
