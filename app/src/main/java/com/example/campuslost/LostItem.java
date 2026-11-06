package com.example.campuslost;

public class LostItem {

    private String itemId;
    private String title;
    private String description;
    private String location;
    private String type;
    private String userId;
    private String imageUrl;
    private String posterName;
    private String contactNumber;
    private long timestamp;

    public LostItem() {
        // Required for Firebase
    }

    public LostItem(String itemId, String title,
                    String description, String location,
                    String type, String userId,
                    long timestamp, String imageUrl,
                    String posterName, String contactNumber) {

        this.itemId = itemId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.type = type;
        this.userId = userId;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
        this.posterName = posterName;
        this.contactNumber = contactNumber;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getPosterName() { return posterName; }
    public void setPosterName(String posterName) { this.posterName = posterName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}