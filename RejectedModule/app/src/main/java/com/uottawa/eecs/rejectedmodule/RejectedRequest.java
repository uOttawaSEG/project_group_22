package com.uottawa.eecs.rejectedmodule;

public class RejectedRequest {
    private int id;
    private String username;
    private String role;
    private String rejectReason;

    public RejectedRequest(int id, String username, String role, String rejectReason) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.rejectReason = rejectReason;
    }

    // getter method
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getRejectReason() { return rejectReason; }
}
