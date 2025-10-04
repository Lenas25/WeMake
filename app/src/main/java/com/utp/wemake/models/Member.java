package com.utp.wemake.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Member {
    private String role;
    private int points;

    @ServerTimestamp
    private Date joinDate;

    public Member() {}

    public Member(String role, int points) {
        this.role = role;
        this.points = points;
    }


    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public Date getJoinDate() { return joinDate; }
    public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}