package com.insurance.autoinsurance.controller;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insurance.autoinsurance.dto.DashboardStats;
import com.insurance.autoinsurance.service.NotificationService;
import com.insurance.autoinsurance.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {
    private final PaymentService      paymentService;
    private final NotificationService notifService;
    private final ObjectMapper        om;

    public DashboardController(PaymentService ps, NotificationService ns, ObjectMapper om) {
        this.paymentService = ps; this.notifService = ns; this.om = om;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        DashboardStats stats = paymentService.buildStats();
        model.addAttribute("stats",        stats);
        model.addAttribute("activePage",   "dashboard");
        model.addAttribute("unreadNotifs", notifService.getUnreadCount());
        addJson(model, "monthlyLabelsJson",     stats.getMonthlyPaymentCounts().keySet());
        addJson(model, "monthlyCountsJson",     stats.getMonthlyPaymentCounts().values());
        addJson(model, "monthlyRevenueJson",    stats.getMonthlyRevenue().values());
        addJson(model, "statusLabelsJson",      stats.getPaymentStatusDistribution().keySet());
        addJson(model, "statusCountsJson",      stats.getPaymentStatusDistribution().values());
        addJson(model, "revPolicyLabelsJson",   stats.getRevenueByPolicy().keySet());
        addJson(model, "revPolicyValuesJson",   stats.getRevenueByPolicy().values());
        addJson(model, "policyStatusLabelsJson",stats.getPolicyStatusDistribution().keySet());
        addJson(model, "policyStatusCountsJson",stats.getPolicyStatusDistribution().values());
        return "dashboard";
    }

    private void addJson(Model model, String key, Object value) {
        try   { model.addAttribute(key, om.writeValueAsString(value)); }
        catch (JsonProcessingException e) { model.addAttribute(key, "[]"); }
    }
}
