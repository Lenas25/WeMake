package com.utp.wemake.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Member {

    private String role;
    private int points;

    @ServerTimestamp
    private Date joinDate;

    // --- Constructores ---
    public Member() {}

    public Member(String role, int points) {
        this.role = role;
        this.points = points;
    }

    // Rol del miembro en el tablero
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Puntos del miembro
    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    // Fecha de unión
    public Date getJoinDate() { return joinDate; }
    public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }

    @Exclude
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}