package com.insurance.autoinsurance.model;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_pay_policy", columnList = "policy_id"),
    @Index(name = "idx_pay_status", columnList = "payment_status"),
    @Index(name = "idx_pay_date",   columnList = "payment_date")
})
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id") private Long paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_payment_policy"))
    private Policy policy;

    @Column(name = "payment_amount", nullable = false) private Double paymentAmount;
    @Column(name = "payment_date",   nullable = false) private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(name = "retry_of_payment_id") private Long retryOfPaymentId;
    @Column(name = "retry_count")         private Integer retryCount = 0;
    @Column(name = "remarks", length = 500) private String remarks;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;

    @PrePersist protected void prePersist() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
    }

    /** Convenience getter — avoids NPE on lazy-loaded policy */
    public Long getPolicyId() { return policy != null ? policy.getPolicyId() : null; }

    public enum PaymentStatus {
        SUCCESS("Success"), FAILED("Failed"), PENDING("Pending");
        private final String label;
        PaymentStatus(String l) { this.label = l; }
        public String getLabel() { return label; }
    }

    public Payment() {}
    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long v) { paymentId = v; }
    public Policy getPolicy() { return policy; }
    public void setPolicy(Policy v) { policy = v; }
    public Double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(Double v) { paymentAmount = v; }
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate v) { paymentDate = v; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus v) { paymentStatus = v; }
    public Long getRetryOfPaymentId() { return retryOfPaymentId; }
    public void setRetryOfPaymentId(Long v) { retryOfPaymentId = v; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer v) { retryCount = v; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String v) { remarks = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Payment p = new Payment();
        public Builder policy(Policy v)              { p.policy = v;              return this; }
        public Builder paymentAmount(Double v)        { p.paymentAmount = v;        return this; }
        public Builder paymentDate(LocalDate v)       { p.paymentDate = v;          return this; }
        public Builder paymentStatus(PaymentStatus v) { p.paymentStatus = v;        return this; }
        public Builder retryOfPaymentId(Long v)       { p.retryOfPaymentId = v;     return this; }
        public Builder retryCount(Integer v)          { p.retryCount = v;           return this; }
        public Builder remarks(String v)              { p.remarks = v;              return this; }
        public Payment build() { return p; }
    }
}
