
        package ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class OrderSummary {

    private static final BigDecimal GST_RATE = new BigDecimal("0.05");

    private final String invoiceNumber;
    private final String customerName;
    private final String packageName;
    private final LocalDate travelDate;
    private final int people;
    private final BigDecimal pricePerPerson;

    // Payment details (PaymentDialog yahan save karta hai)
    private String paymentMethod = "NOT PAID";
    private String paymentReference = "";
    private boolean paid = false;

    public OrderSummary(String invoiceNumber, String customerName, String packageName,
                        LocalDate travelDate, int people, BigDecimal pricePerPerson) {
        this.invoiceNumber = invoiceNumber;
        this.customerName = customerName;
        this.packageName = packageName;
        this.travelDate = travelDate;
        this.people = people;
        this.pricePerPerson = pricePerPerson;
    }

    // ------------------------------------------------------------------
    // Amounts
    // ------------------------------------------------------------------

    public BigDecimal subtotal() {
        return pricePerPerson.multiply(BigDecimal.valueOf(people))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal gst() {
        return subtotal().multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal total() {
        return subtotal().add(gst()).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // Payment
    // ------------------------------------------------------------------

    public void markPayment(String method, String reference, boolean paid) {
        this.paymentMethod = method;
        this.paymentReference = reference;
        this.paid = paid;
    }

    public String getPaymentMethod()    { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public boolean isPaid()             { return paid; }
    public String getPaymentStatus()    { return paid ? "PAID" : "PAYMENT PENDING"; }

    // ------------------------------------------------------------------
    // Getters (PaymentDialog / InvoiceDialog ke liye)
    // ------------------------------------------------------------------

    public String invoiceNumber()         { return invoiceNumber; }
    public String getInvoiceNumber()      { return invoiceNumber; }
    public String getCustomerName()       { return customerName; }
    public String getPackageName()        { return packageName; }
    public LocalDate getTravelDate()      { return travelDate; }
    public int getPeople()                { return people; }
    public BigDecimal getPricePerPerson() { return pricePerPerson; }

    public BigDecimal getSubtotal() { return subtotal(); }
    public BigDecimal getGst()      { return gst(); }
    public BigDecimal getTotal()    { return total(); }
}