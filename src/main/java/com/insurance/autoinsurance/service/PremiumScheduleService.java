package com.insurance.autoinsurance.service;

import com.insurance.autoinsurance.audit.AuditService;
import com.insurance.autoinsurance.dto.PremiumDueDTO;
import com.insurance.autoinsurance.model.AuditLog;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.PaymentRepository;
import com.insurance.autoinsurance.repository.PolicyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * LIC-style premium schedule service.
 *
 * Rules:
 *  - Each active policy has a fixed day-of-month (premiumDayOfMonth, default 26)
 *  - Fixed monthly amount (monthlyPremium, default policyAmount / 12)
 *  - Late fee: after due date          → ₹30
 *              after next month's date → ₹100
 *  - Advance payment allowed only if ALL previous dues are cleared
 */
@Service
@Transactional
public class PremiumScheduleService {

    /** Late fee tiers (₹) */
    public static final double LATE_FEE_TIER1 = 30.0;   // overdue by < 1 month
    public static final double LATE_FEE_TIER2 = 100.0;  // overdue by ≥ 1 month

    private final PolicyRepository  policyRepo;
    private final PaymentRepository paymentRepo;
    private final AuditService      audit;

    public PremiumScheduleService(PolicyRepository policyRepo,
                                  PaymentRepository paymentRepo,
                                  AuditService audit) {
        this.policyRepo  = policyRepo;
        this.paymentRepo = paymentRepo;
        this.audit       = audit;
    }

    // ── Current-month dues ─────────────────────────────────────────────────

    /**
     * Returns premium-due records for ALL active policies in the current month.
     * A due is marked "paid" if a SUCCESS payment exists for that policy
     * in the same year-month.
     */
    public List<PremiumDueDTO> getCurrentMonthDues() {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        return buildDueDTOs(today, thisMonth);
    }

    // ── Advance (next-month) dues ─────────────────────────────────────────

    /**
     * Returns next-month premium-due records.
     * A policy is eligible for advance payment only if it has NO unpaid current-month due.
     */
    public List<PremiumDueDTO> getNextMonthDues() {
        LocalDate today = LocalDate.now();
        YearMonth nextMonth = YearMonth.from(today).plusMonths(1);
        return buildDueDTOs(today, nextMonth);
    }

