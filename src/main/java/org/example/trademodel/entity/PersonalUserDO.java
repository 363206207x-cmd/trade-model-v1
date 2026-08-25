package org.example.trademodel.entity;

import java.time.LocalDateTime;

public class PersonalUserDO {
    private Long id;
    private String username;
    private String passwordHash;
    private String role;
    private Boolean enabled;
    private Long sessionVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLoginAt;
    private LocalDateTime disabledAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Long getSessionVersion() { return sessionVersion; }
    public void setSessionVersion(Long sessionVersion) { this.sessionVersion = sessionVersion; }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getDisabledAt() { return disabledAt; }
    public void setDisabledAt(LocalDateTime disabledAt) { this.disabledAt = disabledAt; }
}
