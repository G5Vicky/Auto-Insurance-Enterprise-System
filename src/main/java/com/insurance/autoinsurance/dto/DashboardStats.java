package com.insurance.autoinsurance.dto;
import java.util.List;
import java.util.Map;

public class DashboardStats {
    private long totalPolicies, activePolicies, expiredPolicies, cancelledPolicies, draftPolicies;
    private long totalPayments, successfulPayments, failedPayments, pendingPayments;
    private double totalRevenue, failedAmount, pendingAmount, successRate;
    private Map<String,Long>   monthlyPaymentCounts;
    private Map<String,Double> monthlyRevenue;
    private Map<String,Long>   paymentStatusDistribution;
    private Map<String,Double> revenueByPolicy;
    private Map<String,Long>   policyStatusDistribution;
    private List<PaymentDetailsView> recentPayments;
    private long policiesExpiringIn30Days, failedPaymentsToday;

    public DashboardStats() {}
    public long getTotalPolicies(){return totalPolicies;} public void setTotalPolicies(long v){totalPolicies=v;}
    public long getActivePolicies(){return activePolicies;} public void setActivePolicies(long v){activePolicies=v;}
    public long getExpiredPolicies(){return expiredPolicies;} public void setExpiredPolicies(long v){expiredPolicies=v;}
    public long getCancelledPolicies(){return cancelledPolicies;} public void setCancelledPolicies(long v){cancelledPolicies=v;}
    public long getDraftPolicies(){return draftPolicies;} public void setDraftPolicies(long v){draftPolicies=v;}
    public long getTotalPayments(){return totalPayments;} public void setTotalPayments(long v){totalPayments=v;}
    public long getSuccessfulPayments(){return successfulPayments;} public void setSuccessfulPayments(long v){successfulPayments=v;}
    public long getFailedPayments(){return failedPayments;} public void setFailedPayments(long v){failedPayments=v;}
    public long getPendingPayments(){return pendingPayments;} public void setPendingPayments(long v){pendingPayments=v;}
    public double getTotalRevenue(){return totalRevenue;} public void setTotalRevenue(double v){totalRevenue=v;}
    public double getFailedAmount(){return failedAmount;} public void setFailedAmount(double v){failedAmount=v;}
    public double getPendingAmount(){return pendingAmount;} public void setPendingAmount(double v){pendingAmount=v;}
    public double getSuccessRate(){return successRate;} public void setSuccessRate(double v){successRate=v;}
    public Map<String,Long> getMonthlyPaymentCounts(){return monthlyPaymentCounts;} public void setMonthlyPaymentCounts(Map<String,Long> v){monthlyPaymentCounts=v;}
    public Map<String,Double> getMonthlyRevenue(){return monthlyRevenue;} public void setMonthlyRevenue(Map<String,Double> v){monthlyRevenue=v;}
    public Map<String,Long> getPaymentStatusDistribution(){return paymentStatusDistribution;} public void setPaymentStatusDistribution(Map<String,Long> v){paymentStatusDistribution=v;}
    public Map<String,Double> getRevenueByPolicy(){return revenueByPolicy;} public void setRevenueByPolicy(Map<String,Double> v){revenueByPolicy=v;}
    public Map<String,Long> getPolicyStatusDistribution(){return policyStatusDistribution;} public void setPolicyStatusDistribution(Map<String,Long> v){policyStatusDistribution=v;}
    public List<PaymentDetailsView> getRecentPayments(){return recentPayments;} public void setRecentPayments(List<PaymentDetailsView> v){recentPayments=v;}
    public long getPoliciesExpiringIn30Days(){return policiesExpiringIn30Days;} public void setPoliciesExpiringIn30Days(long v){policiesExpiringIn30Days=v;}
    public long getFailedPaymentsToday(){return failedPaymentsToday;} public void setFailedPaymentsToday(long v){failedPaymentsToday=v;}

    public static Builder builder(){return new Builder();}
    public static final class Builder {
        private final DashboardStats s = new DashboardStats();
        public Builder totalPolicies(long v){s.totalPolicies=v;return this;}
        public Builder activePolicies(long v){s.activePolicies=v;return this;}
        public Builder expiredPolicies(long v){s.expiredPolicies=v;return this;}
        public Builder cancelledPolicies(long v){s.cancelledPolicies=v;return this;}
        public Builder draftPolicies(long v){s.draftPolicies=v;return this;}
        public Builder totalPayments(long v){s.totalPayments=v;return this;}
        public Builder successfulPayments(long v){s.successfulPayments=v;return this;}
        public Builder failedPayments(long v){s.failedPayments=v;return this;}
        public Builder pendingPayments(long v){s.pendingPayments=v;return this;}
        public Builder totalRevenue(double v){s.totalRevenue=v;return this;}
        public Builder failedAmount(double v){s.failedAmount=v;return this;}
        public Builder pendingAmount(double v){s.pendingAmount=v;return this;}
        public Builder successRate(double v){s.successRate=v;return this;}
        public Builder monthlyPaymentCounts(Map<String,Long> v){s.monthlyPaymentCounts=v;return this;}
        public Builder monthlyRevenue(Map<String,Double> v){s.monthlyRevenue=v;return this;}
        public Builder paymentStatusDistribution(Map<String,Long> v){s.paymentStatusDistribution=v;return this;}
        public Builder revenueByPolicy(Map<String,Double> v){s.revenueByPolicy=v;return this;}
        public Builder policyStatusDistribution(Map<String,Long> v){s.policyStatusDistribution=v;return this;}
        public Builder recentPayments(List<PaymentDetailsView> v){s.recentPayments=v;return this;}
        public Builder policiesExpiringIn30Days(long v){s.policiesExpiringIn30Days=v;return this;}
        public Builder failedPaymentsToday(long v){s.failedPaymentsToday=v;return this;}
        public DashboardStats build(){return s;}
    }
}
