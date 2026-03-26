package com.insurance.autoinsurance.controller;

import com.insurance.autoinsurance.dto.PremiumDueDTO;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.PolicyRepository;
import com.insurance.autoinsurance.service.PremiumScheduleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles LIC-style premium payment pages:
 *   GET  /premium              → current-month dues dashboard
 *   GET  /premium/advance      → next-month advance payment page
 *   GET  /premium/pay          → pay form (current or advance)
 *   POST /premium/pay          → submit payment
 *   GET  /premium/api/check    → JSON eligibility check for a policy
 */
@Controller
@RequestMapping("/premium")
public class PremiumController {

    private final PremiumScheduleService premiumService;
    private final PolicyRepository       policyRepo;

    public PremiumController(PremiumScheduleService premiumService,
                              PolicyRepository policyRepo) {
        this.premiumService = premiumService;
        this.policyRepo     = policyRepo;
    }

    // ── Current month dues ─────────────────────────────────────────────────
    @GetMapping
    public String currentMonth(Model model) {
        List<PremiumDueDTO> dues = premiumService.getCurrentMonthDues();
        long paidCount    = dues.stream().filter(PremiumDueDTO::isPaid).count();
        long overdueCount = dues.stream().filter(PremiumDueDTO::isOverdue).count();
        double totalDue   = dues.stream().filter(d -> !d.isPaid()).mapToDouble(PremiumDueDTO::getTotalDue).sum();

        model.addAttribute("dues",         dues);
        model.addAttribute("paidCount",    paidCount);
        model.addAttribute("overdueCount", overdueCount);
        model.addAttribute("totalDue",     totalDue);
        model.addAttribute("activePage",   "premium");
        return "premium/current";
    }

    // ── Advance (next-month) dues ─────────────────────────────────────────
    @GetMapping("/advance")
    public String advance(Model model) {
        List<PremiumDueDTO> dues = premiumService.getNextMonthDues();
        // Mark each with advance eligibility
        model.addAttribute("dues",       dues);
        model.addAttribute("activePage", "premium");
        return "premium/advance";
    }

    // ── Pay form ───────────────────────────────────────────────────────────
    @GetMapping("/pay")
    public String payForm(@RequestParam(required = false) Long policyId,
                          @RequestParam(required = false, defaultValue = "false") boolean isAdvance,
                          Model model) {
        model.addAttribute("prefillPolicyId", policyId);
        model.addAttribute("isAdvance",       isAdvance);
        model.addAttribute("activePage",      "premium");

        if (policyId != null) {
            Optional<Policy> opt = policyRepo.findById(policyId);
            opt.ifPresent(p -> {
                model.addAttribute("policy", p);
                model.addAttribute("blockReason",
                    isAdvance ? premiumService.getAdvanceBlockReason(policyId) : null);
                model.addAttribute("advanceAllowed",
                    isAdvance && premiumService.isAdvancePaymentAllowed(policyId));
            });
        }
        return "premium/pay";
    }

    // ── Submit payment ─────────────────────────────────────────────────────
    @PostMapping("/pay")
    public String paySubmit(@RequestParam Long policyId,
                            @RequestParam(defaultValue = "false") boolean isAdvance,
                            RedirectAttributes flash) {
        try {
            Payment payment = premiumService.recordPremiumPayment(policyId, isAdvance);
            flash.addFlashAttribute("successMsg",
                String.format("%s premium payment of ₹%.2f recorded successfully.",
                    isAdvance ? "Advance" : "Monthly", payment.getPaymentAmount()));
        } catch (IllegalStateException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/premium/pay?policyId=" + policyId + "&isAdvance=" + isAdvance;
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMsg", e.getMessage());
            return "redirect:/premium";
        }
        return "redirect:/premium";
    }

    // ── JSON API — eligibility check ───────────────────────────────────────
    @GetMapping("/api/check")
    @ResponseBody
    public ResponseEntity<?> checkEligibility(@RequestParam Long policyId,
                                               @RequestParam(defaultValue = "false") boolean isAdvance) {
        Optional<Policy> opt = policyRepo.findById(policyId);
        if (opt.isEmpty()) return ResponseEntity.badRequest()
            .body(Map.of("error", "Policy #" + policyId + " not found."));

        Policy p = opt.get();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("policyId",          p.getPolicyId());
        data.put("policyName",        p.getPolicyName());
        data.put("policyHolderName",  p.getPolicyHolderName());
        data.put("vehicleModel",      p.getVehicleModel());
        data.put("vehicleNumber",     p.getVehicleNumber());
        data.put("monthlyPremium",    p.effectiveMonthlyPremium());
        data.put("premiumDayOfMonth", p.getPremiumDayOfMonth());
        data.put("policyStatus",      p.getPolicyStatus().name());
        data.put("active",            p.getPolicyStatus() == Policy.PolicyStatus.ACTIVE);

        if (isAdvance) {
            boolean allowed = premiumService.isAdvancePaymentAllowed(policyId);
            data.put("advanceAllowed",  allowed);
            data.put("blockReason",     allowed ? null : premiumService.getAdvanceBlockReason(policyId));
        }

        // Current-month due info
        List<PremiumDueDTO> currentDues = premiumService.getCurrentMonthDues();
        currentDues.stream()
            .filter(d -> d.getPolicyId().equals(policyId))
            .findFirst()
            .ifPresent(d -> {
                data.put("currentDueDate",    d.getDueDate().toString());
                data.put("currentPremium",    d.getPremiumAmount());
                data.put("currentLateFee",    d.getLateFee());
                data.put("currentTotalDue",   d.getTotalDue());
                data.put("currentPaid",       d.isPaid());
                data.put("currentOverdue",    d.isOverdue());
                data.put("currentOverdueTier",d.getOverdueTier());
            });

        return ResponseEntity.ok(data);
    }
}
