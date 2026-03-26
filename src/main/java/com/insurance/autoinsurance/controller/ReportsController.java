package com.insurance.autoinsurance.controller;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.PaymentRepository;
import com.insurance.autoinsurance.repository.PolicyRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    private final PolicyRepository  polRepo;
    private final PaymentRepository payRepo;

    public ReportsController(PolicyRepository polRepo, PaymentRepository payRepo) {
        this.polRepo = polRepo; this.payRepo = payRepo;
    }

    @GetMapping
    public String index() { return "redirect:/reports/expiry"; }

    @GetMapping("/expiry")
    public String expiry(@RequestParam(defaultValue = "30") int days, Model model) {
        List<Policy> expiring = polRepo.findPoliciesExpiringBetween(
            LocalDate.now(), LocalDate.now().plusDays(days));

        // Pre-compute daysLeft server-side — avoids T() SpEL and NPE in templates
        List<Map<String, Object>> enriched = new ArrayList<>();
        for (Policy p : expiring) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("policy", p);
            long daysLeft = p.getPolicyEndDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), p.getPolicyEndDate()) : 0;
            row.put("daysLeft", daysLeft);
            row.put("urgency",  daysLeft <= 7 ? "danger" : daysLeft <= 15 ? "warning" : "info");
            enriched.add(row);
        }

        model.addAttribute("expiringRows",  enriched);
        model.addAttribute("expiringPolicies", expiring); // keep for size() in subtitle
        model.addAttribute("days",        days);
        model.addAttribute("activePage",  "reports");
        model.addAttribute("activeReport","expiry");
        return "reports/expiry";
    }

    @GetMapping("/revenue")
    public String revenue(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate to,
            Model model) {
        if (from == null) from = LocalDate.now().withDayOfYear(1);
        if (to   == null) to   = LocalDate.now();
        List<Payment> payments = payRepo.findSuccessWithPolicyBetween(from, to);
        double total = payments.stream()
            .mapToDouble(p -> p.getPaymentAmount() != null ? p.getPaymentAmount() : 0).sum();
        model.addAttribute("payments",     payments);
        model.addAttribute("totalRevenue", total);
        model.addAttribute("fromDate",     from);
        model.addAttribute("toDate",       to);
        model.addAttribute("activePage",   "reports");
        model.addAttribute("activeReport", "revenue");
        return "reports/revenue";
    }

    @GetMapping("/failed")
    public String failed(Model model) {
        List<Payment> failed = payRepo.findByStatusWithPolicy(Payment.PaymentStatus.FAILED);
        model.addAttribute("failedPayments", failed);
        model.addAttribute("activePage",     "reports");
        model.addAttribute("activeReport",   "failed");
        return "reports/failed";
    }
}
