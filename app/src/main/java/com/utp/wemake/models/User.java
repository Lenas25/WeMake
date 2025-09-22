package com.utp.wemake.models;

public class User {
    private String userid;
    private String name;
    private String email;
    private String photoUrl;

    public User() {
        // Constructor predeterminado requerido para Firestore
    }

    public User(String userid, String name, String email, String photoUrl) {
        this.userid = userid;
        this.name = name;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    // Getters and setters
    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
