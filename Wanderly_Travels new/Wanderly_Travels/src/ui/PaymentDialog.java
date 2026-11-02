package ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Locale;

/**
 * Checkout window: left side order summary with GST breakup,
 * right side payment methods (UPI QR / Card / Pay at office).
 *
 * Usage:
 *     boolean done = PaymentDialog.open(this, order);
 *
 * NOTE: Card payment yahan SIMULATED hai (sirf validation hoti hai, paisa
 * nahi katta). Real payments ke liye Razorpay / PayU / Stripe jaisa payment
 * gateway integrate karna padta hai. Card number kabhi database mein store
 * mat karna.
 */
public class PaymentDialog extends JDialog {

    // Apni asli UPI ID yahan daalein
    private static final String UPI_ID = "yourvpa@oksbi";
    private static final String MERCHANT_NAME = "Wanderly Travels";

    private final OrderSummary order;
    private boolean completed = false;

    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel qrLabel = new JLabel("QR load ho raha hai...", SwingConstants.CENTER);
    private final JTextField utrField = new JTextField();

    private final JTextField cardName = new JTextField();
    private final JTextField cardNumber = new JTextField();
    private final JTextField cardExpiry = new JTextField();
    private final JPasswordField cardCvv = new JPasswordField();

    private final JLabel statusLabel = new JLabel(" ");
    private final RoundedButton payBtn =
            new RoundedButton("Confirm UPI Payment", Theme.GOLD_ACCENT, Theme.NAVY);
    private final RoundedButton cancelBtn =
            new RoundedButton("Cancel", Theme.SKY_LIGHT, Theme.NAVY);

