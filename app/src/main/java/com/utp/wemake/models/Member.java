package com.utp.wemake.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Modelo de datos para representar un miembro del tablero
 * Siguiendo las mejores prácticas de Firebase y Android
 */
public class Member {
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;
    private boolean added;
    private long timestamp;
    private String boardId;

    // Constructor vacío requerido por Firebase
    public Member() {
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor principal
    public Member(String id, String name, String email, String avatarUrl, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.added = false;
        this.timestamp = System.currentTimeMillis();
    }

    // Constructor completo
    public Member(String id, String name, String email, String avatarUrl, String role, 
                  boolean added, long timestamp, String boardId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.added = added;
        this.timestamp = timestamp;
        this.boardId = boardId;
    }

    // Getters y Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isAdded() { return added; }
    public void setAdded(boolean added) { this.added = added; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }

    /**
     * Convierte el objeto Member a un Map para Firebase
     * @return Map con los datos del miembro
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("email", email);
        map.put("avatarUrl", avatarUrl);
        map.put("role", role);
        map.put("added", added);
        map.put("timestamp", timestamp);
        map.put("boardId", boardId);
        return map;
    }

    /**
     * Crea un objeto Member desde un Map de Firebase
     * @param map Map con los datos del miembro
     * @return Objeto Member
     */
    public static Member fromMap(Map<String, Object> map) {
        Member member = new Member();
        member.setId((String) map.get("id"));
        member.setName((String) map.get("name"));
        member.setEmail((String) map.get("email"));
        member.setAvatarUrl((String) map.get("avatarUrl"));
        member.setRole((String) map.get("role"));
        member.setAdded((Boolean) map.getOrDefault("added", false));
        member.setTimestamp((Long) map.getOrDefault("timestamp", System.currentTimeMillis()));
        member.setBoardId((String) map.get("boardId"));
        return member;
    }

    /**
     * Verifica si el miembro es administrador
     * @return true si es admin, false si es usuario
     */
    public boolean isAdmin() {
        return "admin".equals(role);
    }

    /**
     * Verifica si el miembro es usuario regular
     * @return true si es usuario, false si es admin
     */
    public boolean isUser() {
        return "user".equals(role);
    }

    @Override
    public String toString() {
        return "Member{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role='" + role + '\'' +
                ", added=" + added +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Member member = (Member) obj;
        return id != null ? id.equals(member.id) : member.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
