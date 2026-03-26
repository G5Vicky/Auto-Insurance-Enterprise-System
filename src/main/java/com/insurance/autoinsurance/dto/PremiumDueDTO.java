package com.insurance.autoinsurance.dto;

import java.time.LocalDate;

/**
 * Carries premium-due information for a single policy for one month.
 */
public class PremiumDueDTO {

    private Long    policyId;
    private String  policyName;
    private String  policyHolderName;
    private String  vehicleModel;
    private String  vehicleNumber;
    private LocalDate dueDate;
    private double  premiumAmount;
    private double  lateFee;       // 0 / 30 / 100
    private double  totalDue;      // premiumAmount + lateFee
    private boolean paid;
    private boolean overdue;
    private String  overdueTier;   // "NONE", "LATE_30", "LATE_100"

    public PremiumDueDTO() {}

    // ── Getters & setters ──────────────────────────────────────────────────
    public Long    getPolicyId()            { return policyId; }
    public void    setPolicyId(Long v)      { policyId = v; }
    public String  getPolicyName()          { return policyName; }
    public void    setPolicyName(String v)  { policyName = v; }
    public String  getPolicyHolderName()    { return policyHolderName; }
    public void    setPolicyHolderName(String v) { policyHolderName = v; }
    public String  getVehicleModel()        { return vehicleModel; }
    public void    setVehicleModel(String v){ vehicleModel = v; }
    public String  getVehicleNumber()       { return vehicleNumber; }
    public void    setVehicleNumber(String v){ vehicleNumber = v; }
    public LocalDate getDueDate()           { return dueDate; }
    public void    setDueDate(LocalDate v)  { dueDate = v; }
    public double  getPremiumAmount()       { return premiumAmount; }
    public void    setPremiumAmount(double v){ premiumAmount = v; }
    public double  getLateFee()             { return lateFee; }
    public void    setLateFee(double v)     { lateFee = v; }
    public double  getTotalDue()            { return totalDue; }
    public void    setTotalDue(double v)    { totalDue = v; }
    public boolean isPaid()                 { return paid; }
    public void    setPaid(boolean v)       { paid = v; }
    public boolean isOverdue()              { return overdue; }
    public void    setOverdue(boolean v)    { overdue = v; }
    public String  getOverdueTier()         { return overdueTier; }
    public void    setOverdueTier(String v) { overdueTier = v; }
}
