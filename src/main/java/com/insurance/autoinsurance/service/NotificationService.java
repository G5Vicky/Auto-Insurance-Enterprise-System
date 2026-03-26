package com.insurance.autoinsurance.service;
import com.insurance.autoinsurance.model.Notification;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.NotificationRepository;
import com.insurance.autoinsurance.repository.PolicyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository repo;
    private final PolicyRepository       polRepo;

    public NotificationService(NotificationRepository repo, PolicyRepository polRepo) {
        this.repo = repo; this.polRepo = polRepo;
    }

    public void push(Notification.Type type, String title, String msg, Long refId, String refType) {
        repo.save(Notification.builder()
            .type(type).title(title).message(msg)
            .referenceId(refId).referenceType(refType).build());
    }

    public List<Notification> getRecent()   { return repo.findTop20ByOrderByCreatedAtDesc(); }
    public List<Notification> getUnread()   { return repo.findByReadFalseOrderByCreatedAtDesc(); }
    public long getUnreadCount()            { return repo.countByReadFalse(); }
    public void markAllRead()               { repo.markAllRead(); }
    public void markRead(Long id)           { repo.findById(id).ifPresent(n -> { n.setRead(true); repo.save(n); }); }

    /** Scans policies and creates expiry notifications for those expiring in ≤30 days */
    public void generateExpiryAlerts() {
        List<Policy> expiring = polRepo.findPoliciesExpiringBetween(
            LocalDate.now(), LocalDate.now().plusDays(30));
        for (Policy p : expiring) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), p.getPolicyEndDate());
            push(Notification.Type.EXPIRY_WARNING,
                "Policy Expiring Soon",
                "Policy \"" + p.getPolicyName() + "\" for " + p.getPolicyHolderName() +
                " expires in " + days + " day(s). Consider renewal.",
                p.getPolicyId(), "Policy");
        }
    }
}
