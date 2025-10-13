package com.utp.wemake.models;

import com.google.firebase.firestore.DocumentId;

import java.io.Serializable;

public class Coupon implements Serializable {
    @DocumentId
    private String id;
    private String title;
    private String description;
    private int cost;

    public Coupon() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
}