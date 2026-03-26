package com.insurance.autoinsurance.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_read", columnList = "is_read"),
    @Index(name = "idx_notif_time", columnList = "created_at")
})
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notif_id") private Long notifId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private Type type;

    @Column(name = "title", nullable = false, length = 200) private String title;
    @Column(name = "message", nullable = false, length = 500) private String message;
    @Column(name = "is_read", nullable = false) private boolean read = false;
    @Column(name = "reference_id") private Long referenceId;
    @Column(name = "reference_type", length = 50) private String referenceType;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void pre() { createdAt = LocalDateTime.now(); }

    public enum Type { EXPIRY_WARNING, PAYMENT_FAILED, PAYMENT_SUCCESS, POLICY_CREATED, POLICY_RENEWED, SYSTEM }

    public Notification() {}
    public Long getNotifId()          { return notifId; }
    public void setNotifId(Long v)    { notifId = v; }
    public Type getType()             { return type; }
    public void setType(Type v)       { type = v; }
    public String getTitle()          { return title; }
    public void setTitle(String v)    { title = v; }
    public String getMessage()        { return message; }
    public void setMessage(String v)  { message = v; }
    public boolean isRead()           { return read; }
    public void setRead(boolean v)    { read = v; }
    public Long getReferenceId()      { return referenceId; }
    public void setReferenceId(Long v){ referenceId = v; }
    public String getReferenceType()  { return referenceType; }
    public void setReferenceType(String v){ referenceType = v; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public void setCreatedAt(LocalDateTime v){ createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Notification n = new Notification();
        public Builder type(Type v)           { n.type = v;          return this; }
        public Builder title(String v)        { n.title = v;         return this; }
        public Builder message(String v)      { n.message = v;       return this; }
        public Builder referenceId(Long v)    { n.referenceId = v;   return this; }
        public Builder referenceType(String v){ n.referenceType = v; return this; }
        public Notification build() { return n; }
    }
}
