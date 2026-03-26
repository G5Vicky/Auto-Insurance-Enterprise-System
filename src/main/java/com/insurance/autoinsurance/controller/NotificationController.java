package com.insurance.autoinsurance.controller;
import com.insurance.autoinsurance.model.Notification;
import com.insurance.autoinsurance.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService ns;
    public NotificationController(NotificationService ns) { this.ns = ns; }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notifications", ns.getRecent());
        model.addAttribute("unreadCount",   ns.getUnreadCount());
        model.addAttribute("activePage",    "notifications");
        return "notifications/list";
    }

    @PostMapping("/mark-all-read")
    public String markAll(RedirectAttributes flash) {
        ns.markAllRead();
        flash.addFlashAttribute("successMsg", "All notifications marked as read.");
        return "redirect:/notifications";
    }

    /** Used by topbar bell dropdown (async fetch) */
    @GetMapping("/api/recent")
    @ResponseBody
    public ResponseEntity<?> recentApi() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM, HH:mm");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : ns.getRecent()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",      n.getNotifId());
            m.put("title",   n.getTitle());
            m.put("message", n.getMessage());
            m.put("type",    n.getType().name());
            m.put("read",    n.isRead());
            m.put("time",    n.getCreatedAt() != null ? n.getCreatedAt().format(fmt) : "");
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/unread-count")
    @ResponseBody
    public ResponseEntity<?> unreadCount() {
        return ResponseEntity.ok(Map.of("count", ns.getUnreadCount()));
    }
}
