package com.ivedi.ivedikuryemanagement.commission.impl;

import com.ivedi.ivedikuryemanagement.commission.api.CommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CommissionRepository extends JpaRepository<Commission, UUID> {

    @Query("""
            SELECT c FROM Commission c
            JOIN FETCH c.deliveryOrder o
            JOIN FETCH c.courier cr
            JOIN FETCH cr.user
            WHERE cr.id = :courierId
            ORDER BY c.createdAt DESC
            """)
    Page<Commission> findByCourierId(@Param("courierId") UUID courierId, Pageable pageable);

    @Query("""
            SELECT c FROM Commission c
            JOIN FETCH c.deliveryOrder o
            JOIN FETCH c.courier cr
            JOIN FETCH cr.user
            WHERE c.id = :id
            """)
    java.util.Optional<Commission> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT c FROM Commission c
            JOIN FETCH c.deliveryOrder o
            JOIN FETCH c.courier cr
            JOIN FETCH cr.user
            WHERE (:courierId IS NULL OR cr.id = :courierId)
              AND (:status IS NULL OR c.status = :status)
              AND (:period IS NULL OR c.paymentPeriod = :period)
            ORDER BY c.createdAt DESC
            """)
    Page<Commission> search(
            @Param("courierId") UUID courierId,
            @Param("status") CommissionStatus status,
            @Param("period") String period,
            Pageable pageable
    );

    /** Earnings summary aggregation for a courier, optionally filtered by period. */
    @Query(value = """
            SELECT
              COUNT(c.id) AS totalCount,
              COALESCE(SUM(c.gross_amount), 0) AS grossTotal,
              COALESCE(SUM(c.net_amount), 0) AS netTotal,
              SUM(CASE WHEN c.status = 'PENDING' THEN 1 ELSE 0 END) AS pendingCount,
              COALESCE(SUM(CASE WHEN c.status = 'PENDING' THEN c.net_amount ELSE 0 END), 0) AS pendingTotal,
              SUM(CASE WHEN c.status = 'PAID' THEN 1 ELSE 0 END) AS paidCount,
              COALESCE(SUM(CASE WHEN c.status = 'PAID' THEN c.net_amount ELSE 0 END), 0) AS paidTotal
            FROM commissions c
            WHERE c.courier_id = :courierId
              AND (:period IS NULL OR c.payment_period = :period)
            """, nativeQuery = true)
    EarningsSummaryProjection aggregateEarnings(
            @Param("courierId") UUID courierId,
            @Param("period") String period
    );

    @Query(value = """
            SELECT
              COUNT(c.id) AS totalDeliveries,
              COALESCE(SUM(CASE WHEN c.status = 'PENDING' THEN c.net_amount ELSE 0 END), 0) AS pendingAmount,
              COALESCE(SUM(CASE WHEN c.status = 'PAID' THEN c.net_amount ELSE 0 END), 0) AS paidAmount,
              COALESCE(SUM(c.net_amount), 0) AS totalAmount,
              COALESCE(SUM(CASE WHEN c.payment_period = :currentPeriod THEN c.net_amount ELSE 0 END), 0) AS currentPeriodAmount
            FROM commissions c
            WHERE c.courier_id = :courierId
            """, nativeQuery = true)
    CommissionSummaryProjection aggregateFrontendSummary(
            @Param("courierId") UUID courierId,
            @Param("currentPeriod") String currentPeriod
    );

    List<Commission> findAllByCourier_IdAndStatusAndPaymentPeriod(
            UUID courierId, CommissionStatus status, String period);

    @Modifying
    @Query("""
            UPDATE Commission c
            SET c.status = 'PAID', c.paidAt = :paidAt
            WHERE c.courier.id = :courierId
              AND c.status = 'PENDING'
              AND c.paymentPeriod = :period
            """)
    int markAsPaid(
            @Param("courierId") UUID courierId,
            @Param("period") String period,
            @Param("paidAt") OffsetDateTime paidAt
    );

    @Modifying
    @Query("""
            UPDATE Commission c
            SET c.status = 'PAID', c.paidAt = :paidAt
            WHERE c.id = :id
              AND c.status = 'PENDING'
            """)
    int markOneAsPaid(@Param("id") UUID id, @Param("paidAt") OffsetDateTime paidAt);

    @Query("""
            SELECT c.deliveryOrder.branch.restaurant.id, COALESCE(SUM(c.netAmount), 0)
            FROM Commission c
            WHERE c.deliveryOrder.branch.restaurant.id IN :restaurantIds
            GROUP BY c.deliveryOrder.branch.restaurant.id
            """)
    List<Object[]> sumNetAmountByRestaurantIds(@Param("restaurantIds") List<UUID> restaurantIds);
}
