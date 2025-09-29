package com.utp.wemake.models;

public class User {
    private String userid;
    private String name;
    private String publicName;
    private String email;
    private String photoUrl;
    private String phone;
    private String birthDate;

    public User() {
        // Constructor vacío requerido para Firestore
    }

    public User(String userid, String name, String publicName, String email, String photoUrl, String phone, String birthDate) {
        this.userid = userid;
        this.name = name;
        this.publicName = publicName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.phone = phone;
        this.birthDate = birthDate;
    }

    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPublicName() { return publicName; }
    public void setPublicName(String publicName) { this.publicName = publicName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
}