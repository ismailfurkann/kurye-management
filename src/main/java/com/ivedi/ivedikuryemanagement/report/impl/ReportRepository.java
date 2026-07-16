package com.ivedi.ivedikuryemanagement.report.impl;

import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<DeliveryOrder, UUID> {

    /** Delivery counts grouped by status, optionally scoped to a branch or restaurant. */
    @Query(value = """
            SELECT
              d.status                                                    AS status,
              COUNT(*)                                                    AS cnt,
              COALESCE(SUM(d.delivery_fee), 0)                           AS total_fee
            FROM delivery_orders d
            JOIN branches b ON b.id = d.branch_id
            WHERE (:branchId     IS NULL OR d.branch_id = :branchId)
              AND (:restaurantId IS NULL OR b.restaurant_id = :restaurantId)
              AND (:from IS NULL OR d.created_at >= :from)
              AND (:to   IS NULL OR d.created_at <  :to)
            GROUP BY d.status
            """, nativeQuery = true)
    List<StatusAggregateProjection> aggregateByStatus(
            @Param("branchId")     UUID branchId,
            @Param("restaurantId") UUID restaurantId,
            @Param("from")         OffsetDateTime from,
            @Param("to")           OffsetDateTime to
    );

    @Query(value = """
            SELECT
              (SELECT COUNT(*) FROM restaurants) AS totalRestaurants,
              (SELECT COUNT(*) FROM branches) AS totalBranches,
              (SELECT COUNT(*) FROM couriers) AS totalCouriers,
              (SELECT COUNT(*) FROM couriers WHERE approval_status = 'APPROVED' AND availability_status IN ('AVAILABLE', 'BUSY')) AS activeCouriers,
              (SELECT COUNT(*) FROM couriers WHERE approval_status = 'PENDING') AS pendingApprovalCouriers,
              (SELECT COUNT(*) FROM delivery_orders WHERE status = 'DELIVERED' AND delivered_at >= :todayStart AND delivered_at < :tomorrowStart) AS todayDeliveries,
              (SELECT COALESCE(SUM(net_amount), 0) FROM commissions) AS totalCommission,
              (SELECT COUNT(*) FROM delivery_orders WHERE status = 'FAILED') AS failedDeliveries
            """, nativeQuery = true)
    AdminDashboardProjection adminDashboard(
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("tomorrowStart") OffsetDateTime tomorrowStart
    );

    @Query(value = """
            SELECT
              (SELECT COUNT(*) FROM delivery_orders WHERE created_at >= :todayStart AND created_at < :tomorrowStart) AS todayOrders,
              (SELECT COUNT(*) FROM delivery_orders WHERE status = 'READY_FOR_DISPATCH') AS readyForDispatch,
              (SELECT COUNT(DISTINCT delivery_order_id) FROM delivery_assignments WHERE status IN ('ASSIGNED', 'ACCEPTED', 'PICKED_UP')) AS activeDeliveries,
              (SELECT COUNT(*) FROM delivery_orders WHERE status = 'DELIVERED' AND delivered_at >= :todayStart AND delivered_at < :tomorrowStart) AS delivered,
              (SELECT COUNT(*) FROM delivery_orders WHERE status = 'FAILED' AND failed_at >= :todayStart AND failed_at < :tomorrowStart) AS failed,
              (SELECT COUNT(*) FROM delivery_orders WHERE status = 'CANCELLED' AND cancelled_at >= :todayStart AND cancelled_at < :tomorrowStart) AS cancelled,
              (SELECT COUNT(*) FROM couriers WHERE approval_status = 'APPROVED' AND availability_status IN ('AVAILABLE', 'BUSY')) AS activeCouriers,
              (SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (delivered_at - COALESCE(picked_up_at, assigned_at))) / 60), 0)
                 FROM delivery_orders
                 WHERE status = 'DELIVERED'
                   AND delivered_at IS NOT NULL
                   AND COALESCE(picked_up_at, assigned_at) IS NOT NULL
                   AND delivered_at >= COALESCE(picked_up_at, assigned_at)) AS averageDeliveryMinutes
            """, nativeQuery = true)
    DashboardProjection managerDashboard(
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("tomorrowStart") OffsetDateTime tomorrowStart
    );

    @Query(value = """
            SELECT
              (SELECT COUNT(*) FROM delivery_orders d JOIN branches b ON b.id = d.branch_id
                WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                  AND (:branchId IS NULL OR d.branch_id = :branchId)
                  AND d.created_at >= :todayStart AND d.created_at < :tomorrowStart) AS todayOrders,
              (SELECT COUNT(*) FROM delivery_orders d JOIN branches b ON b.id = d.branch_id
                WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                  AND (:branchId IS NULL OR d.branch_id = :branchId)
                  AND d.status = 'READY_FOR_DISPATCH') AS readyForDispatch,
              (SELECT COUNT(DISTINCT a.delivery_order_id) FROM delivery_assignments a
                JOIN delivery_orders d ON d.id = a.delivery_order_id
                JOIN branches b ON b.id = d.branch_id
                WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                  AND (:branchId IS NULL OR d.branch_id = :branchId)
                  AND a.status IN ('ASSIGNED', 'ACCEPTED', 'PICKED_UP')) AS activeDeliveries,
              (SELECT COUNT(*) FROM delivery_orders d JOIN branches b ON b.id = d.branch_id
                WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                  AND (:branchId IS NULL OR d.branch_id = :branchId)
                  AND d.status = 'DELIVERED' AND d.delivered_at >= :todayStart AND d.delivered_at < :tomorrowStart) AS delivered,
              (SELECT COUNT(*) FROM delivery_orders d JOIN branches b ON b.id = d.branch_id
                WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                  AND (:branchId IS NULL OR d.branch_id = :branchId)
                  AND d.status = 'FAILED' AND d.failed_at >= :todayStart AND d.failed_at < :tomorrowStart) AS failed,
              (SELECT COUNT(*) FROM delivery_orders d JOIN branches b ON b.id = d.branch_id
                WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                  AND (:branchId IS NULL OR d.branch_id = :branchId)
                  AND d.status = 'CANCELLED' AND d.cancelled_at >= :todayStart AND d.cancelled_at < :tomorrowStart) AS cancelled,
              0 AS activeCouriers,
              (SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (d.delivered_at - COALESCE(d.picked_up_at, d.assigned_at))) / 60), 0)
                 FROM delivery_orders d JOIN branches b ON b.id = d.branch_id
                 WHERE b.restaurant_id IN (SELECT id FROM restaurants WHERE owner_id = :ownerId)
                   AND (:branchId IS NULL OR d.branch_id = :branchId)
                   AND d.status = 'DELIVERED'
                   AND d.delivered_at IS NOT NULL
                   AND COALESCE(d.picked_up_at, d.assigned_at) IS NOT NULL
                   AND d.delivered_at >= COALESCE(d.picked_up_at, d.assigned_at)) AS averageDeliveryMinutes
            """, nativeQuery = true)
    DashboardProjection restaurantDashboard(
            @Param("ownerId") UUID ownerId,
            @Param("branchId") UUID branchId,
            @Param("todayStart") OffsetDateTime todayStart,
            @Param("tomorrowStart") OffsetDateTime tomorrowStart
    );

    @Query(value = """
            SELECT
              d.id AS orderId,
              d.order_code AS orderCode,
              r.id AS restaurantId,
              r.legal_name AS restaurantName,
              b.id AS branchId,
              b.name AS branchName,
              c.id AS courierId,
              u.full_name AS courierName,
              d.status AS status,
              d.order_amount AS orderAmount,
              d.delivery_fee AS deliveryFee,
              COALESCE(cm.net_amount, 0) AS commissionAmount,
              d.created_at AS createdAt,
              d.delivered_at AS deliveredAt,
              CASE
                WHEN d.delivered_at IS NOT NULL AND COALESCE(d.picked_up_at, d.assigned_at) IS NOT NULL
                     AND d.delivered_at >= COALESCE(d.picked_up_at, d.assigned_at)
                THEN EXTRACT(EPOCH FROM (d.delivered_at - COALESCE(d.picked_up_at, d.assigned_at))) / 60
                ELSE 0
              END AS deliveryMinutes
            FROM delivery_orders d
            JOIN branches b ON b.id = d.branch_id
            JOIN restaurants r ON r.id = b.restaurant_id
            LEFT JOIN delivery_assignments a ON a.delivery_order_id = d.id AND a.status IN ('ASSIGNED', 'ACCEPTED', 'PICKED_UP', 'DELIVERED', 'FAILED')
            LEFT JOIN couriers c ON c.id = a.courier_id
            LEFT JOIN users u ON u.id = c.user_id
            LEFT JOIN commissions cm ON cm.delivery_order_id = d.id
            WHERE (:branchId IS NULL OR d.branch_id = :branchId)
              AND (:restaurantId IS NULL OR b.restaurant_id = :restaurantId)
              AND (:from IS NULL OR d.created_at >= :from)
              AND (:to IS NULL OR d.created_at < :to)
            ORDER BY d.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM delivery_orders d
            JOIN branches b ON b.id = d.branch_id
            WHERE (:branchId IS NULL OR d.branch_id = :branchId)
              AND (:restaurantId IS NULL OR b.restaurant_id = :restaurantId)
              AND (:from IS NULL OR d.created_at >= :from)
              AND (:to IS NULL OR d.created_at < :to)
            """,
            nativeQuery = true)
    Page<DeliveryReportProjection> deliveryReport(
            @Param("branchId") UUID branchId,
            @Param("restaurantId") UUID restaurantId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            Pageable pageable
    );

    /** Courier performance: assignment counts + average delivered duration. */
    @Query(value = """
            SELECT
              c.id AS courierId,
              u.full_name AS courierName,
              c.vehicle_type AS vehicleType,
              c.rating AS rating,
              COUNT(a.id) AS totalAssignments,
              SUM(CASE WHEN a.status = 'DELIVERED' THEN 1 ELSE 0 END) AS deliveredCount,
              SUM(CASE WHEN a.status = 'FAILED' THEN 1 ELSE 0 END) AS failedCount,
              COALESCE(AVG(CASE
                WHEN a.status = 'DELIVERED' AND a.delivered_at IS NOT NULL AND COALESCE(a.picked_up_at, a.accepted_at, a.assigned_at) IS NOT NULL
                     AND a.delivered_at >= COALESCE(a.picked_up_at, a.accepted_at, a.assigned_at)
                THEN EXTRACT(EPOCH FROM (a.delivered_at - COALESCE(a.picked_up_at, a.accepted_at, a.assigned_at))) / 60
                ELSE NULL
              END), 0) AS avgDeliveryMinutes
            FROM delivery_assignments a
            JOIN couriers c ON c.id = a.courier_id
            JOIN users u ON u.id = c.user_id
            WHERE (:courierId IS NULL OR c.id = :courierId)
              AND (:from IS NULL OR a.assigned_at >= :from)
              AND (:to IS NULL OR a.assigned_at < :to)
            GROUP BY c.id, u.full_name, c.vehicle_type, c.rating
            ORDER BY deliveredCount DESC
            """, nativeQuery = true)
    List<CourierPerformanceProjection> courierPerformance(
            @Param("courierId") UUID courierId,
            @Param("from")      OffsetDateTime from,
            @Param("to")        OffsetDateTime to
    );
}
