package com.illbethere.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String googleSub;

    private String name;
    private String email;
    private String avatarUrl;

    @Column(length = 2048)
    private String encryptedRefreshToken;

    @Column(length = 2048)
    private String encryptedAccessToken;

    private java.time.Instant accessTokenExpiresAt;

    public Long getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getEncryptedRefreshToken() {
        return encryptedRefreshToken;
    }

    public void setEncryptedRefreshToken(String encryptedRefreshToken) {
        this.encryptedRefreshToken = encryptedRefreshToken;
    }

    public String getEncryptedAccessToken() {
        return encryptedAccessToken;
    }

    public void setEncryptedAccessToken(String encryptedAccessToken) {
        this.encryptedAccessToken = encryptedAccessToken;
    }

    public java.time.Instant getAccessTokenExpiresAt() {
        return accessTokenExpiresAt;
    }

    public void setAccessTokenExpiresAt(java.time.Instant accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public boolean hasCalendarToken() {
        return (encryptedRefreshToken != null && !encryptedRefreshToken.isBlank())
                || (encryptedAccessToken != null && !encryptedAccessToken.isBlank());
    }
}
