package com.ktuedu.rtMessaging.Models;

public class User implements  Comparable<User> {

    public String Name;
    public String Photo;
    private String ID;

    public User(String id, String name, String imageId) {
        Name = name;
        Photo = imageId;
        ID = id;
    }

    public String getID() {
        return ID;
    }

    @Override
    public int compareTo(User o) {
        return this.Name.toLowerCase().compareTo(o.Name.toLowerCase());
    }

}
