package com.insurance.autoinsurance.service;
import com.insurance.autoinsurance.audit.AuditService;
import com.insurance.autoinsurance.model.AuditLog;
import com.insurance.autoinsurance.model.Policy;
import com.insurance.autoinsurance.repository.PolicyRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PolicyService {
    private final PolicyRepository repo;
    private final AuditService audit;

    public PolicyService(PolicyRepository repo, AuditService audit) {
        this.repo = repo; this.audit = audit;
    }

    public Policy createPolicy(Policy p) {
        if (p.getPolicyStatus() == null) p.setPolicyStatus(Policy.PolicyStatus.ACTIVE);
        Policy saved = repo.save(p);
        audit.log(AuditLog.ActionType.CREATE, "Policy", saved.getPolicyId(),
            "Policy created: " + saved.getPolicyName() + " for " + saved.getPolicyHolderName());
        return saved;
    }

    public List<Policy> getAllPolicies() { return repo.findAllByOrderByCreatedAtDesc(); }

    public Optional<Policy> getPolicyById(Long id) { return repo.findById(id); }

    public Policy getPolicyByIdOrThrow(Long id) {
        Policy p = repo.findById(id).orElseThrow(() ->
            new IllegalArgumentException("Policy not found with ID: " + id));
        p.getPayments().size(); // force-init lazy collection within transaction
        return p;
    }

    public List<Policy> getPoliciesByStatus(Policy.PolicyStatus status) {
        return repo.findByPolicyStatus(status);
    }

    public List<Policy> searchPolicies(String query) {
        if (query == null || query.isBlank()) return getAllPolicies();
        return repo.search(query.trim());
    }

    public List<Policy> getPoliciesExpiringWithinDays(int days) {
        return repo.findPoliciesExpiringBetween(LocalDate.now(), LocalDate.now().plusDays(days));
    }

    /**
     * Safe update: loads the existing persisted entity, applies only user-editable fields
     * from the form-bound object, preserving createdAt, createdBy and payments collection.
     */
    public Policy updatePolicy(Policy formData) {
        Long id = formData.getPolicyId();
        if (id == null) throw new IllegalArgumentException("Cannot update — policy ID is null.");
        Policy existing = repo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Cannot update — policy not found."));

        // Copy only form-submitted fields onto the managed entity
        existing.setPolicyName(formData.getPolicyName());
        existing.setPolicyHolderName(formData.getPolicyHolderName());
        existing.setPolicyAmount(formData.getPolicyAmount());
        existing.setPolicyStartDate(formData.getPolicyStartDate());
        existing.setPolicyEndDate(formData.getPolicyEndDate());
        existing.setPolicyStatus(formData.getPolicyStatus() != null
            ? formData.getPolicyStatus() : existing.getPolicyStatus());
        existing.setCoverageType(formData.getCoverageType());
        existing.setDeductibleAmount(formData.getDeductibleAmount());
        existing.setHolderEmail(formData.getHolderEmail());
        existing.setHolderPhone(formData.getHolderPhone());
        existing.setHolderAddress(formData.getHolderAddress());
        existing.setVehicleNumber(formData.getVehicleNumber());
        existing.setVehicleModel(formData.getVehicleModel());
        existing.setVehicleYear(formData.getVehicleYear());
        // Premium schedule fields
        if (formData.getPremiumDayOfMonth() != null) existing.setPremiumDayOfMonth(formData.getPremiumDayOfMonth());
        if (formData.getMonthlyPremium() != null && formData.getMonthlyPremium() > 0) existing.setMonthlyPremium(formData.getMonthlyPremium());
        // Preserve audit fields
        if (formData.getCreatedBy() != null) existing.setCreatedBy(formData.getCreatedBy());

        Policy saved = repo.save(existing);
        audit.log(AuditLog.ActionType.UPDATE, "Policy", saved.getPolicyId(),
            "Policy updated: " + saved.getPolicyName());
        return saved;
    }

    public Policy changeStatus(Long id, Policy.PolicyStatus newStatus) {
        Policy p = getPolicyByIdOrThrow(id);
        Policy.PolicyStatus old = p.getPolicyStatus();
        p.setPolicyStatus(newStatus);
        Policy saved = repo.save(p);
        audit.log(AuditLog.ActionType.STATUS_CHANGE, "Policy", id,
            "Status changed: " + old + " -> " + newStatus);
        return saved;
    }

    public void deletePolicy(Long id) {
        Policy p = repo.findById(id).orElseThrow(() ->
            new IllegalArgumentException("Policy not found with ID: " + id));
        repo.deleteById(id);
        audit.log(AuditLog.ActionType.DELETE, "Policy", id,
            "Policy deleted: " + p.getPolicyName());
    }

    public long countByStatus(Policy.PolicyStatus status) { return repo.countByPolicyStatus(status); }
    public long getTotalPolicies() { return repo.count(); }
    public List<Object[]> getStatusDistribution() { return repo.countGroupedByStatus(); }
}
