package me.vvander.wander;

public class TagData {
    private String tagTitle = "Enter a Tag Location";
    private String tagReview = "Enter a review or description of the location.";
    private String tagImage;

    public String getTagTitle() {
        return tagTitle;
    }

    public String getTagReview() {
        return tagReview;
    }

    public String getTagImage() {
        return tagImage;
    }

    public void setTagTitle(String tagTitle) {
        this.tagTitle = tagTitle;
    }

    public void setTagReview(String tagReview) {
        this.tagReview = tagReview;
    }

    public void setTagImage(String tagImage) {
        this.tagImage = tagImage;
    }
}
