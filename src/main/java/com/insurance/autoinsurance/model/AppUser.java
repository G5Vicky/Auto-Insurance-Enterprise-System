package com.insurance.autoinsurance.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true)
})
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id") private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.ROLE_USER;

    @Column(name = "full_name", length = 120) private String fullName;
    @Column(name = "email", length = 200)     private String email;
    @Column(name = "enabled", nullable = false) private boolean enabled = true;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "last_login") private LocalDateTime lastLogin;

    @PrePersist protected void pre() { createdAt = LocalDateTime.now(); }

    public enum Role { ROLE_ADMIN, ROLE_USER }

    public AppUser() {}
    public Long getUserId()               { return userId; }
    public void setUserId(Long v)         { userId = v; }
    public String getUsername()           { return username; }
    public void setUsername(String v)     { username = v; }
    public String getPasswordHash()       { return passwordHash; }
    public void setPasswordHash(String v) { passwordHash = v; }
    public Role getRole()                 { return role; }
    public void setRole(Role v)           { role = v; }
    public String getFullName()           { return fullName; }
    public void setFullName(String v)     { fullName = v; }
    public String getEmail()              { return email; }
    public void setEmail(String v)        { email = v; }
    public boolean isEnabled()            { return enabled; }
    public void setEnabled(boolean v)     { enabled = v; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getLastLogin()   { return lastLogin; }
    public void setLastLogin(LocalDateTime v) { lastLogin = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final AppUser u = new AppUser();
        public Builder username(String v)     { u.username = v;     return this; }
        public Builder passwordHash(String v) { u.passwordHash = v; return this; }
        public Builder role(Role v)           { u.role = v;         return this; }
        public Builder fullName(String v)     { u.fullName = v;     return this; }
        public Builder email(String v)        { u.email = v;        return this; }
        public Builder enabled(boolean v)     { u.enabled = v;      return this; }
        public AppUser build() { return u; }
    }
}
