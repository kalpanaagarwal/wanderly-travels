package ui;

import data.BookingDAO;
import data.PackageDAO;
import model.Booking;
import model.TourPackage;
import util.Session;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.sql.Date;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * Lets a logged-in user pick a tour package, choose a travel date and
 * number of people, see the total price calculated live, and confirm the
 * booking (which is stored in MySQL, tied to their user_id). Their own
 * past bookings are shown in a table underneath - and only theirs, because
 * every query is filtered by the logged-in user's numeric user_id.
 */
public class BookingPanel extends JPanel {

    // ---------------- payment settings (apne hisaab se badlein) ----------------
    private static final String DEFAULT_UPI_ID = "yourname@upi";
    private static final String UPI_ID = DEFAULT_UPI_ID;      // <-- yahan apni UPI ID likhein, jaise "kalpana@okhdfcbank"
    private static final String PAYEE_NAME = "Wanderly Travels";
    private static final BigDecimal GST_RATE = new BigDecimal("0.05");   // 5% GST (0 likhne par GST nahi lagega)

    private final MainFrame frame;

    private final JLabel usernameValue = new JLabel();
    private final JComboBox<TourPackage> packageCombo = new JComboBox<>();
    private final JSpinner dateSpinner;
    private final JSpinner peopleSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
    private final JTextField phoneField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JLabel totalValue = new JLabel("Rs. 0.00");

    private final DefaultTableModel bookingsTableModel =
            new DefaultTableModel(new Object[]{"Package", "Travel Date", "People", "Total (Rs.)", "Status", "Booked On"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public BookingPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel pageTitle = new JLabel("Book a Tour Package");
        pageTitle.setFont(Theme.heading(24));
        pageTitle.setForeground(Theme.NAVY);
        pageTitle.setBorder(BorderFactory.createEmptyBorder(24, 40, 10, 40));
        add(pageTitle, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

        SpinnerDateModel dateModel = new SpinnerDateModel();
        dateSpinner = new JSpinner(dateModel);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));

        body.add(buildFormCard());
        body.add(Box.createVerticalStrut(24));
        body.add(buildBookingsTableCard());

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        packageCombo.addActionListener(e -> updateTotal());
        peopleSpinner.addChangeListener(e -> updateTotal());
    }

