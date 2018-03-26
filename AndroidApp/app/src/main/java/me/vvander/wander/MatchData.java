package me.vvander.wander;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Kyle on 3/21/2018.
 */

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
    //TODO: add any other info about matches as necessary


    public void setName(String name) {this.name = name;}
    public void setInterests(String interests) {this.interests = interests;}
    public void setAbout(String about) {this.about = about;}
    public void setLocation(String location) {this.location = location;}
    public void setEmail(String email) {this.email = email;}
    public void setUserId(String userId) {this.userId = userId;}
    public void setPicture(String picture) {this.picture = picture;}
    public void setNumPathCrosses(int numPathCrosses) {this.numPathCrosses = numPathCrosses;}
    public void setApproved(boolean approved) {this.approved = approved;}
    public void setLatitudes(int[] latitudes) {this.latitudes = latitudes;}
    public void setLongitudes(int[] longitudes) {this.longitudes = longitudes;}
    public void setMatchTime(Date matchTime) {this.matchTime = matchTime;}

    public String getName() {return name;}
    public String getInterests() {return interests;}
    public String getAbout() {return about;}
    public String getLocation() {return location;}
    public String getEmail() {return email;}
    public String getPicture() {return picture;}
    public String getUserId() {return userId;}
    public int getNumPathCrosses() {return numPathCrosses;}
    public boolean getApproved() {return approved;}
    public Date getMatchTime() {return matchTime;}
    public int[] getLatitudes() {return latitudes;}
    public int[] getLongitudes() {return longitudes;}
}
