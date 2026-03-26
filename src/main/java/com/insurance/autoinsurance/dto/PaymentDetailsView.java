package com.insurance.autoinsurance.dto;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentDetailsView {
    private Long paymentId, policyId, retryOfPaymentId;
    private Double paymentAmount, policyAmount;
    private LocalDate paymentDate, policyStartDate, policyEndDate;
    private Payment.PaymentStatus paymentStatus;
    private Policy.PolicyStatus policyStatus;
    private LocalDateTime createdAt;
    private Integer retryCount;
    private String remarks, policyName, policyHolderName;
    private String vehicleNumber, vehicleModel, holderEmail, holderPhone, coverageType;

    public PaymentDetailsView() {}
    public Long getPaymentId(){return paymentId;} public void setPaymentId(Long v){paymentId=v;}
    public Long getPolicyId(){return policyId;} public void setPolicyId(Long v){policyId=v;}
    public Long getRetryOfPaymentId(){return retryOfPaymentId;} public void setRetryOfPaymentId(Long v){retryOfPaymentId=v;}
    public Double getPaymentAmount(){return paymentAmount;} public void setPaymentAmount(Double v){paymentAmount=v;}
    public Double getPolicyAmount(){return policyAmount;} public void setPolicyAmount(Double v){policyAmount=v;}
    public LocalDate getPaymentDate(){return paymentDate;} public void setPaymentDate(LocalDate v){paymentDate=v;}
    public LocalDate getPolicyStartDate(){return policyStartDate;} public void setPolicyStartDate(LocalDate v){policyStartDate=v;}
    public LocalDate getPolicyEndDate(){return policyEndDate;} public void setPolicyEndDate(LocalDate v){policyEndDate=v;}
    public Payment.PaymentStatus getPaymentStatus(){return paymentStatus;} public void setPaymentStatus(Payment.PaymentStatus v){paymentStatus=v;}
    public Policy.PolicyStatus getPolicyStatus(){return policyStatus;} public void setPolicyStatus(Policy.PolicyStatus v){policyStatus=v;}
    public LocalDateTime getCreatedAt(){return createdAt;} public void setCreatedAt(LocalDateTime v){createdAt=v;}
    public Integer getRetryCount(){return retryCount;} public void setRetryCount(Integer v){retryCount=v;}
    public String getRemarks(){return remarks;} public void setRemarks(String v){remarks=v;}
    public String getPolicyName(){return policyName;} public void setPolicyName(String v){policyName=v;}
    public String getPolicyHolderName(){return policyHolderName;} public void setPolicyHolderName(String v){policyHolderName=v;}
    public String getVehicleNumber(){return vehicleNumber;} public void setVehicleNumber(String v){vehicleNumber=v;}
    public String getVehicleModel(){return vehicleModel;} public void setVehicleModel(String v){vehicleModel=v;}
    public String getHolderEmail(){return holderEmail;} public void setHolderEmail(String v){holderEmail=v;}
    public String getHolderPhone(){return holderPhone;} public void setHolderPhone(String v){holderPhone=v;}
    public String getCoverageType(){return coverageType;} public void setCoverageType(String v){coverageType=v;}

    public static Builder builder(){return new Builder();}
    public static final class Builder {
        private final PaymentDetailsView v = new PaymentDetailsView();
        public Builder paymentId(Long x){v.paymentId=x;return this;}
        public Builder policyId(Long x){v.policyId=x;return this;}
        public Builder retryOfPaymentId(Long x){v.retryOfPaymentId=x;return this;}
        public Builder paymentAmount(Double x){v.paymentAmount=x;return this;}
        public Builder policyAmount(Double x){v.policyAmount=x;return this;}
        public Builder paymentDate(LocalDate x){v.paymentDate=x;return this;}
        public Builder policyStartDate(LocalDate x){v.policyStartDate=x;return this;}
        public Builder policyEndDate(LocalDate x){v.policyEndDate=x;return this;}
        public Builder paymentStatus(Payment.PaymentStatus x){v.paymentStatus=x;return this;}
        public Builder policyStatus(Policy.PolicyStatus x){v.policyStatus=x;return this;}
        public Builder createdAt(LocalDateTime x){v.createdAt=x;return this;}
        public Builder retryCount(Integer x){v.retryCount=x;return this;}
        public Builder remarks(String x){v.remarks=x;return this;}
        public Builder policyName(String x){v.policyName=x;return this;}
        public Builder policyHolderName(String x){v.policyHolderName=x;return this;}
        public Builder vehicleNumber(String x){v.vehicleNumber=x;return this;}
        public Builder vehicleModel(String x){v.vehicleModel=x;return this;}
        public Builder holderEmail(String x){v.holderEmail=x;return this;}
        public Builder holderPhone(String x){v.holderPhone=x;return this;}
        public Builder coverageType(String x){v.coverageType=x;return this;}
        public PaymentDetailsView build(){return v;}
    }
}
