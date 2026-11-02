
    
package ui;

/**
 * Holds the destination / package the user picked on the Explore Tours page,
 * so the Booking page can show and use it.
 */
public class BookingSelection {
    private static String destination = "";
    private static String location = "";
    private static String packageName = "";
    private static String duration = "";
    private static double price = 0;
    private static boolean set = false;

    private BookingSelection() {}

    public static void set(String destination, String location, String packageName, String duration, double price) {
        BookingSelection.destination = destination;
        BookingSelection.location = location;
        BookingSelection.packageName = packageName;
        BookingSelection.duration = duration;
        BookingSelection.price = price;
        BookingSelection.set = true;
    }

    public static boolean isSet()          { return set; }
    public static String getDestination()  { return destination; }
    public static String getLocation()     { return location; }
    public static String getPackageName()  { return packageName; }
    public static String getDuration()     { return duration; }
    public static double getPrice()        { return price; }

    public static void clear() {
        destination = ""; location = ""; packageName = ""; duration = ""; price = 0; set = false;
    }
}
