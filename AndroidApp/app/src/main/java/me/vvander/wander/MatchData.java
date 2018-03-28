package me.vvander.wander;

import java.util.Date;

public class MatchData {
    private String name;
    private String interests;
    private String about;
    private String location; //This is the location displayed in profile
    private String email;
    private String picture;
    private String userId;

    private int numPathCrosses;
    private Date matchTime;
    private int[] latitudes; //An array of the latitude data for all path crosses (if you think there is a better way
    private int[] longitudes; //to store location data go for it)
    private boolean approved;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = about;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getNumPathCrosses() {
        return numPathCrosses;
    }

    public void setNumPathCrosses(int numPathCrosses) {
        this.numPathCrosses = numPathCrosses;
    }

    public boolean getApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Date getMatchTime() {
        return matchTime;
    }

    public void setMatchTime(Date matchTime) {
        this.matchTime = matchTime;
    }

    public int[] getLatitudes() {
        return latitudes;
    }

    public void setLatitudes(int[] latitudes) {
        this.latitudes = latitudes;
    }

    public int[] getLongitudes() {
        return longitudes;
    }

    public void setLongitudes(int[] longitudes) {
        this.longitudes = longitudes;
    }
}