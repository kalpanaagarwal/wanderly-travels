package ui;

import java.awt.Color;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Shared formatting helpers used by the booking, checkout and invoice screens. */
final class Formats {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private Formats() {
    }

    /**
     * Formats an amount as "Rs. 12,34,567.50". Indian digit grouping (last three
     * digits, then pairs) is applied by hand because java.text.DecimalFormat only
     * supports a single, uniform grouping size.
     */
    static String money(BigDecimal amount) {
        String plain = amount.setScale(2, RoundingMode.HALF_UP).abs().toPlainString();
        int dot = plain.indexOf('.');
        String whole = plain.substring(0, dot);
        String fraction = plain.substring(dot);

        StringBuilder grouped = new StringBuilder();
        int head = whole.length() - 3;
        if (head > 0) {
            for (int i = 0; i < head; i++) {
                if (i > 0 && (head - i) % 2 == 0) {
                    grouped.append(',');
                }
                grouped.append(whole.charAt(i));
            }
            grouped.append(',').append(whole, head, whole.length());
        } else {
            grouped.append(whole);
        }
        return (amount.signum() < 0 ? "-Rs. " : "Rs. ") + grouped + fraction;
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }

    static String dateTime(LocalDateTime dateTime) {
        return DATE_TIME.format(dateTime);
    }

    /** Converts a colour to a CSS hex string such as "#0B2545". */
    static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    /** Escapes text so it can be embedded safely in HTML. */
    static String html(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (char c : text.toCharArray()) {
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }
}