    private RoundedPanel buildFormCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gc.gridx = 0; gc.gridy = row; card.add(formLabel("Booking For"), gc);
        gc.gridx = 1; gc.gridy = row; usernameValue.setFont(Theme.bodyBold(14));
        usernameValue.setForeground(Theme.PRIMARY_DARK);
        card.add(usernameValue, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; card.add(formLabel("Tour Package"), gc);
        gc.gridx = 1; gc.gridy = row; gc.weightx = 1; card.add(packageCombo, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; gc.weightx = 0; card.add(formLabel("Travel Date"), gc);
        gc.gridx = 1; gc.gridy = row; card.add(dateSpinner, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; card.add(formLabel("Number of People"), gc);
        gc.gridx = 1; gc.gridy = row; card.add(peopleSpinner, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; card.add(formLabel("Contact Phone"), gc);
        gc.gridx = 1; gc.gridy = row; card.add(phoneField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; card.add(formLabel("Contact Email"), gc);
        gc.gridx = 1; gc.gridy = row; card.add(emailField, gc);

        row++;
        gc.gridx = 0; gc.gridy = row; card.add(formLabel("Total Amount"), gc);
        gc.gridx = 1; gc.gridy = row;
        totalValue.setFont(Theme.heading(18));
        totalValue.setForeground(Theme.SUCCESS);
        card.add(totalValue, gc);

        row++;
        gc.gridx = 1; gc.gridy = row; gc.anchor = GridBagConstraints.EAST; gc.fill = GridBagConstraints.NONE;
        RoundedButton confirmBtn = new RoundedButton("Confirm Booking", Theme.GOLD_ACCENT, Theme.NAVY);
        confirmBtn.addActionListener(e -> confirmBooking());
        card.add(confirmBtn, gc);

        return card;
    }

    private JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.bodyBold(13));
        l.setForeground(Theme.TEXT_DARK);
        return l;
    }

    private RoundedPanel buildBookingsTableCard() {
        RoundedPanel card = new RoundedPanel();
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        card.setPreferredSize(new Dimension(100, 260));

        JLabel title = new JLabel("My Bookings");
        title.setFont(Theme.heading(16));
        title.setForeground(Theme.NAVY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        card.add(title, BorderLayout.NORTH);

        JTable table = new JTable(bookingsTableModel);
        table.setRowHeight(26);
        table.setFont(Theme.body(12));
        table.getTableHeader().setFont(Theme.bodyBold(12));
        table.getTableHeader().setBackground(Theme.SKY_LIGHT);
        table.setFillsViewportHeight(true);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    /**
     * Reloads packages + this user's bookings. If a package was chosen via a
     * "Book Now" button elsewhere, it is preselected here.
     */
    public void refresh(TourPackage preselect) {
        if (Session.getCurrentUser() != null) {
            usernameValue.setText(Session.getCurrentUser().getUsername());
            if (emailField.getText().isBlank()) {
                emailField.setText(nullToEmpty(Session.getCurrentUser().getEmail()));
            }
            if (phoneField.getText().isBlank()) {
                phoneField.setText(nullToEmpty(Session.getCurrentUser().getPhone()));
            }
        } else {
            usernameValue.setText("Not logged in");
        }

        packageCombo.removeAllItems();
        try {
            List<TourPackage> packages = new PackageDAO().getAllPackages();
            for (TourPackage p : packages) {
                packageCombo.addItem(p);
            }
            if (preselect != null) {
                for (int i = 0; i < packageCombo.getItemCount(); i++) {
                    if (packageCombo.getItemAt(i).getPackageId() == preselect.getPackageId()) {
                        packageCombo.setSelectedIndex(i);
                        break;
                    }
                }
            } else if (BookingSelection.isSet()) {
                // Package chosen on the Explore Tours page via "Book Now"
                selectPackageFromExplore();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load tour packages.\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        updateTotal();
        loadMyBookings();
    }

    /**
     * Selects, in the combo box, the package the user clicked "Book Now" on.
     * Matches by package name (and by price too, if several packages share a name).
     */
    private void selectPackageFromExplore() {
        String wantedName = BookingSelection.getPackageName() == null
                ? "" : BookingSelection.getPackageName().trim();
        BigDecimal wantedPrice = BigDecimal.valueOf(BookingSelection.getPrice());

        int foundIndex = -1;
        for (int i = 0; i < packageCombo.getItemCount(); i++) {
            TourPackage p = packageCombo.getItemAt(i);
            if (p.getPackageName() != null && p.getPackageName().trim().equalsIgnoreCase(wantedName)) {
                if (foundIndex == -1) {
                    foundIndex = i;
                }
                if (p.getPrice() != null && p.getPrice().compareTo(wantedPrice) == 0) {
                    foundIndex = i;
                    break;
                }
            }
        }
        if (foundIndex >= 0) {
            packageCombo.setSelectedIndex(foundIndex);
        }
        // Use the selection only once, so later visits to this page start fresh
        BookingSelection.clear();
    }

    private void updateTotal() {
        TourPackage selected = (TourPackage) packageCombo.getSelectedItem();
        int people = (Integer) peopleSpinner.getValue();
        if (selected != null && selected.getPrice() != null) {
            BigDecimal[] b = bill(selected.getPrice(), people);
            totalValue.setText("Rs. " + money(b[2]) + "   (incl. GST)");
        } else {
            totalValue.setText("Rs. 0.00");
        }
    }

    private void loadMyBookings() {
        bookingsTableModel.setRowCount(0);
        if (Session.getCurrentUser() == null) {
            return;
        }
        try {
            List<Booking> bookings = new BookingDAO().getBookingsForUser(Session.getCurrentUser().getUserId());
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy");
            for (Booking b : bookings) {
                bookingsTableModel.addRow(new Object[]{
                        b.getPackageName(),
                        b.getTravelDate() == null ? "" : fmt.format(b.getTravelDate()),
                        b.getNumPeople(),
                        b.getTotalAmount(),
                        b.getStatus(),
                        b.getBookingDate() == null ? "" : fmt.format(b.getBookingDate())
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Could not load your bookings.\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void confirmBooking() {
        if (Session.getCurrentUser() == null) {
            JOptionPane.showMessageDialog(this, "Please login first.", "Login Required", JOptionPane.WARNING_MESSAGE);
            frame.navigateTo(MainFrame.LOGIN);
            return;
        }
        TourPackage selected = (TourPackage) packageCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a tour package.", "Missing Information",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (phoneField.getText().isBlank() || emailField.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Please fill in your contact phone and email.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        java.util.Date chosenDate = (java.util.Date) dateSpinner.getValue();
        LocalDate today = LocalDate.now();
        LocalDate chosenLocalDate = chosenDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (chosenLocalDate.isBefore(today)) {
            JOptionPane.showMessageDialog(this, "Travel date cannot be in the past.",
                    "Invalid Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int people = (Integer) peopleSpinner.getValue();
        BigDecimal[] amt = bill(selected.getPrice(), people);
        String orderRef = "WT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String customer = Session.getCurrentUser().getUsername();
        String phone = phoneField.getText().trim();
        String email = emailField.getText().trim();

        // 1) bill + payment window (UPI QR code or pay at office)
        PaymentPanel pay = showPaymentDialog(SwingUtilities.getWindowAncestor(this), customer, orderRef,
                selected.getPackageName(), chosenLocalDate, people, selected.getPrice(), amt);
        if (!pay.confirmed) {
            return;
        }

        // 2) save the booking (total includes GST)
        Booking booking = new Booking();
        booking.setUserId(Session.getCurrentUser().getUserId());
        booking.setPackageId(selected.getPackageId());
        booking.setTravelDate(Date.valueOf(chosenLocalDate));
        booking.setNumPeople(people);
        booking.setTotalAmount(amt[2]);
        booking.setContactPhone(phone);
        booking.setContactEmail(email);

        try {
            int newId;
            try {
                booking.setStatus(pay.upi ? "PAID" : "PAY LATER");
                newId = new BookingDAO().createBooking(booking);
            } catch (SQLException statusRejected) {
                // some databases only allow the old status values - fall back to CONFIRMED
                booking.setStatus("CONFIRMED");
                newId = new BookingDAO().createBooking(booking);
            }
            if (newId > 0) {
                loadMyBookings();
                // 3) invoice (can be printed or saved)
                String html = buildInvoiceHtml(newId, orderRef, customer, phone, email, selected.getPackageName(),
                        chosenLocalDate, people, selected.getPrice(), amt, pay.upi, pay.utr);
                showInvoice(html, "Invoice_" + orderRef + ".html");
            } else {
                JOptionPane.showMessageDialog(this, "Booking could not be saved. Please try again.",
                        "Booking Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error while saving booking:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =====================================================================
    //  Billing + payment
    // =====================================================================

    /** {subtotal, GST, total payable} for one booking. */
    private static BigDecimal[] bill(BigDecimal ratePerPerson, int people) {
        BigDecimal subtotal = ratePerPerson.multiply(BigDecimal.valueOf(people)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gst = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal[]{subtotal, gst, subtotal.add(gst)};
    }

    private static String money(BigDecimal v) {
        return String.format(Locale.US, "%,.2f", v);
    }

    private static String gstLabel() {
        return "GST (" + GST_RATE.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%)";
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** upi://pay link - this is what the QR code contains. Any UPI app can scan it. */
    private static String upiLink(BigDecimal amount, String orderRef) {
        try {
            String name = URLEncoder.encode(PAYEE_NAME, "UTF-8").replace("+", "%20");
            String note = URLEncoder.encode("Wanderly " + orderRef, "UTF-8").replace("+", "%20");
            return "upi://pay?pa=" + UPI_ID + "&pn=" + name + "&am=" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + "&cu=INR&tn=" + note;
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Opens the "Review & Pay" window and returns what the customer chose (check .confirmed). */
    private static PaymentPanel showPaymentDialog(Window owner, String customer, String orderRef, String packageName,
                                                  LocalDate travelDate, int people, BigDecimal rate, BigDecimal[] amt) {
        JDialog d = new JDialog(owner, "Review & Pay", Dialog.ModalityType.APPLICATION_MODAL);
        PaymentPanel p = new PaymentPanel(customer, orderRef, packageName, travelDate, people, rate, amt);
        d.setContentPane(p);
        d.pack();
        d.setResizable(false);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        return p;
    }

    /** Content of the payment window: bill on the left, payment method (UPI QR / pay at office) on the right. */
    private static class PaymentPanel extends JPanel {
        boolean confirmed = false;
        boolean upi = true;
        String utr = "";

        private final JTextField utrField = new JTextField();
        private static final Color LINE = new Color(222, 229, 242);

        PaymentPanel(String customer, String orderRef, String packageName, LocalDate travelDate, int people,
                     BigDecimal rate, BigDecimal[] amt) {
            super(new BorderLayout());
            setBackground(Color.WHITE);

            // ---------- header ----------
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(Theme.NAVY);
            header.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
            JLabel title = new JLabel("Review & Pay");
            title.setFont(Theme.heading(20));
            title.setForeground(Color.WHITE);
            JLabel ref = new JLabel("Order Ref: " + orderRef);
            ref.setFont(Theme.body(12));
            ref.setForeground(new Color(200, 216, 240));
            header.add(title, BorderLayout.WEST);
            header.add(ref, BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            // ---------- bill (left) ----------
            JPanel bill = new JPanel(new GridBagLayout());
            bill.setBackground(new Color(248, 250, 255));
            bill.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, LINE),
                    BorderFactory.createEmptyBorder(20, 24, 20, 24)));
            bill.setPreferredSize(new Dimension(340, 400));

            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.weightx = 1;
            gc.gridwidth = 2;
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.NORTHWEST;
            int row = 0;

            JLabel billTitle = new JLabel("BILL SUMMARY");
            billTitle.setFont(Theme.bodyBold(12));
            billTitle.setForeground(Theme.TEXT_MUTED);
            gc.gridy = row++;
            gc.insets = new Insets(0, 0, 8, 0);
            bill.add(billTitle, gc);

            JLabel pkg = new JLabel(packageName);
            pkg.setFont(Theme.heading(17));
            pkg.setForeground(Theme.NAVY);
            pkg.setToolTipText(packageName);
            gc.gridy = row++;
            gc.insets = new Insets(0, 0, 14, 0);
            bill.add(pkg, gc);

            row = billRow(bill, row, "Customer", customer, false);
            row = billRow(bill, row, "Travel date", travelDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")), false);
            row = billRow(bill, row, "Travellers", String.valueOf(people), false);
            row = billRow(bill, row, "Rate per person", "Rs. " + money(rate), false);
            row = billLine(bill, row);
            row = billRow(bill, row, "Subtotal", "Rs. " + money(amt[0]), false);
            row = billRow(bill, row, gstLabel(), "Rs. " + money(amt[1]), false);
            row = billLine(bill, row);

            JLabel totalLbl = new JLabel("Total Payable");
            totalLbl.setFont(Theme.bodyBold(14));
            totalLbl.setForeground(Theme.NAVY);
            JLabel totalVal = new JLabel("Rs. " + money(amt[2]), SwingConstants.RIGHT);
            totalVal.setFont(Theme.heading(20));
            totalVal.setForeground(Theme.SUCCESS);
            gc.gridwidth = 1;
            gc.gridy = row;
            gc.weightx = 0;
            gc.insets = new Insets(8, 0, 0, 8);
            bill.add(totalLbl, gc);
            gc.gridx = 1;
            gc.weightx = 1;
            gc.insets = new Insets(8, 0, 0, 0);
            bill.add(totalVal, gc);
            row++;

            GridBagConstraints filler = new GridBagConstraints();
            filler.gridy = row;
            filler.weighty = 1;
            bill.add(Box.createVerticalGlue(), filler);

            add(bill, BorderLayout.WEST);

            // ---------- payment method (right) ----------
            JPanel right = new JPanel(new BorderLayout(0, 12));
            right.setBackground(Color.WHITE);
            right.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));

            JToggleButton upiBtn = toggle("UPI / QR Code", true);
            JToggleButton cashBtn = toggle("Pay at Office", false);
            ButtonGroup group = new ButtonGroup();
            group.add(upiBtn);
            group.add(cashBtn);
            JPanel toggles = new JPanel(new GridLayout(1, 2, 8, 0));
            toggles.setOpaque(false);
            toggles.add(upiBtn);
            toggles.add(cashBtn);
            right.add(toggles, BorderLayout.NORTH);

            CardLayout cards = new CardLayout();
            JPanel holder = new JPanel(cards);
            holder.setOpaque(false);
            holder.add(buildUpiCard(amt[2], orderRef), "upi");
            holder.add(buildCashCard(amt[2]), "cash");
            right.add(holder, BorderLayout.CENTER);

            // ---------- buttons ----------
            CardLayout btnCards = new CardLayout();
            JPanel confirmHolder = new JPanel(btnCards);
            confirmHolder.setOpaque(false);

            RoundedButton paidBtn = new RoundedButton("I Have Paid", Theme.GOLD_ACCENT, Theme.NAVY);
            paidBtn.addActionListener(e -> finish());
            RoundedButton reserveBtn = new RoundedButton("Reserve Booking", Theme.GOLD_ACCENT, Theme.NAVY);
            reserveBtn.addActionListener(e -> finish());
            confirmHolder.add(paidBtn, "upi");
            confirmHolder.add(reserveBtn, "cash");

            RoundedButton cancelBtn = new RoundedButton("Cancel", new Color(225, 231, 242), Theme.NAVY);
            cancelBtn.addActionListener(e -> {
                confirmed = false;
                closeWindow();
            });

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttons.setOpaque(false);
            buttons.add(cancelBtn);
            buttons.add(confirmHolder);
            right.add(buttons, BorderLayout.SOUTH);

            upiBtn.addActionListener(e -> {
                upi = true;
                cards.show(holder, "upi");
                btnCards.show(confirmHolder, "upi");
            });
            cashBtn.addActionListener(e -> {
                upi = false;
                cards.show(holder, "cash");
                btnCards.show(confirmHolder, "cash");
            });

            add(right, BorderLayout.CENTER);
        }

        private JToggleButton toggle(String text, boolean selected) {
            JToggleButton b = new JToggleButton(text, selected);
            b.setFont(Theme.bodyBold(13));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setPreferredSize(new Dimension(160, 38));
            return b;
        }

        private int billRow(JPanel p, int row, String label, String value, boolean bold) {
            JLabel l = new JLabel(label);
            l.setFont(Theme.body(13));
            l.setForeground(Theme.TEXT_MUTED);
            JLabel v = new JLabel(value, SwingConstants.RIGHT);
            v.setFont(bold ? Theme.bodyBold(13) : Theme.body(13));
            v.setForeground(Theme.TEXT_DARK);
            v.setToolTipText(value);

            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row;
            c.anchor = GridBagConstraints.NORTHWEST;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(4, 0, 4, 8);
            c.gridx = 0;
            p.add(l, c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(4, 0, 4, 0);
            p.add(v, c);
            return row + 1;
        }

        private int billLine(JPanel p, int row) {
            JSeparator sep = new JSeparator();
            GridBagConstraints c = new GridBagConstraints();
            c.gridy = row;
            c.gridx = 0;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(8, 0, 8, 0);
            p.add(sep, c);
            return row + 1;
        }

        private JComponent buildUpiCard(BigDecimal total, String orderRef) {
            JPanel card = new JPanel();
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            QrPanel qr = new QrPanel(QrEncoder.encode(upiLink(total, orderRef)), 230);
            qr.setAlignmentX(Component.CENTER_ALIGNMENT);
            qr.setBorder(BorderFactory.createLineBorder(LINE));

            JLabel scan = new JLabel("Scan & pay Rs. " + money(total));
            scan.setFont(Theme.heading(16));
            scan.setForeground(Theme.NAVY);
            scan.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel apps = new JLabel("Use GPay, PhonePe, Paytm or any UPI app");
            apps.setFont(Theme.body(12));
            apps.setForeground(Theme.TEXT_MUTED);
            apps.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel upiId = new JLabel("UPI ID: " + UPI_ID);
            upiId.setFont(Theme.bodyBold(12));
            upiId.setForeground(Theme.TEXT_DARK);
            upiId.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(Box.createRigidArea(new Dimension(0, 4)));
            card.add(qr);
            card.add(Box.createRigidArea(new Dimension(0, 10)));
            card.add(scan);
            card.add(Box.createRigidArea(new Dimension(0, 2)));
            card.add(apps);
            card.add(Box.createRigidArea(new Dimension(0, 2)));
            card.add(upiId);

            if (UPI_ID.equals(DEFAULT_UPI_ID)) {
                JLabel warn = new JLabel("Demo QR - set your own UPI_ID in BookingPanel.java");
                warn.setFont(Theme.body(11));
                warn.setForeground(Theme.ERROR);
                warn.setAlignmentX(Component.CENTER_ALIGNMENT);
                card.add(Box.createRigidArea(new Dimension(0, 2)));
                card.add(warn);
            }

            JLabel utrLbl = new JLabel("After paying, enter the UPI Transaction ID (UTR):");
            utrLbl.setFont(Theme.bodyBold(12));
            utrLbl.setForeground(Theme.TEXT_DARK);
            utrLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            utrField.setMaximumSize(new Dimension(300, 30));
            utrField.setPreferredSize(new Dimension(300, 30));
            utrField.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel verify = new JLabel("Our team verifies the payment using this ID.");
            verify.setFont(Theme.body(11));
            verify.setForeground(Theme.TEXT_MUTED);
            verify.setAlignmentX(Component.CENTER_ALIGNMENT);

            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(utrLbl);
            card.add(Box.createRigidArea(new Dimension(0, 4)));
            card.add(utrField);
            card.add(Box.createRigidArea(new Dimension(0, 3)));
            card.add(verify);
            return card;
        }

        private JComponent buildCashCard(BigDecimal total) {
            JPanel card = new JPanel();
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

            JLabel head = new JLabel("Reserve now, pay later");
            head.setFont(Theme.heading(17));
            head.setForeground(Theme.NAVY);
            head.setAlignmentX(Component.LEFT_ALIGNMENT);

            JTextArea text = new JTextArea("Your booking will be saved as PAY LATER.\n\n"
                    + "Please pay Rs. " + money(total) + " at our office (cash, card or UPI) before your travel date "
                    + "and show your invoice. Unpaid bookings may be released.");
            text.setLineWrap(true);
            text.setWrapStyleWord(true);
            text.setEditable(false);
            text.setOpaque(false);
            text.setFont(Theme.body(13));
            text.setForeground(Theme.TEXT_DARK);
            text.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.setMaximumSize(new Dimension(340, 160));

            card.add(Box.createRigidArea(new Dimension(0, 24)));
            card.add(head);
            card.add(Box.createRigidArea(new Dimension(0, 12)));
            card.add(text);
            card.add(Box.createVerticalGlue());
            return card;
        }

        private void finish() {
            if (upi) {
                String t = utrField.getText().trim();
                if (!t.matches("[A-Za-z0-9]{6,30}")) {
                    JOptionPane.showMessageDialog(this,
                            "Please enter the UPI Transaction ID (UTR) shown in your payment app\n"
                                    + "(6 to 30 letters or digits).",
                            "Transaction ID needed", JOptionPane.WARNING_MESSAGE);
                    utrField.requestFocus();
                    return;
                }
                utr = t.toUpperCase();
            }
            confirmed = true;
            closeWindow();
        }

        private void closeWindow() {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) w.dispose();
        }
    }

    // ---------------------------------------------------------------------
    //  Invoice
    // ---------------------------------------------------------------------
    private String buildInvoiceHtml(int bookingId, String orderRef, String customer, String phone, String email,
                                    String packageName, LocalDate travelDate, int people, BigDecimal rate,
                                    BigDecimal[] amt, boolean paidByUpi, String utr) {
        String invoiceNo = "INV-" + orderRef;
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String payText = paidByUpi
                ? "UPI &nbsp;|&nbsp; Txn ID: <b>" + esc(utr) + "</b> &nbsp;(subject to verification)"
                : "Pay at office &nbsp;|&nbsp; <b>PAYMENT PENDING</b>";

        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='UTF-8'><title>").append(invoiceNo).append("</title></head>")
         .append("<body style='font-family:Segoe UI,Arial,sans-serif;margin:0;padding:0'>");

        h.append("<table width='100%' cellpadding='14' cellspacing='0' bgcolor='#0f2d55'><tr>")
         .append("<td><font color='#ffffff' size='5'><b>Wanderly Travels</b></font><br>")
         .append("<font color='#c8d8f0' size='2'>Explore Incredible India</font></td>")
         .append("<td align='right'><font color='#ffffff' size='4'><b>INVOICE</b></font><br>")
         .append("<font color='#c8d8f0' size='2'>").append(invoiceNo).append("</font></td></tr></table>");

        h.append("<table width='100%' cellpadding='10' cellspacing='0'><tr valign='top'>")
         .append("<td width='50%'><font size='2' color='#78879f'>BILLED TO</font><br><b>").append(esc(customer)).append("</b><br>")
         .append("<font size='2'>").append(esc(phone)).append("<br>").append(esc(email)).append("</font></td>")
         .append("<td width='50%' align='right'><font size='2' color='#78879f'>INVOICE DATE</font><br><b>").append(today).append("</b><br>")
         .append("<font size='2'>Booking ID: <b>").append(bookingId).append("</b><br>Travel date: <b>")
         .append(travelDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))).append("</b></font></td></tr></table>");

        h.append("<table width='100%' cellpadding='8' cellspacing='0' border='0'>")
         .append("<tr bgcolor='#e8f1fc'><td><b>Description</b></td><td align='right'><b>Qty</b></td>")
         .append("<td align='right'><b>Rate (Rs.)</b></td><td align='right'><b>Amount (Rs.)</b></td></tr>")
         .append("<tr><td>").append(esc(packageName)).append("</td><td align='right'>").append(people)
         .append("</td><td align='right'>").append(money(rate)).append("</td><td align='right'>")
         .append(money(amt[0])).append("</td></tr>")
         .append("<tr><td colspan='4'><hr></td></tr>")
         .append("<tr><td colspan='3' align='right'>Subtotal</td><td align='right'>").append(money(amt[0])).append("</td></tr>")
         .append("<tr><td colspan='3' align='right'>").append(gstLabel()).append("</td><td align='right'>")
         .append(money(amt[1])).append("</td></tr>")
         .append("<tr bgcolor='#eaf7ee'><td colspan='3' align='right'><b>Total Paid / Payable</b></td>")
         .append("<td align='right'><font size='4' color='#1b7a3a'><b>Rs. ").append(money(amt[2])).append("</b></font></td></tr>")
         .append("</table>");

        h.append("<table width='100%' cellpadding='10' cellspacing='0'><tr><td>")
         .append("<font size='2' color='#78879f'>PAYMENT</font><br>").append(payText).append("</td></tr>")
         .append("<tr><td><font size='2' color='#78879f'>Thank you for choosing Wanderly Travels. ")
         .append("Please carry a copy of this invoice and a valid photo ID on your trip.</font></td></tr></table>");

        h.append("</body></html>");
        return h.toString();
    }

    private void showInvoice(String html, String fileName) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog d = new JDialog(owner, "Booking Confirmed - Invoice", Dialog.ModalityType.APPLICATION_MODAL);

        JEditorPane pane = new JEditorPane("text/html", html);
        pane.setEditable(false);
        JScrollPane sp = new JScrollPane(pane);
        sp.setPreferredSize(new Dimension(660, 480));
        sp.setBorder(BorderFactory.createEmptyBorder());

        RoundedButton saveBtn = new RoundedButton("Save Invoice", Theme.GOLD_ACCENT, Theme.NAVY);
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File(fileName));
            if (fc.showSaveDialog(d) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".html")) f = new File(f.getPath() + ".html");
                try {
                    java.nio.file.Files.write(f.toPath(), html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    JOptionPane.showMessageDialog(d, "Invoice saved:\n" + f.getAbsolutePath()
                            + "\n\nOpen it in a browser to print or save as PDF.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (java.io.IOException ex) {
                    JOptionPane.showMessageDialog(d, "Could not save the invoice:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        RoundedButton printBtn = new RoundedButton("Print", Theme.ACCENT_BLUE, Color.WHITE);
        printBtn.addActionListener(e -> {
            try {
                pane.print();
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(d, "Could not print:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        RoundedButton closeBtn = new RoundedButton("Close", new Color(225, 231, 242), Theme.NAVY);
        closeBtn.addActionListener(e -> d.dispose());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bar.setBackground(Color.WHITE);
        bar.add(printBtn);
        bar.add(saveBtn);
        bar.add(closeBtn);

        d.getContentPane().setLayout(new BorderLayout());
        d.getContentPane().add(sp, BorderLayout.CENTER);
        d.getContentPane().add(bar, BorderLayout.SOUTH);
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    // =====================================================================
    //  Tiny QR-code generator (no external library needed).
    //  Byte mode, error correction level M, versions 1-10 (up to ~213 characters).
    // =====================================================================
    static final class QrEncoder {
        private static final int[] ECC_PER_BLOCK = {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26};
        private static final int[] NUM_BLOCKS    = {-1,  1,  1,  1,  2,  2,  4,  4,  4,  5,  5};

        private final int version, size;
        private final boolean[][] modules;
        private final boolean[][] isFunction;

        /** Returns the QR code as a square grid (true = dark module). */
        static boolean[][] encode(String text) {
            byte[] data = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            int ver = -1;
            for (int v = 1; v <= 10; v++) {
                int ccBits = v <= 9 ? 8 : 16;
                int used = 4 + ccBits + data.length * 8;
                if (used <= numDataCodewords(v) * 8) { ver = v; break; }
            }
            if (ver < 0) throw new IllegalArgumentException("Text too long for QR code");
            return new QrEncoder(ver, data).modules;
        }

        private static int numRawDataModules(int ver) {
            int result = (16 * ver + 128) * ver + 64;
            if (ver >= 2) {
                int numAlign = ver / 7 + 2;
                result -= (25 * numAlign - 10) * numAlign - 55;
                if (ver >= 7) result -= 36;
            }
            return result;
        }

        private static int numDataCodewords(int ver) {
            return numRawDataModules(ver) / 8 - ECC_PER_BLOCK[ver] * NUM_BLOCKS[ver];
        }

        private QrEncoder(int ver, byte[] text) {
            version = ver;
            size = ver * 4 + 17;
            modules = new boolean[size][size];
            isFunction = new boolean[size][size];

            // ---- data bits ----
            java.util.List<Boolean> bits = new java.util.ArrayList<>();
            appendBits(bits, 0x4, 4);                                   // byte mode
            appendBits(bits, text.length, ver <= 9 ? 8 : 16);           // character count
            for (byte b : text) appendBits(bits, b & 0xFF, 8);
            int capacityBits = numDataCodewords(ver) * 8;
            appendBits(bits, 0, Math.min(4, capacityBits - bits.size())); // terminator
            while (bits.size() % 8 != 0) bits.add(false);
            for (int pad = 0xEC; bits.size() < capacityBits; pad ^= 0xEC ^ 0x11) appendBits(bits, pad, 8);

            byte[] dataCodewords = new byte[bits.size() / 8];
            for (int i = 0; i < bits.size(); i++) {
                if (bits.get(i)) dataCodewords[i >>> 3] |= 1 << (7 - (i & 7));
            }

            // ---- draw ----
            drawFunctionPatterns();
            drawCodewords(addEccAndInterleave(dataCodewords));

            int bestMask = 0;
            int minPenalty = Integer.MAX_VALUE;
            for (int m = 0; m < 8; m++) {
                applyMask(m);
                drawFormatBits(m);
                int p = penaltyScore();
                if (p < minPenalty) { minPenalty = p; bestMask = m; }
                applyMask(m);                                           // undo
            }
            applyMask(bestMask);
            drawFormatBits(bestMask);
        }

        private static void appendBits(java.util.List<Boolean> list, int value, int len) {
            for (int i = len - 1; i >= 0; i--) list.add(((value >>> i) & 1) != 0);
        }

        private static boolean bit(int x, int i) { return ((x >>> i) & 1) != 0; }

        private void setFunction(int x, int y, boolean dark) {
            modules[y][x] = dark;
            isFunction[y][x] = true;
        }

        private void drawFunctionPatterns() {
            for (int i = 0; i < size; i++) {
                setFunction(6, i, i % 2 == 0);
                setFunction(i, 6, i % 2 == 0);
            }
            drawFinder(3, 3);
            drawFinder(size - 4, 3);
            drawFinder(3, size - 4);

            int[] pos = alignmentPositions();
            int n = pos.length;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if ((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0)) continue;
                    drawAlignment(pos[i], pos[j]);
                }
            }
            drawFormatBits(0);
            drawVersion();
        }

        private int[] alignmentPositions() {
            if (version == 1) return new int[0];
            int numAlign = version / 7 + 2;
            int step = (version * 4 + numAlign * 2 + 1) / (numAlign * 2 - 2) * 2;
            int[] result = new int[numAlign];
            result[0] = 6;
            for (int i = numAlign - 1, pos = version * 4 + 10; i >= 1; i--, pos -= step) result[i] = pos;
            return result;
        }

        private void drawFinder(int cx, int cy) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dx = -4; dx <= 4; dx++) {
                    int dist = Math.max(Math.abs(dx), Math.abs(dy));
                    int x = cx + dx, y = cy + dy;
                    if (x >= 0 && x < size && y >= 0 && y < size) setFunction(x, y, dist != 2 && dist != 4);
                }
            }
        }

        private void drawAlignment(int cx, int cy) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dx = -2; dx <= 2; dx++) {
                    setFunction(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
                }
            }
        }

        private void drawFormatBits(int mask) {
            int data = (0 /* level M */ << 3) | mask;
            int rem = data;
            for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
            int bits = ((data << 10) | rem) ^ 0x5412;

            for (int i = 0; i <= 5; i++) setFunction(8, i, bit(bits, i));
            setFunction(8, 7, bit(bits, 6));
            setFunction(8, 8, bit(bits, 7));
            setFunction(7, 8, bit(bits, 8));
            for (int i = 9; i < 15; i++) setFunction(14 - i, 8, bit(bits, i));

            for (int i = 0; i < 8; i++) setFunction(size - 1 - i, 8, bit(bits, i));
            for (int i = 8; i < 15; i++) setFunction(8, size - 15 + i, bit(bits, i));
            setFunction(8, size - 8, true);
        }

        private void drawVersion() {
            if (version < 7) return;
            int rem = version;
            for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
            int bits = (version << 12) | rem;
            for (int i = 0; i < 18; i++) {
                boolean b = bit(bits, i);
                int a = size - 11 + i % 3;
                int c = i / 3;
                setFunction(a, c, b);
                setFunction(c, a, b);
            }
        }

        // ---- error correction ----
        private byte[] addEccAndInterleave(byte[] data) {
            int numBlocks = NUM_BLOCKS[version];
            int blockEccLen = ECC_PER_BLOCK[version];
            int rawCodewords = numRawDataModules(version) / 8;
            int numShortBlocks = numBlocks - rawCodewords % numBlocks;
            int shortBlockLen = rawCodewords / numBlocks;

            byte[][] blocks = new byte[numBlocks][];
            byte[] divisor = rsDivisor(blockEccLen);
            for (int i = 0, k = 0; i < numBlocks; i++) {
                int datLen = shortBlockLen - blockEccLen + (i < numShortBlocks ? 0 : 1);
                byte[] dat = java.util.Arrays.copyOfRange(data, k, k + datLen);
                k += datLen;
                byte[] ecc = rsRemainder(dat, divisor);
                byte[] block = new byte[shortBlockLen + 1];
                System.arraycopy(dat, 0, block, 0, dat.length);
                System.arraycopy(ecc, 0, block, block.length - blockEccLen, ecc.length);
                blocks[i] = block;
            }

            byte[] result = new byte[rawCodewords];
            for (int i = 0, k = 0; i < blocks[0].length; i++) {
                for (int j = 0; j < blocks.length; j++) {
                    if (i != shortBlockLen - blockEccLen || j >= numShortBlocks) result[k++] = blocks[j][i];
                }
            }
            return result;
        }

        private static byte[] rsDivisor(int degree) {
            byte[] result = new byte[degree];
            result[degree - 1] = 1;
            int root = 1;
            for (int i = 0; i < degree; i++) {
                for (int j = 0; j < degree; j++) {
                    result[j] = (byte) gfMultiply(result[j] & 0xFF, root);
                    if (j + 1 < degree) result[j] ^= result[j + 1];
                }
                root = gfMultiply(root, 0x02);
            }
            return result;
        }

        private static byte[] rsRemainder(byte[] data, byte[] divisor) {
            byte[] result = new byte[divisor.length];
            for (byte b : data) {
                int factor = (b ^ result[0]) & 0xFF;
                System.arraycopy(result, 1, result, 0, result.length - 1);
                result[result.length - 1] = 0;
                for (int i = 0; i < result.length; i++) result[i] ^= gfMultiply(divisor[i] & 0xFF, factor);
            }
            return result;
        }

        private static int gfMultiply(int x, int y) {
            int z = 0;
            for (int i = 7; i >= 0; i--) {
                z = (z << 1) ^ ((z >>> 7) * 0x11D);
                z ^= ((y >>> i) & 1) * x;
            }
            return z;
        }

        // ---- placing data + masking ----
        private void drawCodewords(byte[] data) {
            int i = 0;
            for (int right = size - 1; right >= 1; right -= 2) {
                if (right == 6) right = 5;
                for (int vert = 0; vert < size; vert++) {
                    for (int j = 0; j < 2; j++) {
                        int x = right - j;
                        boolean upward = ((right + 1) & 2) == 0;
                        int y = upward ? size - 1 - vert : vert;
                        if (!isFunction[y][x] && i < data.length * 8) {
                            modules[y][x] = bit(data[i >>> 3], 7 - (i & 7));
                            i++;
                        }
                    }
                }
            }
        }

        private void applyMask(int mask) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    boolean invert;
                    switch (mask) {
                        case 0: invert = (x + y) % 2 == 0; break;
                        case 1: invert = y % 2 == 0; break;
                        case 2: invert = x % 3 == 0; break;
                        case 3: invert = (x + y) % 3 == 0; break;
                        case 4: invert = (x / 3 + y / 2) % 2 == 0; break;
                        case 5: invert = x * y % 2 + x * y % 3 == 0; break;
                        case 6: invert = (x * y % 2 + x * y % 3) % 2 == 0; break;
                        default: invert = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                    }
                    if (invert && !isFunction[y][x]) modules[y][x] = !modules[y][x];
                }
            }
        }

        /** Penalty used only to choose the mask that scans best (same rules as the QR standard). */
        private int penaltyScore() {
            int result = 0;
            // rule 1: runs of 5+ same-colour modules, rows and columns
            for (int pass = 0; pass < 2; pass++) {
                for (int a = 0; a < size; a++) {
                    int run = 1;
                    for (int b = 1; b < size; b++) {
                        boolean cur = pass == 0 ? modules[a][b] : modules[b][a];
                        boolean prev = pass == 0 ? modules[a][b - 1] : modules[b - 1][a];
                        if (cur == prev) {
                            run++;
                            if (run == 5) result += 3; else if (run > 5) result++;
                        } else {
                            run = 1;
                        }
                    }
                }
            }
            // rule 2: 2x2 blocks of one colour
            for (int y = 0; y < size - 1; y++) {
                for (int x = 0; x < size - 1; x++) {
                    boolean c = modules[y][x];
                    if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) result += 3;
                }
            }
            // rule 3: finder-like patterns
            int[] p1 = {1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0};
            int[] p2 = {0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 1};
            for (int pass = 0; pass < 2; pass++) {
                for (int a = 0; a < size; a++) {
                    for (int b = 0; b + 11 <= size; b++) {
                        boolean m1 = true, m2 = true;
                        for (int k = 0; k < 11 && (m1 || m2); k++) {
                            int v = (pass == 0 ? modules[a][b + k] : modules[b + k][a]) ? 1 : 0;
                            if (v != p1[k]) m1 = false;
                            if (v != p2[k]) m2 = false;
                        }
                        if (m1 || m2) result += 40;
                    }
                }
            }
            // rule 4: dark / light balance
            int dark = 0;
            for (boolean[] row : modules) for (boolean m : row) if (m) dark++;
            int total = size * size;
            int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
            return result + Math.max(k, 0) * 10;
        }
    }

    /** Draws a QR code (with the white border scanners need) at the biggest whole-pixel size that fits. */
    private static class QrPanel extends JPanel {
        private final boolean[][] qr;

        QrPanel(boolean[][] qr, int px) {
            this.qr = qr;
            setPreferredSize(new Dimension(px, px));
            setMinimumSize(new Dimension(px, px));
            setMaximumSize(new Dimension(px, px));
            setBackground(Color.WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int n = qr.length;
            int quiet = 4;
            int scale = Math.max(1, Math.min(getWidth(), getHeight()) / (n + quiet * 2));
            int total = scale * (n + quiet * 2);
            int ox = (getWidth() - total) / 2 + quiet * scale;
            int oy = (getHeight() - total) / 2 + quiet * scale;
            g.setColor(Color.WHITE);
            g.fillRect(ox - quiet * scale, oy - quiet * scale, total, total);
            g.setColor(Color.BLACK);
            for (int y = 0; y < n; y++) {
                for (int x = 0; x < n; x++) {
                    if (qr[y][x]) g.fillRect(ox + x * scale, oy + y * scale, scale, scale);
                }
            }
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}