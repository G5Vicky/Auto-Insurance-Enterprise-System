package com.insurance.autoinsurance.repository;
import com.insurance.autoinsurance.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByEntityTypeAndEntityIdOrderByActionTimeDesc(String entityType, Long entityId);
    List<AuditLog> findTop10ByOrderByActionTimeDesc();
    List<AuditLog> findTop5ByOrderByActionTimeDesc();
}
