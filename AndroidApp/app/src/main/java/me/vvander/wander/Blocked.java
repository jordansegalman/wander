package me.vvander.wander;

class Blocked {
    private String uid;
    private String name;
    private String picture;

    Blocked(String uid, String name, String picture) {
        this.uid = uid;
        this.name = name;
        this.picture = picture;
    }

    public String getUserID() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public String getPicture() {
        return picture;
    }
}