package org.example.sici1.controller;

public class UserSession {
    private static UserSession instance;
    private String username;
    private String role;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void setUser(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public void clear() {
        username = null;
        role = null;
        instance = null;
    }
}