    /**
     * Checks whether a policy has any outstanding (unpaid) current-month due.
     * Returns true if ALL current-month dues are cleared (advance payment allowed).
     */
    public boolean isAdvancePaymentAllowed(Long policyId) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        Policy policy = policyRepo.findById(policyId).orElse(null);
        if (policy == null) return false;
        PremiumDueDTO current = buildDueDTO(policy, today, thisMonth);
        return current.isPaid();
    }

    /**
     * Returns a human-readable message about why advance payment is blocked,
     * or null if it is allowed.
     */
    public String getAdvanceBlockReason(Long policyId) {
        if (isAdvancePaymentAllowed(policyId)) return null;
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.from(today);
        Policy policy = policyRepo.findById(policyId).orElse(null);
        if (policy == null) return "Policy not found.";
        PremiumDueDTO due = buildDueDTO(policy, today, thisMonth);
        return String.format(
            "Advance payment is blocked. Current month's premium of ₹%.2f (due %s) is still unpaid.",
            due.getTotalDue(), due.getDueDate()
        );
    }

    // ── Premium payment ────────────────────────────────────────────────────

    /**
     * Records a premium payment (current or advance) for a policy.
     * Validates advance eligibility before proceeding.
     *
     * @param policyId   target policy
     * @param isAdvance  true = next-month payment
     * @return saved Payment entity
     */
    public Payment recordPremiumPayment(Long policyId, boolean isAdvance) {
        Policy policy = policyRepo.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy #" + policyId + " not found."));

        if (isAdvance && !isAdvancePaymentAllowed(policyId)) {
            throw new IllegalStateException(getAdvanceBlockReason(policyId));
        }

        LocalDate today  = LocalDate.now();
        YearMonth target = isAdvance ? YearMonth.from(today).plusMonths(1) : YearMonth.from(today);

        // Determine due date for that month
        int dayOfMonth = policy.getPremiumDayOfMonth() != null ? policy.getPremiumDayOfMonth() : 26;
        LocalDate dueDate = target.atDay(Math.min(dayOfMonth, target.lengthOfMonth()));

        double premium = policy.effectiveMonthlyPremium();
        double lateFee = isAdvance ? 0.0 : calculateLateFee(dueDate, today);
        double total   = premium + lateFee;

        Payment payment = Payment.builder()
            .policy(policy)
            .paymentAmount(total)
            .paymentDate(today)
            .paymentStatus(Payment.PaymentStatus.SUCCESS)
            .retryCount(0)
            .remarks(String.format("%s premium for %s%s",
                isAdvance ? "Advance" : "Monthly",
                target,
                lateFee > 0 ? String.format(" (incl. ₹%.0f late fee)", lateFee) : ""))
            .build();

        Payment saved = paymentRepo.save(payment);
        audit.log(AuditLog.ActionType.PAYMENT_MADE, "Payment", saved.getPaymentId(),
            String.format("%s premium ₹%.2f for Policy#%d [%s]",
                isAdvance ? "Advance" : "Monthly", total, policyId, policy.getPolicyName()));
        return saved;
    }

    // ── Late fee calculation ───────────────────────────────────────────────

    /**
     * Calculates late fee based on how overdue the payment is.
     *  today ≤ dueDate          → ₹0
     *  dueDate < today < dueDate+30 → ₹30
     *  today ≥ dueDate+30       → ₹100
     */
    public double calculateLateFee(LocalDate dueDate, LocalDate today) {
        if (!today.isAfter(dueDate)) return 0.0;
        if (today.isBefore(dueDate.plusMonths(1))) return LATE_FEE_TIER1;
        return LATE_FEE_TIER2;
    }

    // ── Internal builders ─────────────────────────────────────────────────

    private List<PremiumDueDTO> buildDueDTOs(LocalDate today, YearMonth month) {
        List<Policy> activePolicies = policyRepo.findByPolicyStatus(Policy.PolicyStatus.ACTIVE);
        List<PremiumDueDTO> result = new ArrayList<>();
        for (Policy p : activePolicies) {
            // Only include policies active during that month
            if (p.getPolicyEndDate() != null && p.getPolicyEndDate().isBefore(month.atDay(1))) continue;
            if (p.getPolicyStartDate() != null && p.getPolicyStartDate().isAfter(month.atEndOfMonth())) continue;
            result.add(buildDueDTO(p, today, month));
        }
        return result;
    }

    private PremiumDueDTO buildDueDTO(Policy policy, LocalDate today, YearMonth month) {
        int dayOfMonth = policy.getPremiumDayOfMonth() != null ? policy.getPremiumDayOfMonth() : 26;
        LocalDate dueDate = month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));

        double premium = policy.effectiveMonthlyPremium();
        boolean overdue = today.isAfter(dueDate);
        double lateFee = calculateLateFee(dueDate, today);
        double total = premium + lateFee;

        // Check if already paid this month (any SUCCESS payment in that year-month)
        boolean paid = paymentRepo
            .findByPolicy_PolicyIdOrderByPaymentDateDesc(policy.getPolicyId())
            .stream()
            .anyMatch(pay ->
                pay.getPaymentStatus() == Payment.PaymentStatus.SUCCESS &&
                pay.getPaymentDate() != null &&
                YearMonth.from(pay.getPaymentDate()).equals(month)
            );

        String tier = "NONE";
        if (overdue && !paid) {
            tier = (lateFee >= LATE_FEE_TIER2) ? "LATE_100" : "LATE_30";
        }

        PremiumDueDTO dto = new PremiumDueDTO();
        dto.setPolicyId(policy.getPolicyId());
        dto.setPolicyName(policy.getPolicyName());
        dto.setPolicyHolderName(policy.getPolicyHolderName());
        dto.setVehicleModel(policy.getVehicleModel());
        dto.setVehicleNumber(policy.getVehicleNumber());
        dto.setDueDate(dueDate);
        dto.setPremiumAmount(premium);
        dto.setLateFee(paid ? 0.0 : lateFee);
        dto.setTotalDue(paid ? premium : total);
        dto.setPaid(paid);
        dto.setOverdue(overdue && !paid);
        dto.setOverdueTier(paid ? "NONE" : tier);
        return dto;
    }
}
