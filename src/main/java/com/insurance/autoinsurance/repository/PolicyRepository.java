package com.insurance.autoinsurance.repository;
import com.insurance.autoinsurance.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findAllByOrderByCreatedAtDesc();

    List<Policy> findByPolicyStatus(Policy.PolicyStatus status);

    long countByPolicyStatus(Policy.PolicyStatus status);

    @Query("SELECT p FROM Policy p WHERE " +
           "LOWER(p.policyName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.policyHolderName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.vehicleNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.vehicleModel) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.coverageType) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "ORDER BY p.createdAt DESC")
    List<Policy> search(@Param("q") String query);

    @Query("SELECT p FROM Policy p WHERE p.policyEndDate BETWEEN :from AND :to AND p.policyStatus = com.insurance.autoinsurance.model.Policy.PolicyStatus.ACTIVE ORDER BY p.policyEndDate ASC")
    List<Policy> findPoliciesExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT p.policyStatus, COUNT(p) FROM Policy p GROUP BY p.policyStatus")
    List<Object[]> countGroupedByStatus();
}
