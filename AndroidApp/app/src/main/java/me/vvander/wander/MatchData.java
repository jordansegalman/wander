package me.vvander.wander;

public class MatchData {
    private String uid;
    private String name;
    private String about;
    private String interests;
    private String picture;
    private int timesCrossed;
    private boolean approved;

    MatchData(String uid, String name, String about, String interests, String picture, int timesCrossed, boolean approved) {
        this.uid = uid;
        this.name = name;
        this.about = about;
        this.interests = interests;
        this.picture = picture;
        this.timesCrossed = timesCrossed;
        this.approved = approved;
    }

    public String getUserID() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getAbout() {
        return about;
    }

    public String getInterests() {
        return interests;
    }

    public String getPicture() {
        return picture;
    }

    public int getTimesCrossed() {
        return timesCrossed;
    }

    public boolean getApproved() {
        return approved;
    }
}