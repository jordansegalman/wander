package me.vvander.wander;

public class Match implements Comparable<Match> {
    private String uid;
    private String name;
    private String about;
    private String interests;
    private String picture;
    private int timesCrossed;
    private boolean approved;

    Match(String uid, String name, String about, String interests, String picture, int timesCrossed, boolean approved) {
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

    @Override
    public int compareTo(Match match) {
        if (this.timesCrossed < match.timesCrossed) {
            return 1;
        }
        return -1;
    }
}