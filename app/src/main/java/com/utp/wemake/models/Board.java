package com.utp.wemake.models;

import com.google.firebase.firestore.DocumentId;
import java.util.List;

public class Board {
    @DocumentId
    private String id;

    private String name;
    private String description;
    private String color;
    private List<String> members;
    private String invitationCode;

    public Board() {
        // Constructor vacío para Firestore
    }

    public Board(String name, String description, String color, List<String> members) {
        this.name = name;
        this.description = description;
        this.color = color;
        this.members = members;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }
    public String getInvitationCode() { return invitationCode; }
    public void setInvitationCode(String invitationCode) { this.invitationCode = invitationCode; }
}