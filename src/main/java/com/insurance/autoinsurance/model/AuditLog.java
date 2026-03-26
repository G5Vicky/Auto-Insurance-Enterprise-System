package com.insurance.autoinsurance.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_type,entity_id"),
    @Index(name = "idx_audit_time",   columnList = "action_time")
})
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id") private Long auditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "entity_type", nullable = false, length = 50) private String entityType;
    @Column(name = "entity_id")                                   private Long entityId;
    @Column(name = "description", nullable = false, length = 500) private String description;
    @Column(name = "action_time", updatable = false)              private LocalDateTime actionTime;
    @Column(name = "performed_by", length = 100)                  private String performedBy;

    @PrePersist protected void prePersist() { actionTime = LocalDateTime.now(); }

    public enum ActionType {
        CREATE, UPDATE, DELETE, PAYMENT_MADE, PAYMENT_RETRY, STATUS_CHANGE, VIEW
    }

    public AuditLog() {}
    public Long getAuditId() { return auditId; }
    public void setAuditId(Long v) { auditId = v; }
    public ActionType getActionType() { return actionType; }
    public void setActionType(ActionType v) { actionType = v; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String v) { entityType = v; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long v) { entityId = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public LocalDateTime getActionTime() { return actionTime; }
    public void setActionTime(LocalDateTime v) { actionTime = v; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String v) { performedBy = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final AuditLog a = new AuditLog();
        public Builder actionType(ActionType v)  { a.actionType = v;   return this; }
        public Builder entityType(String v)      { a.entityType = v;   return this; }
        public Builder entityId(Long v)          { a.entityId = v;     return this; }
        public Builder description(String v)     { a.description = v;  return this; }
        public Builder performedBy(String v)     { a.performedBy = v;  return this; }
        public AuditLog build() { return a; }
    }
}
