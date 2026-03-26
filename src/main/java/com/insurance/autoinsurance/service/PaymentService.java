package com.insurance.autoinsurance.service;
import com.insurance.autoinsurance.audit.AuditService;
import com.insurance.autoinsurance.dto.DashboardStats;
import com.insurance.autoinsurance.dto.PaymentDetailsView;
import com.insurance.autoinsurance.model.AuditLog;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.PaymentRepository;
import com.insurance.autoinsurance.repository.PolicyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {
    private static final String[] MONTHS =
        {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};

    private final PaymentRepository payRepo;
    private final PolicyRepository  polRepo;
    private final AuditService      audit;
    private final Random            rng = new Random();

    public PaymentService(PaymentRepository payRepo, PolicyRepository polRepo, AuditService audit) {
        this.payRepo = payRepo; this.polRepo = polRepo; this.audit = audit;
    }

    // ── Core Operations ────────────────────────────────────────────────────

    public Payment makePayment(Long policyId, Double amount, LocalDate date, String remarks) {
        Policy policy = polRepo.findById(policyId)
            .orElseThrow(() -> new IllegalArgumentException("Policy #" + policyId + " not found."));
        Payment.PaymentStatus status = simulateGateway();
        Payment payment = Payment.builder()
            .policy(policy).paymentAmount(amount).paymentDate(date)
            .paymentStatus(status).retryCount(0)
            .remarks(remarks != null && !remarks.isBlank() ? remarks : null).build();
        Payment saved = payRepo.save(payment);
        audit.log(AuditLog.ActionType.PAYMENT_MADE, "Payment", saved.getPaymentId(),
            String.format("Payment \u20b9%.2f for Policy#%d [%s] — %s",
                amount, policyId, policy.getPolicyName(), status));
        return saved;
    }

    public Payment retryPayment(Long originalId) {
        Payment original = payRepo.findById(originalId)
            .orElseThrow(() -> new IllegalArgumentException("Payment #" + originalId + " not found."));
        if (original.getPaymentStatus() != Payment.PaymentStatus.FAILED)
            throw new IllegalStateException("Only FAILED payments can be retried. " +
                "Current status: " + original.getPaymentStatus().getLabel());
        int retries = original.getRetryCount() != null ? original.getRetryCount() : 0;
        Payment retry = Payment.builder()
            .policy(original.getPolicy())
            .paymentAmount(original.getPaymentAmount())
            .paymentDate(LocalDate.now())
            .paymentStatus(simulateGateway())
            .retryOfPaymentId(originalId)
            .retryCount(retries + 1)
            .remarks("Retry of Payment #" + originalId).build();
        Payment saved = payRepo.save(retry);
        audit.log(AuditLog.ActionType.PAYMENT_RETRY, "Payment", saved.getPaymentId(),
            "Retry #" + saved.getPaymentId() + " for original #" + originalId +
            " — " + saved.getPaymentStatus());
        return saved;
    }

    private Payment.PaymentStatus simulateGateway() {
        double r = rng.nextDouble();
        if (r < 0.85) return Payment.PaymentStatus.SUCCESS;
        if (r < 0.95) return Payment.PaymentStatus.FAILED;
        return Payment.PaymentStatus.PENDING;
    }

    // ── Queries ────────────────────────────────────────────────────────────

    public Optional<Payment> getById(Long id)   { return payRepo.findById(id); }
    public List<Payment> getAll()               { return payRepo.findAll(); }

    /** Returns payments for a policy — policy already loaded via the join */
    public List<Payment> getByPolicy(Long polId) {
        return payRepo.findByPolicy_PolicyIdOrderByPaymentDateDesc(polId);
    }

    public Optional<Policy> getPolicyDetails(Long policyId) {
        return polRepo.findById(policyId);
    }

    public LocalDate getNextPaymentDate(Long policyId) {
        Optional<Policy> opt = polRepo.findById(policyId);
        if (opt.isEmpty()) return null;
        Policy p = opt.get();
        List<Payment> payments = payRepo.findByPolicy_PolicyIdOrderByPaymentDateDesc(policyId);
        LocalDate base = payments.isEmpty() ? p.getPolicyStartDate()
                                            : payments.get(0).getPaymentDate();
        if (base == null) return LocalDate.now();
        LocalDate next = base.plusMonths(1);
        if (p.getPolicyEndDate() != null && next.isAfter(p.getPolicyEndDate())) return null;
        return next;
    }

    // ── View Projections ───────────────────────────────────────────────────

    public PaymentDetailsView toView(Payment pay) {
        Policy pol = pay.getPolicy();
        return PaymentDetailsView.builder()
            .paymentId(pay.getPaymentId()).policyId(pay.getPolicyId())
            .paymentAmount(pay.getPaymentAmount()).paymentDate(pay.getPaymentDate())
            .paymentStatus(pay.getPaymentStatus()).createdAt(pay.getCreatedAt())
            .retryCount(pay.getRetryCount()).retryOfPaymentId(pay.getRetryOfPaymentId())
            .remarks(pay.getRemarks())
            .policyName        (pol != null ? pol.getPolicyName()        : null)
            .policyHolderName  (pol != null ? pol.getPolicyHolderName()  : null)
            .policyAmount      (pol != null ? pol.getPolicyAmount()       : null)
            .policyStartDate   (pol != null ? pol.getPolicyStartDate()    : null)
            .policyEndDate     (pol != null ? pol.getPolicyEndDate()      : null)
            .policyStatus      (pol != null ? pol.getPolicyStatus()       : null)
            .vehicleNumber     (pol != null ? pol.getVehicleNumber()      : null)
            .vehicleModel      (pol != null ? pol.getVehicleModel()       : null)
            .holderEmail       (pol != null ? pol.getHolderEmail()        : null)
            .holderPhone       (pol != null ? pol.getHolderPhone()        : null)
            .coverageType      (pol != null ? pol.getCoverageType()       : null)
            .build();
    }

    public Optional<PaymentDetailsView> getViewById(Long id) {
        return payRepo.findById(id).map(this::toView);
    }
    public List<PaymentDetailsView> getAllViews() {
        return payRepo.findAll().stream().map(this::toView).collect(Collectors.toList());
    }
    public List<PaymentDetailsView> getViewsByStatus(Payment.PaymentStatus s) {
        return payRepo.findByPaymentStatusOrderByCreatedAtDesc(s)
            .stream().map(this::toView).collect(Collectors.toList());
    }

    // ── Dashboard Stats ────────────────────────────────────────────────────

    public DashboardStats buildStats() {
        long totPol   = polRepo.count();
        long actPol   = polRepo.countByPolicyStatus(Policy.PolicyStatus.ACTIVE);
        long expPol   = polRepo.countByPolicyStatus(Policy.PolicyStatus.EXPIRED);
        long canPol   = polRepo.countByPolicyStatus(Policy.PolicyStatus.CANCELLED);
        long dftPol   = polRepo.countByPolicyStatus(Policy.PolicyStatus.DRAFT);
        long totPay   = payRepo.count();
        long sucPay   = payRepo.countByPaymentStatus(Payment.PaymentStatus.SUCCESS);
        long failPay  = payRepo.countByPaymentStatus(Payment.PaymentStatus.FAILED);
        long penPay   = payRepo.countByPaymentStatus(Payment.PaymentStatus.PENDING);
        double rev    = orZero(payRepo.sumByStatus(Payment.PaymentStatus.SUCCESS));
        double fail   = orZero(payRepo.sumByStatus(Payment.PaymentStatus.FAILED));
        double pend   = orZero(payRepo.sumByStatus(Payment.PaymentStatus.PENDING));
        double rate   = totPay > 0 ? (sucPay * 100.0 / totPay) : 0;

        Map<String,Long>   mCounts = initLong();
        Map<String,Double> mRev    = initDouble();
        safeMonthCounts(mCounts);
        safeMonthRevenue(mRev);

        Map<String,Long>   statusDist = new LinkedHashMap<>();
        statusDist.put("SUCCESS", sucPay);
        statusDist.put("FAILED",  failPay);
        statusDist.put("PENDING", penPay);

        Map<String,Double> revByPol = new LinkedHashMap<>();
        try {
            for (Object[] r : payRepo.revenueGroupedByPolicy())
                revByPol.put((String) r[0], ((Number) r[1]).doubleValue());
        } catch (Exception ignored) {}

        Map<String,Long> polStatus = new LinkedHashMap<>();
        try {
            for (Object[] r : polRepo.countGroupedByStatus())
                polStatus.put(r[0].toString(), ((Number) r[1]).longValue());
        } catch (Exception ignored) {}

        List<PaymentDetailsView> recent = payRepo.findTop5ByOrderByCreatedAtDesc()
            .stream().map(this::toView).collect(Collectors.toList());
        long exp30  = polRepo.findPoliciesExpiringBetween(
            LocalDate.now(), LocalDate.now().plusDays(30)).size();
        long failTd = payRepo.countFailedToday(LocalDate.now());

        return DashboardStats.builder()
            .totalPolicies(totPol).activePolicies(actPol).expiredPolicies(expPol)
            .cancelledPolicies(canPol).draftPolicies(dftPol)
            .totalPayments(totPay).successfulPayments(sucPay)
            .failedPayments(failPay).pendingPayments(penPay)
            .totalRevenue(rev).failedAmount(fail).pendingAmount(pend).successRate(rate)
            .monthlyPaymentCounts(mCounts).monthlyRevenue(mRev)
            .paymentStatusDistribution(statusDist).revenueByPolicy(revByPol)
            .policyStatusDistribution(polStatus)
            .recentPayments(recent)
            .policiesExpiringIn30Days(exp30).failedPaymentsToday(failTd)
            .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private double orZero(Double v) { return v != null ? v : 0.0; }

    private Map<String,Long> initLong() {
        Map<String,Long> m = new LinkedHashMap<>();
        for (String k : MONTHS) m.put(k, 0L);
        return m;
    }
    private Map<String,Double> initDouble() {
        Map<String,Double> m = new LinkedHashMap<>();
        for (String k : MONTHS) m.put(k, 0.0);
        return m;
    }
    private void safeMonthCounts(Map<String,Long> m) {
        try {
            for (Object[] r : payRepo.paymentCountsByMonth()) {
                int mo = ((Number) r[0]).intValue();
                if (mo >= 1 && mo <= 12) m.put(MONTHS[mo - 1], ((Number) r[1]).longValue());
            }
        } catch (Exception ignored) {}
    }
    private void safeMonthRevenue(Map<String,Double> m) {
        try {
            for (Object[] r : payRepo.revenueByMonth()) {
                int mo = ((Number) r[0]).intValue();
                if (mo >= 1 && mo <= 12) m.put(MONTHS[mo - 1], ((Number) r[1]).doubleValue());
            }
        } catch (Exception ignored) {}
    }
}
