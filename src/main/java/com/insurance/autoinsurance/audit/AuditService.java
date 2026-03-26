package com.insurance.autoinsurance.audit;
import com.insurance.autoinsurance.model.AuditLog;
import com.insurance.autoinsurance.repository.AuditLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Transactional
public class AuditService {
    private final AuditLogRepository repo;
    public AuditService(AuditLogRepository repo) { this.repo = repo; }

    public void log(AuditLog.ActionType type, String entity, Long id, String desc) {
        AuditLog entry = AuditLog.builder()
            .actionType(type).entityType(entity).entityId(id)
            .description(desc).performedBy("system").build();
        repo.save(entry);
    }

    public List<AuditLog> getLogsForEntity(String entityType, Long entityId) {
        return repo.findByEntityTypeAndEntityIdOrderByActionTimeDesc(entityType, entityId);
    }

    public List<AuditLog> getRecentLogs() {
        return repo.findTop10ByOrderByActionTimeDesc();
    }
}
