package me.vvander.wander;

public class LocationTag {
    private String title;
    private String description;

    LocationTag() {
        title = "Location Tag Title";
        description = "Enter a description for the tag.";
    }

    LocationTag(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}