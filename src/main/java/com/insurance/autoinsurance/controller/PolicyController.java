package com.insurance.autoinsurance.controller;
import com.insurance.autoinsurance.audit.AuditService;
import com.insurance.autoinsurance.model.Notification;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.service.NotificationService;
import com.insurance.autoinsurance.service.PolicyService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/policies")
public class PolicyController {
    private final PolicyService        policyService;
    private final AuditService         auditService;
    private final NotificationService  notifService;

    public PolicyController(PolicyService ps, AuditService as, NotificationService ns) {
        this.policyService = ps; this.auditService = as; this.notifService = ns;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Long id,
                       Model model) {
        List<Policy> policies;
        if (id != null) {
            policies = policyService.getPolicyById(id).map(List::of).orElse(List.of());
            model.addAttribute("searchId", id);
        } else if (q != null && !q.isBlank()) {
            policies = policyService.searchPolicies(q);
            model.addAttribute("searchQuery", q);
        } else if (status != null && !status.isBlank()) {
            try {
                Policy.PolicyStatus ps = Policy.PolicyStatus.valueOf(status.toUpperCase());
                policies = policyService.getPoliciesByStatus(ps);
                model.addAttribute("appliedStatus", ps);
            } catch (IllegalArgumentException e) {
                policies = policyService.getAllPolicies();
            }
        } else {
            policies = policyService.getAllPolicies();
        }
        model.addAttribute("policies",   policies);
        model.addAttribute("statuses",   Policy.PolicyStatus.values());
        model.addAttribute("totalCount", policyService.getTotalPolicies());
        model.addAttribute("activePage", "policies");
        return "policies/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Policy policy = policyService.getPolicyByIdOrThrow(id);
        model.addAttribute("policy",    policy);
        model.addAttribute("auditLogs", auditService.getLogsForEntity("Policy", id));
        model.addAttribute("statuses",  Policy.PolicyStatus.values());
        model.addAttribute("activePage","policies");
        return "policies/detail";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("policy",   new Policy());
        model.addAttribute("statuses", Policy.PolicyStatus.values());
        model.addAttribute("mode",     "add");
        model.addAttribute("activePage","policies");
        return "policies/form";
    }

    @PostMapping("/add")
    public String addSubmit(@Valid @ModelAttribute Policy policy, BindingResult result,
                            Model model, RedirectAttributes flash, Principal principal) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", Policy.PolicyStatus.values());
            model.addAttribute("mode",     "add");
            model.addAttribute("activePage","policies");
            return "policies/form";
        }
        // Set the creator from the authenticated user
        if (principal != null) policy.setCreatedBy(principal.getName());
        Policy saved = policyService.createPolicy(policy);
        notifService.push(Notification.Type.POLICY_CREATED,
            "New Policy Created",
            "Policy \"" + saved.getPolicyName() + "\" created for " + saved.getPolicyHolderName(),
            saved.getPolicyId(), "Policy");
        flash.addFlashAttribute("successMsg",
            "Policy \"" + saved.getPolicyName() + "\" created successfully!");
        return "redirect:/policies/" + saved.getPolicyId();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("policy",   policyService.getPolicyByIdOrThrow(id));
        model.addAttribute("statuses", Policy.PolicyStatus.values());
        model.addAttribute("mode",     "edit");
        model.addAttribute("activePage","policies");
        return "policies/form";
    }

    @PostMapping("/{id}/edit")
    public String editSubmit(@PathVariable Long id,
                             @Valid @ModelAttribute Policy policy,
                             BindingResult result,
                             Model model, RedirectAttributes flash, Principal principal) {
        if (result.hasErrors()) {
            model.addAttribute("statuses", Policy.PolicyStatus.values());
            model.addAttribute("mode",     "edit");
            model.addAttribute("activePage","policies");
            return "policies/form";
        }
        policy.setPolicyId(id);
        if (principal != null) policy.setCreatedBy(principal.getName());
        policyService.updatePolicy(policy);
        flash.addFlashAttribute("successMsg", "Policy updated successfully.");
        return "redirect:/policies/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam String status,
                               RedirectAttributes flash) {
        try {
            Policy.PolicyStatus ns = Policy.PolicyStatus.valueOf(status.toUpperCase());
            policyService.changeStatus(id, ns);
            flash.addFlashAttribute("successMsg", "Status changed to " + ns.getLabel());
        } catch (IllegalArgumentException e) {
            flash.addFlashAttribute("errorMsg", "Invalid status: " + status);
        }
        return "redirect:/policies/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        Policy p = policyService.getPolicyByIdOrThrow(id);
        policyService.deletePolicy(id);
        flash.addFlashAttribute("successMsg",
            "Policy \"" + p.getPolicyName() + "\" deleted.");
        return "redirect:/policies";
    }
}
