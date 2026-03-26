package com.insurance.autoinsurance.repository;
import com.insurance.autoinsurance.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByPolicy_PolicyIdOrderByPaymentDateDesc(Long policyId);

    List<Payment> findByPaymentStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);

    long countByPaymentStatus(Payment.PaymentStatus status);

    List<Payment> findTop5ByOrderByCreatedAtDesc();

    List<Payment> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p WHERE p.paymentStatus = :status")
    Double sumByStatus(@Param("status") Payment.PaymentStatus status);

    @Query("SELECT EXTRACT(MONTH FROM p.paymentDate), COUNT(p) FROM Payment p WHERE p.paymentDate IS NOT NULL GROUP BY EXTRACT(MONTH FROM p.paymentDate) ORDER BY EXTRACT(MONTH FROM p.paymentDate)")
    List<Object[]> paymentCountsByMonth();

    @Query("SELECT EXTRACT(MONTH FROM p.paymentDate), COALESCE(SUM(p.paymentAmount), 0) FROM Payment p WHERE p.paymentStatus = com.insurance.autoinsurance.model.Payment.PaymentStatus.SUCCESS AND p.paymentDate IS NOT NULL GROUP BY EXTRACT(MONTH FROM p.paymentDate) ORDER BY EXTRACT(MONTH FROM p.paymentDate)")
    List<Object[]> revenueByMonth();

    @Query("SELECT p.policy.policyName, COALESCE(SUM(p.paymentAmount), 0) FROM Payment p WHERE p.paymentStatus = com.insurance.autoinsurance.model.Payment.PaymentStatus.SUCCESS GROUP BY p.policy.policyName ORDER BY SUM(p.paymentAmount) DESC")
    List<Object[]> revenueGroupedByPolicy();

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentStatus = com.insurance.autoinsurance.model.Payment.PaymentStatus.FAILED AND p.paymentDate = :today")
    long countFailedToday(@Param("today") LocalDate today);

    /** JOIN FETCH ensures policy is loaded eagerly — safe with open-in-view=false */
    @Query("SELECT p FROM Payment p JOIN FETCH p.policy WHERE p.paymentStatus = :status ORDER BY p.createdAt DESC")
    List<Payment> findByStatusWithPolicy(@Param("status") Payment.PaymentStatus status);

    /** Revenue report: load only SUCCESS payments with policy eagerly, filtered by date range */
    @Query("SELECT p FROM Payment p JOIN FETCH p.policy WHERE p.paymentStatus = com.insurance.autoinsurance.model.Payment.PaymentStatus.SUCCESS AND p.paymentDate >= :from AND p.paymentDate <= :to ORDER BY p.paymentDate DESC")
    List<Payment> findSuccessWithPolicyBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
