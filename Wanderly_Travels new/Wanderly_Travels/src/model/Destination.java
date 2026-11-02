package model;

/**
 * Represents one row of the "destinations" table.
 */
public class Destination {
    private int destinationId;
    private String name;
    private String location;
    private String description;
    private String touristInfo;
    private String imagePath;

    public Destination() {
    }

    public Destination(int destinationId, String name, String location, String description,
                        String touristInfo, String imagePath) {
        this.destinationId = destinationId;
        this.name = name;
        this.location = location;
        this.description = description;
        this.touristInfo = touristInfo;
        this.imagePath = imagePath;
    }

    public int getDestinationId() {
        return destinationId;
    }

    public void setDestinationId(int destinationId) {
        this.destinationId = destinationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTouristInfo() {
        return touristInfo;
    }

    public void setTouristInfo(String touristInfo) {
        this.touristInfo = touristInfo;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    @Override
    public String toString() {
        return name;
    }
}
