package com.insurance.autoinsurance.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "policies", indexes = {
    @Index(name = "idx_pol_status", columnList = "policy_status"),
    @Index(name = "idx_pol_holder", columnList = "policy_holder_name"),
    @Index(name = "idx_pol_end",    columnList = "policy_end_date")
})
public class Policy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id") private Long policyId;

    @NotBlank(message = "Policy name is required")
    @Column(name = "policy_name", nullable = false, length = 120)
    private String policyName;

    @NotBlank(message = "Holder name is required")
    @Column(name = "policy_holder_name", nullable = false, length = 150)
    private String policyHolderName;

    @NotNull(message = "Premium is required") @Positive
    @Column(name = "policy_amount", nullable = false)
    private Double policyAmount;

    @NotNull(message = "Start date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "policy_start_date", nullable = false)
    private LocalDate policyStartDate;

    @NotNull(message = "End date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "policy_end_date", nullable = false)
    private LocalDate policyEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_status", nullable = false, length = 20)
    private PolicyStatus policyStatus = PolicyStatus.ACTIVE;

    @Column(name = "vehicle_number", length = 50)  private String vehicleNumber;
    @Column(name = "vehicle_model", length = 100)   private String vehicleModel;
    @Column(name = "vehicle_year")                  private Integer vehicleYear;
    @Column(name = "coverage_type", length = 50)    private String coverageType;
    @Column(name = "deductible_amount")             private Double deductibleAmount;
    @Email @Column(name = "holder_email", length = 200) private String holderEmail;
    @Column(name = "holder_phone", length = 15)     private String holderPhone;
    @Column(name = "holder_address", length = 300)  private String holderAddress;
    @Column(name = "created_at", updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at")                    private LocalDateTime updatedAt;
    @Column(name = "created_by", length = 100)      private String createdBy;

    // ── Premium schedule fields ─────────────────────────────────────────
    /** Day of month (1–28) on which monthly premium is due. Default 26. */
    @Column(name = "premium_day_of_month") private Integer premiumDayOfMonth = 26;

    /** Fixed monthly premium amount. Defaults to policyAmount / 12 if null. */
    @Column(name = "monthly_premium") private Double monthlyPremium;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments = new ArrayList<>();

    @PrePersist protected void prePersist() {
        createdAt = updatedAt = LocalDateTime.now();
        if (policyStatus == null) policyStatus = PolicyStatus.ACTIVE;
        if (premiumDayOfMonth == null) premiumDayOfMonth = 26;
        if (monthlyPremium == null && policyAmount != null) monthlyPremium = policyAmount / 12.0;
    }
    @PreUpdate protected void preUpdate() { updatedAt = LocalDateTime.now(); }

    /** Effective monthly premium — always safe to call */
    public double effectiveMonthlyPremium() {
        if (monthlyPremium != null && monthlyPremium > 0) return monthlyPremium;
        return policyAmount != null ? policyAmount / 12.0 : 0.0;
    }

    public enum PolicyStatus {
        DRAFT("Draft"), ACTIVE("Active"), EXPIRED("Expired"),
        CANCELLED("Cancelled"), RENEWED("Renewed"), SUSPENDED("Suspended");
        private final String label;
        PolicyStatus(String l) { this.label = l; }
        public String getLabel() { return label; }
    }

    public Policy() {}
    public Long getPolicyId() { return policyId; }
    public void setPolicyId(Long v) { policyId = v; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String v) { policyName = v; }
    public String getPolicyHolderName() { return policyHolderName; }
    public void setPolicyHolderName(String v) { policyHolderName = v; }
    public Double getPolicyAmount() { return policyAmount; }
    public void setPolicyAmount(Double v) { policyAmount = v; }
    public LocalDate getPolicyStartDate() { return policyStartDate; }
    public void setPolicyStartDate(LocalDate v) { policyStartDate = v; }
    public LocalDate getPolicyEndDate() { return policyEndDate; }
    public void setPolicyEndDate(LocalDate v) { policyEndDate = v; }
    public PolicyStatus getPolicyStatus() { return policyStatus; }
    public void setPolicyStatus(PolicyStatus v) { policyStatus = v; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String v) { vehicleNumber = v; }
    public String getVehicleModel() { return vehicleModel; }
    public void setVehicleModel(String v) { vehicleModel = v; }
    public Integer getVehicleYear() { return vehicleYear; }
    public void setVehicleYear(Integer v) { vehicleYear = v; }
    public String getCoverageType() { return coverageType; }
    public void setCoverageType(String v) { coverageType = v; }
    public Double getDeductibleAmount() { return deductibleAmount; }
    public void setDeductibleAmount(Double v) { deductibleAmount = v; }
    public String getHolderEmail() { return holderEmail; }
    public void setHolderEmail(String v) { holderEmail = v; }
    public String getHolderPhone() { return holderPhone; }
    public void setHolderPhone(String v) { holderPhone = v; }
    public String getHolderAddress() { return holderAddress; }
    public void setHolderAddress(String v) { holderAddress = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public Integer getPremiumDayOfMonth() { return premiumDayOfMonth != null ? premiumDayOfMonth : 26; }
    public void setPremiumDayOfMonth(Integer v) { premiumDayOfMonth = v; }
    public Double getMonthlyPremium() { return monthlyPremium; }
    public void setMonthlyPremium(Double v) { monthlyPremium = v; }
    public List<Payment> getPayments() { return payments; }
    public void setPayments(List<Payment> v) { payments = v; }

    public static Builder builder() { return new Builder(); }
    public static final class Builder {
        private final Policy p = new Policy();
        public Builder policyName(String v)          { p.policyName = v;          return this; }
        public Builder policyHolderName(String v)    { p.policyHolderName = v;    return this; }
        public Builder policyAmount(Double v)         { p.policyAmount = v;         return this; }
        public Builder policyStartDate(LocalDate v)   { p.policyStartDate = v;      return this; }
        public Builder policyEndDate(LocalDate v)     { p.policyEndDate = v;        return this; }
        public Builder policyStatus(PolicyStatus v)   { p.policyStatus = v;         return this; }
        public Builder vehicleNumber(String v)        { p.vehicleNumber = v;        return this; }
        public Builder vehicleModel(String v)         { p.vehicleModel = v;         return this; }
        public Builder vehicleYear(Integer v)         { p.vehicleYear = v;          return this; }
        public Builder coverageType(String v)         { p.coverageType = v;         return this; }
        public Builder deductibleAmount(Double v)     { p.deductibleAmount = v;     return this; }
        public Builder holderEmail(String v)          { p.holderEmail = v;          return this; }
        public Builder holderPhone(String v)          { p.holderPhone = v;          return this; }
        public Builder holderAddress(String v)        { p.holderAddress = v;        return this; }
        public Builder createdBy(String v)            { p.createdBy = v;            return this; }
        public Builder premiumDayOfMonth(Integer v)   { p.premiumDayOfMonth = v;    return this; }
        public Builder monthlyPremium(Double v)       { p.monthlyPremium = v;       return this; }
        public Policy build() { return p; }
    }
}
