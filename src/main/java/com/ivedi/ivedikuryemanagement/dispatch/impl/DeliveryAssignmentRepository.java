package com.ivedi.ivedikuryemanagement.dispatch.impl;

import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAssignmentRepository extends JpaRepository<DeliveryAssignment, UUID> {

    /** Used before assigning: verify no active assignment exists for this order. */
    boolean existsByDeliveryOrder_IdAndStatusIn(UUID orderId, List<AssignmentStatus> statuses);

    /** Returns the single active assignment for an order (at most one enforced by DB constraint). */
    @Query("""
            SELECT a FROM DeliveryAssignment a
            JOIN FETCH a.courier c
            JOIN FETCH c.user
            JOIN FETCH a.deliveryOrder o
            JOIN FETCH o.branch b
            JOIN FETCH b.restaurant
            WHERE a.deliveryOrder.id = :orderId
              AND a.status IN :statuses
            """)
    Optional<DeliveryAssignment> findActiveByOrderId(
            @Param("orderId")  UUID orderId,
            @Param("statuses") List<AssignmentStatus> statuses
    );

    /** Courier's current active assignment — at most one expected. */
    @Query("""
            SELECT a FROM DeliveryAssignment a
            JOIN FETCH a.deliveryOrder o
            JOIN FETCH o.branch b
            JOIN FETCH b.restaurant
            JOIN FETCH a.courier c
            JOIN FETCH c.user
            WHERE a.courier.id = :courierId
              AND a.status IN :statuses
            """)
    List<DeliveryAssignment> findActiveByCourierId(
            @Param("courierId") UUID courierId,
            @Param("statuses")  List<AssignmentStatus> statuses
    );

    /** Full assignment history for an order, oldest first. */
    @Query("""
            SELECT a FROM DeliveryAssignment a
            JOIN FETCH a.courier c
            JOIN FETCH c.user
            WHERE a.deliveryOrder.id = :orderId
            ORDER BY a.assignedAt ASC
            """)
    List<DeliveryAssignment> findAllByOrderIdWithCourier(@Param("orderId") UUID orderId);

    /** Courier's assignment by id — verifies it belongs to this courier. */
    @Query("""
            SELECT a FROM DeliveryAssignment a
            JOIN FETCH a.deliveryOrder o
            JOIN FETCH o.branch b
            JOIN FETCH b.restaurant
            JOIN FETCH a.courier c
            JOIN FETCH c.user
            WHERE a.id = :id AND a.courier.id = :courierId
            """)
    Optional<DeliveryAssignment> findByIdAndCourierId(
            @Param("id")        UUID id,
            @Param("courierId") UUID courierId
    );

    @Query("""
            SELECT a FROM DeliveryAssignment a
            JOIN FETCH a.deliveryOrder o
            JOIN FETCH o.branch b
            JOIN FETCH b.restaurant
            JOIN FETCH a.courier c
            JOIN FETCH c.user
            WHERE a.id = :id
            """)
    Optional<DeliveryAssignment> findByIdWithDetails(@Param("id") UUID id);

    @Query("""
            SELECT a FROM DeliveryAssignment a
            JOIN FETCH a.deliveryOrder o
            JOIN FETCH o.branch b
            JOIN FETCH b.restaurant
            JOIN FETCH a.courier c
            JOIN FETCH c.user
            WHERE a.deliveryOrder.id IN :orderIds
              AND a.status IN :statuses
            """)
    List<DeliveryAssignment> findActiveByOrderIds(
            @Param("orderIds") List<UUID> orderIds,
            @Param("statuses") List<AssignmentStatus> statuses
    );

    @Query(
            value = """
                    SELECT a FROM DeliveryAssignment a
                    JOIN FETCH a.deliveryOrder o
                    JOIN FETCH o.branch b
                    JOIN FETCH b.restaurant
                    JOIN FETCH a.courier c
                    JOIN FETCH c.user
                    WHERE a.courier.id = :courierId
                      AND (:status IS NULL OR a.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(a) FROM DeliveryAssignment a
                    WHERE a.courier.id = :courierId
                      AND (:status IS NULL OR a.status = :status)
                    """
    )
    Page<DeliveryAssignment> findPageByCourierIdAndStatus(
            @Param("courierId") UUID courierId,
            @Param("status") AssignmentStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT a.courier.id, COUNT(a)
            FROM DeliveryAssignment a
            WHERE a.courier.id IN :courierIds
              AND a.status IN :statuses
            GROUP BY a.courier.id
            """)
    List<Object[]> countByCourierIdsAndStatusIn(
            @Param("courierIds") List<UUID> courierIds,
            @Param("statuses") List<AssignmentStatus> statuses
    );
}
