package user_api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
// hola 4
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "google_subject", unique = true)
    private String googleSubject;

    @Column(name = "is_premium", nullable = false)
    private Boolean isPremium;

    @Column(name = "premium_since")
    private LocalDateTime premiumSince;

    @Column(name = "premium_until")
    private LocalDateTime premiumUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_daily_coins_claimed_at")
    private LocalDateTime lastDailyCoinsClaimedAt;

    @Column
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(name = "session_token")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String sessionToken;

    @Transient
    private String accessToken;

    @Transient
    private String tokenType;

    public User() {}

    public User(
            Integer userId,
            String username,
            String email,
            Boolean isPremium,
            LocalDateTime premiumSince,
            LocalDateTime premiumUntil,
            LocalDateTime createdAt,
            LocalDateTime lastDailyCoinsClaimedAt,
            String password) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.isPremium = isPremium;
        this.premiumSince = premiumSince;
        this.premiumUntil = premiumUntil;
        this.createdAt = createdAt;
        this.lastDailyCoinsClaimedAt = lastDailyCoinsClaimedAt;
        this.password = password;
        this.accountType = "LOCAL";
    }

    public User(
            Integer userId,
            String username,
            String email,
            Boolean isPremium,
            LocalDateTime premiumSince,
            LocalDateTime premiumUntil,
            LocalDateTime createdAt,
            String password) {
        this(
                userId,
                username,
                email,
                isPremium,
                premiumSince,
                premiumUntil,
                createdAt,
                null,
                password);
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (isPremium == null) {
            isPremium = Boolean.FALSE;
        }

        if (accountType == null || accountType.isBlank()) {
            accountType = "LOCAL";
        }

        if (Boolean.TRUE.equals(isPremium) && premiumSince == null) {
            premiumSince = createdAt;
        }

        if (Boolean.FALSE.equals(isPremium)) {
            premiumSince = null;
            premiumUntil = null;
        }
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public void setGoogleSubject(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean premium) {
        isPremium = premium;
    }

    public LocalDateTime getPremiumSince() {
        return premiumSince;
    }

    public void setPremiumSince(LocalDateTime premiumSince) {
        this.premiumSince = premiumSince;
    }

    public LocalDateTime getPremiumUntil() {
        return premiumUntil;
    }

    public void setPremiumUntil(LocalDateTime premiumUntil) {
        this.premiumUntil = premiumUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastDailyCoinsClaimedAt() {
        return lastDailyCoinsClaimedAt;
    }

    public void setLastDailyCoinsClaimedAt(LocalDateTime lastDailyCoinsClaimedAt) {
        this.lastDailyCoinsClaimedAt = lastDailyCoinsClaimedAt;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
