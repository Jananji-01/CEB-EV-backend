package com.example.EVProject.dto;

import java.util.Set;

public class ApiResponse {
    private boolean success;
    private String message;
    private String token;
    private Set<String> roles;
    private String username;
    private String eAccountNo;

    // ✅ Login response constructor (token + roles + username + account)
    public ApiResponse(boolean success, String message, String token, Set<String> roles, String username, String eAccountNo) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.roles = roles;
        this.username = username;
        this.eAccountNo = eAccountNo;
    }

    // ✅ Old constructor (token + roles only) - keep if you need
    public ApiResponse(boolean success, String message, String token, Set<String> roles) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.roles = roles;
    }

    // ✅ Simple constructor (success + message)
    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // ✅ Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getToken() { return token; }
    public Set<String> getRoles() { return roles; }
    public String getUsername() { return username; }
    public String getEAccountNo() { return eAccountNo; }

    // ✅ Setters (optional but useful)
    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setToken(String token) { this.token = token; }
    public void setRoles(Set<String> roles) { this.roles = roles; }
    public void setUsername(String username) { this.username = username; }
    public void setEAccountNo(String eAccountNo) { this.eAccountNo = eAccountNo; }
}