    /** Dialog kholta hai. true = payment/booking process poora hua. */
    public static boolean open(Component parent, OrderSummary order) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        PaymentDialog d = new PaymentDialog(owner, order);
        d.setVisible(true);
        return d.completed;
    }

    private PaymentDialog(Window owner, OrderSummary order) {
        super(owner, "Secure Checkout - Wanderly Travels", Dialog.ModalityType.APPLICATION_MODAL);
        this.order = order;

        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        tabs.addChangeListener(e -> updatePayButtonText());
        payBtn.addActionListener(e -> onPay());
        cancelBtn.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(payBtn); // Enter dabane se pay

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(880, 610);
        setResizable(false);
        setLocationRelativeTo(owner);
        loadQrAsync();
    }

    // ------------------------------------------------------------------
    // UI building
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.NAVY);
        p.setBorder(new EmptyBorder(16, 26, 16, 26));

        JLabel title = new JLabel("Secure Checkout");
        title.setFont(Theme.heading(20));
        title.setForeground(Color.WHITE);

        JLabel inv = new JLabel("Invoice No: " + order.invoiceNumber());
        inv.setFont(Theme.bodyBold(12));
        inv.setForeground(Theme.GOLD_ACCENT);

        p.add(title, BorderLayout.WEST);
        p.add(inv, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.add(buildSummaryPanel(), BorderLayout.WEST);

        tabs.setFont(Theme.bodyBold(13));
        tabs.addTab("UPI / QR Code", buildUpiTab());
        tabs.addTab("Debit / Credit Card", buildCardTab());
        tabs.addTab("Pay at Office", buildCashTab());

        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(Color.WHITE);
        right.setBorder(new EmptyBorder(20, 20, 10, 24));
        right.add(tabs, BorderLayout.CENTER);
        p.add(right, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSummaryPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.SKY_LIGHT);
        p.setBorder(new EmptyBorder(24, 26, 24, 26));
        p.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.insets = new Insets(0, 0, 14, 0);

        JLabel title = new JLabel("Order Summary");
        title.setFont(Theme.heading(17));
        title.setForeground(Theme.NAVY);
        p.add(title, gc);

        addRow(p, gc, "Customer", order.getCustomerName(), false);
        addRow(p, gc, "Package", order.getPackageName(), false);
        addRow(p, gc, "Travel Date", String.valueOf(order.getTravelDate()), false);
        addRow(p, gc, "Travellers", String.valueOf(order.getPeople()), false);
        addSeparator(p, gc);
        addRow(p, gc, "Price x " + order.getPeople(), money(order.subtotal()), false);
        addRow(p, gc, "GST (5%)", money(order.gst()), false);
        addSeparator(p, gc);
        addRow(p, gc, "Total Payable", money(order.total()), true);

        // filler, taaki sab kuch upar rahe
        gc.gridy++;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weighty = 1;
        gc.insets = new Insets(0, 0, 0, 0);
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        p.add(filler, gc);

        gc.gridy++;
        gc.weighty = 0;
        JLabel secure = new JLabel("<html>100% secure checkout. Aapki card details store nahi ki jaati.</html>");
        secure.setFont(Theme.body(11));
        secure.setForeground(Theme.TEXT_DARK);
        p.add(secure, gc);
        return p;
    }

    private void addRow(JPanel p, GridBagConstraints gc, String label, String value, boolean big) {
        gc.gridy++;
        gc.gridwidth = 1;
        gc.weighty = 0;
        gc.insets = new Insets(5, 0, 5, 8);

        gc.gridx = 0;
        gc.weightx = 0;
        JLabel l = new JLabel(label);
        l.setFont(big ? Theme.heading(15) : Theme.body(13));
        l.setForeground(Theme.TEXT_DARK);
        p.add(l, gc);

        gc.gridx = 1;
        gc.weightx = 1;
        JLabel v = new JLabel("<html><div style='text-align:right'>" + value + "</div></html>");
        v.setHorizontalAlignment(SwingConstants.RIGHT);
        v.setFont(big ? Theme.heading(17) : Theme.bodyBold(13));
        v.setForeground(big ? Theme.SUCCESS : Theme.NAVY);
        p.add(v, gc);
    }

    private void addSeparator(JPanel p, GridBagConstraints gc) {
        gc.gridy++;
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.insets = new Insets(8, 0, 8, 0);
        p.add(new JSeparator(), gc);
    }

    private JPanel buildUpiTab() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(14, 20, 10, 20));

        qrLabel.setPreferredSize(new Dimension(240, 240));
        qrLabel.setMaximumSize(new Dimension(240, 240));
        qrLabel.setBorder(new LineBorder(new Color(0xD9DEE5), 1, true));
        qrLabel.setFont(Theme.body(12));
        qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel scan = new JLabel("Kisi bhi UPI app se scan karein (GPay, PhonePe, Paytm)");
        scan.setFont(Theme.bodyBold(12));
        scan.setForeground(Theme.NAVY);
        scan.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel upi = new JLabel("UPI ID: " + UPI_ID + "   |   Amount: " + money(order.total()));
        upi.setFont(Theme.body(12));
        upi.setForeground(Theme.TEXT_DARK);
        upi.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel utrLabel = new JLabel("Payment ke baad 12-digit UTR / Reference No. daalein");
        utrLabel.setFont(Theme.bodyBold(12));
        utrLabel.setForeground(Theme.TEXT_DARK);
        utrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        utrField.setMaximumSize(new Dimension(300, 32));
        utrField.setPreferredSize(new Dimension(300, 32));
        utrField.setHorizontalAlignment(SwingConstants.CENTER);
        utrField.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(qrLabel);
        p.add(Box.createVerticalStrut(10));
        p.add(scan);
        p.add(Box.createVerticalStrut(4));
        p.add(upi);
        p.add(Box.createVerticalStrut(18));
        p.add(utrLabel);
        p.add(Box.createVerticalStrut(6));
        p.add(utrField);
        return p;
    }

    private JPanel buildCardTab() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(20, 24, 10, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;
        gc.insets = new Insets(4, 0, 4, 0);

        gc.gridy = 0; p.add(fieldLabel("Name on Card"), gc);
        gc.gridy = 1; p.add(cardName, gc);
        gc.gridy = 2; p.add(fieldLabel("Card Number"), gc);
        gc.gridy = 3; p.add(cardNumber, gc);

        JPanel row = new JPanel(new GridLayout(2, 2, 14, 4));
        row.setBackground(Color.WHITE);
        row.add(fieldLabel("Expiry (MM/YY)"));
        row.add(fieldLabel("CVV"));
        row.add(cardExpiry);
        row.add(cardCvv);
        gc.gridy = 4; gc.insets = new Insets(10, 0, 4, 0); p.add(row, gc);

        JLabel note = new JLabel("<html>Demo mode: card payment sirf validate hoti hai, paisa nahi katta.</html>");
        note.setFont(Theme.body(11));
        note.setForeground(Theme.TEXT_DARK);
        gc.gridy = 5; gc.insets = new Insets(14, 0, 0, 0); p.add(note, gc);

        gc.gridy = 6; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        p.add(filler, gc);
        return p;
    }

    private JPanel buildCashTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(30, 30, 10, 30));
        JLabel msg = new JLabel("<html><div style='width:360px'>"
                + "<b>Office par payment karein</b><br><br>"
                + "Booking abhi confirm ho jayegi. Travel date se pehle hamare office me aakar "
                + "<b>" + money(order.total()) + "</b> (cash ya card) pay karein.<br><br>"
                + "Invoice par status <b>PAYMENT PENDING</b> dikhega jab tak payment nahi hota."
                + "</div></html>");
        msg.setFont(Theme.body(13));
        msg.setForeground(Theme.TEXT_DARK);
        msg.setVerticalAlignment(SwingConstants.TOP);
        p.add(msg, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(8, 26, 18, 24));

        statusLabel.setFont(Theme.bodyBold(12));
        statusLabel.setForeground(Theme.NAVY);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Color.WHITE);
        buttons.add(cancelBtn);
        buttons.add(payBtn);

        p.add(statusLabel, BorderLayout.WEST);
        p.add(buttons, BorderLayout.EAST);
        return p;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyBold(12));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private void updatePayButtonText() {
        switch (tabs.getSelectedIndex()) {
            case 0: payBtn.setText("Confirm UPI Payment"); break;
            case 1: payBtn.setText("Pay " + money(order.total())); break;
            default: payBtn.setText("Confirm Booking"); break;
        }
    }

    // ------------------------------------------------------------------
    // QR code
    // ------------------------------------------------------------------

    private void loadQrAsync() {
        String upiUrl = "upi://pay?pa=" + enc(UPI_ID)
                + "&pn=" + enc(MERCHANT_NAME)
                + "&am=" + order.total().toPlainString()
                + "&cu=INR&tn=" + enc("Booking " + order.invoiceNumber());
        final String api = "https://api.qrserver.com/v1/create-qr-code/?size=240x240&margin=8&data=" + enc(upiUrl);

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage img = ImageIO.read(java.net.URI.create(api).toURL());
                if (img == null) {
                    throw new IllegalStateException("QR image nahi mili");
                }
                return new ImageIcon(img);
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    qrLabel.setText("");
                    qrLabel.setIcon(icon);
                } catch (Exception ex) {
                    qrLabel.setText("<html><center>QR load nahi hua.<br>Internet check karein.</center></html>");
                }
            }
        }.execute();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    // ------------------------------------------------------------------
    // Payment logic
    // ------------------------------------------------------------------

    private void onPay() {
        switch (tabs.getSelectedIndex()) {
            case 0: {
                String utr = utrField.getText().trim();
                if (!utr.matches("\\d{12}")) {
                    warn("Sahi 12-digit UTR / Reference number daalein.\n"
                            + "Ye aapki UPI app ki payment details me milta hai.");
                    return;
                }
                process("UPI", utr, true);
                break;
            }
            case 1: {
                String error = validateCard();
                if (error != null) {
                    warn(error);
                    return;
                }
                String digits = cardNumber.getText().replaceAll("\\s+", "");
                String last4 = digits.substring(digits.length() - 4);
                process("CARD ending " + last4, "TXN" + System.currentTimeMillis(), true);
                break;
            }
            default:
                process("PAY AT OFFICE", "PENDING", false);
        }
    }

    private void process(final String method, final String reference, final boolean paid) {
        payBtn.setEnabled(false);
        cancelBtn.setEnabled(false);
        statusLabel.setText("Processing... kripya wait karein");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.sleep(1300); // gateway response ka demo delay
                return null;
            }

            @Override
            protected void done() {
                try {
                    order.markPayment(method, reference, paid);
                    completed = true;
                    dispose();
                } catch (Exception ex) {
                    // Error aaye to dialog band nahi hoga, user ko dobara try karne do
                    ex.printStackTrace();
                    statusLabel.setText(" ");
                    payBtn.setEnabled(true);
                    cancelBtn.setEnabled(true);
                    warn("Payment save nahi ho payi: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private String validateCard() {
        if (cardName.getText().isBlank()) {
            return "Card par likha naam daalein.";
        }
        String digits = cardNumber.getText().replaceAll("\\s+", "");
        if (!digits.matches("\\d{13,19}") || !luhnValid(digits)) {
            return "Card number galat hai.";
        }
        String exp = cardExpiry.getText().trim();
        if (!exp.matches("\\d{2}/\\d{2}")) {
            return "Expiry MM/YY format me daalein (jaise 08/28).";
        }
        try {
            int month = Integer.parseInt(exp.substring(0, 2));
            int year = 2000 + Integer.parseInt(exp.substring(3, 5));
            if (YearMonth.of(year, month).isBefore(YearMonth.now())) {
                return "Card expire ho chuka hai.";
            }
        } catch (Exception ex) {
            return "Expiry date galat hai.";
        }
        String cvv = new String(cardCvv.getPassword());
        if (!cvv.matches("\\d{3,4}")) {
            return "CVV 3 ya 4 digit ka hona chahiye.";
        }
        return null;
    }

    private static boolean luhnValid(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int d = number.charAt(i) - '0';
            if (alternate) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Payment", JOptionPane.WARNING_MESSAGE);
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.ENGLISH, "Rs. %,.2f", v);
    }
}