package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Self-contained invoice window (Print + Save as HTML).
 * No OrderSummary / Formats class needed - everything is inside this file.
 *
 * Usage:
 *   InvoiceDialog.Data d = new InvoiceDialog.Data();
 *   d.bookingId = 7; d.customerName = "Kalpana"; d.email = "a@b.com"; d.phone = "98...";
 *   d.packageName = "Royal Jaipur Escape"; d.travelDate = LocalDate.of(2026,10,20);
 *   d.people = 3; d.unitPrice = new BigDecimal("8999");
 *   d.paid = true; d.paidOn = LocalDateTime.now();
 *   d.paymentMethod = "UPI"; d.transactionRef = "412345678901";
 *   InvoiceDialog.show(this, d);
 */
public final class InvoiceDialog {

    private static final String PENDING_COLOUR = "#E37400";
    private static final String MUTED_COLOUR = "#777777";
    private static final String BORDER_COLOUR = "#D9DEE5";
    private static final String PANEL_COLOUR = "#F5F8FC";
    private static final BigDecimal GST_RATE = new BigDecimal("0.05");
    private static final String GST_TEXT = "5%";

    private InvoiceDialog() { }

    /** Plain data holder - fill the fields and pass to show(). */
    public static class Data {
        public int bookingId;
        public String customerName = "";
        public String email = "";
        public String phone = "";
        public String packageName = "";
        public LocalDate travelDate;
        public int people = 1;
        public BigDecimal unitPrice = BigDecimal.ZERO;
        public boolean paid;
        public LocalDateTime issuedOn = LocalDateTime.now();
        public LocalDateTime paidOn;
        public String paymentMethod = "-";
        public String transactionRef = "-";

        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(people)).setScale(2, RoundingMode.HALF_UP);
        }
        public BigDecimal gst() { return subtotal().multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP); }
        public BigDecimal total() { return subtotal().add(gst()); }
        public String invoiceNumber() {
            return "INV-" + issuedOn.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + bookingId;
        }
    }

    public static void show(Component parent, Data order) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(owner, "Invoice " + order.invoiceNumber(),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        String html = buildHtml(order);
        JEditorPane view = new JEditorPane("text/html", html);
        view.setEditable(false);
        view.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        RoundedButton printBtn = new RoundedButton("Print", Theme.GOLD_ACCENT, Theme.NAVY);
        RoundedButton saveBtn = new RoundedButton("Save as HTML", Theme.SKY_LIGHT, Theme.NAVY);
        RoundedButton closeBtn = new RoundedButton("Close", Theme.SKY_LIGHT, Theme.NAVY);
        printBtn.addActionListener(e -> print(dialog, view));
        saveBtn.addActionListener(e -> save(dialog, html, order.invoiceNumber()));
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttons.setBackground(Color.WHITE);
        buttons.add(printBtn);
        buttons.add(saveBtn);
        buttons.add(closeBtn);

        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(680, 760);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static void print(Component parent, JEditorPane view) {
        try {
            view.print();
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(parent, "The invoice could not be printed:\n" + ex.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void save(Component parent, String html, String invoiceNumber) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(invoiceNumber + ".html"));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".html")) file = new File(file.getPath() + ".html");
        if (file.exists() && JOptionPane.showConfirmDialog(parent,
                file.getName() + " already exists. Do you want to replace it?",
                "Confirm Overwrite", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            Files.write(file.toPath(), html.getBytes(StandardCharsets.UTF_8));
            JOptionPane.showMessageDialog(parent, "The invoice was saved successfully.",
                    "Invoice Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "The invoice could not be saved:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------- small helpers (replace the old Formats class) ----------------

    private static String hex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private static String esc(String s) {
        if (s == null) return "-";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String money(BigDecimal v) {
        return "Rs. " + String.format(Locale.US, "%,.2f", v == null ? BigDecimal.ZERO : v);
    }

    private static String date(LocalDate d) {
        return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
    }

    private static String dateTime(LocalDateTime d) {
        return d == null ? "-" : d.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH));
    }

    // ---------------- HTML invoice ----------------

    static String buildHtml(Data o) {
        String navy = hex(Theme.NAVY);
        String gold = hex(Theme.GOLD_ACCENT);
        String statusColour = o.paid ? hex(Theme.SUCCESS) : PENDING_COLOUR;
        String statusText = o.paid ? "PAID" : "PAYMENT PENDING";

        StringBuilder h = new StringBuilder(4096);
        h.append("<html><head><meta charset='UTF-8'></head>")
                .append("<body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0;color:#222'>");

        h.append("<table width='100%' cellpadding='18' cellspacing='0' bgcolor='").append(navy).append("'><tr>")
                .append("<td><font color='#FFFFFF' size='6'><b>WANDERLY TRAVELS</b></font><br>")
                .append("<font color='").append(gold).append("' size='3'>Discover Incredible India</font></td>")
                .append("<td align='right'><font color='#FFFFFF' size='5'><b>INVOICE</b></font><br>")
                .append("<font color='").append(gold).append("' size='3'>")
                .append(esc(o.invoiceNumber())).append("</font></td></tr></table>");

        h.append("<div style='padding:22px'>");

        h.append("<table width='100%' cellpadding='4' cellspacing='0'><tr valign='top'>")
                .append("<td width='50%'><font color='").append(MUTED_COLOUR).append("' size='2'>BILLED TO</font><br>")
                .append("<b>").append(esc(o.customerName)).append("</b><br>")
                .append(esc(o.email)).append("<br>")
                .append(esc(o.phone)).append("</td>")
                .append("<td width='50%' align='right'><font color='").append(MUTED_COLOUR)
                .append("' size='2'>INVOICE DETAILS</font><br>")
                .append("Booking ID: <b>#").append(o.bookingId).append("</b><br>")
                .append("Issued: ").append(dateTime(o.issuedOn)).append("<br>");
        if (o.paid) {
            h.append("Paid on: ").append(dateTime(o.paidOn)).append("<br>");
        }
        h.append("Status: <font color='").append(statusColour).append("'><b>")
                .append(statusText).append("</b></font></td></tr></table><br>");

        h.append("<table width='100%' cellpadding='8' cellspacing='0' border='1' bordercolor='")
                .append(BORDER_COLOUR).append("'>")
                .append("<tr bgcolor='").append(hex(Theme.SKY_LIGHT)).append("'>")
                .append("<td><b>Description</b></td><td align='center'><b>Travel date</b></td>")
                .append("<td align='center'><b>Qty</b></td><td align='right'><b>Rate</b></td>")
                .append("<td align='right'><b>Amount</b></td></tr>")
                .append("<tr><td>").append(esc(o.packageName)).append("</td>")
                .append("<td align='center'>").append(date(o.travelDate)).append("</td>")
                .append("<td align='center'>").append(o.people).append("</td>")
                .append("<td align='right'>").append(money(o.unitPrice)).append("</td>")
                .append("<td align='right'>").append(money(o.subtotal())).append("</td></tr>")
                .append("</table><br>");

        h.append("<table width='100%' cellpadding='4' cellspacing='0'><tr><td width='55%'></td><td>")
                .append("<table width='100%' cellpadding='6' cellspacing='0'>")
                .append("<tr><td>Subtotal</td><td align='right'>").append(money(o.subtotal())).append("</td></tr>")
                .append("<tr><td>GST (").append(GST_TEXT).append(")</td>")
                .append("<td align='right'>").append(money(o.gst())).append("</td></tr>")
                .append("<tr bgcolor='").append(navy).append("'><td><font color='#FFFFFF'><b>TOTAL</b></font></td>")
                .append("<td align='right'><font color='").append(gold).append("'><b>")
                .append(money(o.total())).append("</b></font></td></tr>")
                .append("</table></td></tr></table><br>");

        h.append("<table width='100%' cellpadding='10' cellspacing='0' bgcolor='").append(PANEL_COLOUR)
                .append("'><tr><td><b>Payment information</b><br>")
                .append("Method: ").append(esc(o.paymentMethod)).append("<br>")
                .append("Transaction / reference no: ").append(esc(o.transactionRef))
                .append("</td></tr></table><br><br>");

        h.append("<center><font color='").append(MUTED_COLOUR).append("' size='2'>")
                .append("This is a computer-generated invoice and does not require a signature.<br>")
                .append("Thank you for booking with Wanderly Travels. Have a wonderful journey!")
                .append("</font></center></div></body></html>");
        return h.toString();
    }
}