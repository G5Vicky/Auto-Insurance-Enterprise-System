package com.insurance.autoinsurance.controller;
import com.insurance.autoinsurance.dto.PaymentDetailsView;
import com.insurance.autoinsurance.model.Notification;
import com.insurance.autoinsurance.model.Payment;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.service.NotificationService;
import com.insurance.autoinsurance.service.PaymentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/payments")
public class PaymentController {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final PaymentService      paymentService;
    private final NotificationService notifService;

    public PaymentController(PaymentService ps, NotificationService ns) {
        this.paymentService = ps; this.notifService = ns;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status, Model model) {
        List<PaymentDetailsView> views;
        if (status != null && !status.isBlank()) {
            try {
                Payment.PaymentStatus ps = Payment.PaymentStatus.valueOf(status.toUpperCase());
                views = paymentService.getViewsByStatus(ps);
                model.addAttribute("appliedStatus", ps);
            } catch (IllegalArgumentException e) {
                views = paymentService.getAllViews();
            }
        } else {
            views = paymentService.getAllViews();
        }
        model.addAttribute("paymentViews", views);
        model.addAttribute("statuses",     Payment.PaymentStatus.values());
        model.addAttribute("activePage",   "payments");
        return "payments/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PaymentDetailsView view = paymentService.getViewById(id)
            .orElseThrow(() -> new IllegalArgumentException("Payment #" + id + " not found."));
        model.addAttribute("paymentView", view);
        model.addAttribute("activePage",  "payments");
        return "payments/detail";
    }

    @GetMapping("/make")
    public String makeForm(@RequestParam(required = false) Long policyId,
                           @RequestParam(required = false) Double amount,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           Model model) {
        if (policyId != null) model.addAttribute("prefillPolicyId", policyId);
        if (amount   != null) model.addAttribute("prefillAmount",   amount);
        if (date     != null) model.addAttribute("prefillDate",     date.format(DF));
        model.addAttribute("activePage", "payments");
        return "payments/make";
    }

    @PostMapping("/make")
    public String makeSubmit(@RequestParam Long policyId,
                             @RequestParam Double paymentAmount,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate paymentDate,
                             @RequestParam(required = false, defaultValue = "") String remarks,
                             RedirectAttributes flash) {
        Payment p = paymentService.makePayment(policyId, paymentAmount, paymentDate, remarks);
        // Push notification
        if (p.getPaymentStatus() == Payment.PaymentStatus.FAILED) {
            notifService.push(Notification.Type.PAYMENT_FAILED,
                "Payment Failed",
                "Payment of ₹" + String.format("%.2f", paymentAmount) + " for Policy #" + policyId + " failed.",
                p.getPaymentId(), "Payment");
        } else if (p.getPaymentStatus() == Payment.PaymentStatus.SUCCESS) {
            notifService.push(Notification.Type.PAYMENT_SUCCESS,
                "Payment Successful",
                "Payment of ₹" + String.format("%.2f", paymentAmount) + " for Policy #" + policyId + " succeeded.",
                p.getPaymentId(), "Payment");
        }
        flash.addFlashAttribute("successMsg",
            "Payment of ₹" + String.format("%.2f", paymentAmount) +
            " — Status: " + p.getPaymentStatus().getLabel());
        return "redirect:/payments/" + p.getPaymentId();
    }

    @PostMapping("/{id}/retry")
    public String retry(@PathVariable Long id, RedirectAttributes flash) {
        Payment p = paymentService.retryPayment(id);
        flash.addFlashAttribute("successMsg",
            "Retry submitted — Status: " + p.getPaymentStatus().getLabel());
        return "redirect:/payments/" + p.getPaymentId();
    }

    @GetMapping("/byPolicy/{policyId}")
    public String byPolicy(@PathVariable Long policyId, Model model) {
        model.addAttribute("payments",   paymentService.getByPolicy(policyId));
        model.addAttribute("policyId",   policyId);
        model.addAttribute("activePage", "payments");
        paymentService.getPolicyDetails(policyId)
            .ifPresent(p -> model.addAttribute("policy", p));
        return "payments/byPolicy";
    }

    @GetMapping("/api/policyDetails")
    @ResponseBody
    public ResponseEntity<?> policyDetails(@RequestParam Long policyId) {
        Optional<Policy> opt = paymentService.getPolicyDetails(policyId);
        if (opt.isEmpty()) return ResponseEntity.badRequest()
            .body(Map.of("error", "Policy #" + policyId + " not found."));
        Policy p = opt.get();
        LocalDate next = paymentService.getNextPaymentDate(policyId);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("policyId",         p.getPolicyId());
        data.put("policyName",       p.getPolicyName());
        data.put("policyHolderName", p.getPolicyHolderName());
        data.put("policyAmount",     p.getPolicyAmount());
        data.put("policyStatus",     p.getPolicyStatus().name());
        data.put("statusLabel",      p.getPolicyStatus().getLabel());
        data.put("vehicleNumber",    p.getVehicleNumber());
        data.put("vehicleModel",     p.getVehicleModel());
        data.put("holderEmail",      p.getHolderEmail());
        data.put("holderPhone",      p.getHolderPhone());
        data.put("coverageType",     p.getCoverageType());
        data.put("policyStartDate",  p.getPolicyStartDate() != null ? p.getPolicyStartDate().format(DF) : null);
        data.put("policyEndDate",    p.getPolicyEndDate()   != null ? p.getPolicyEndDate().format(DF)   : null);
        data.put("nextPaymentDate",  next != null ? next.format(DF) : LocalDate.now().format(DF));
        data.put("active",           p.getPolicyStatus() == Policy.PolicyStatus.ACTIVE);
        return ResponseEntity.ok(data);
    }
}
