package com.ivedi.ivedikuryemanagement.delivery.impl;

import com.ivedi.ivedikuryemanagement.delivery.api.DeliveryOrderStatus;
import com.ivedi.ivedikuryemanagement.delivery.api.OrderPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface DeliveryOrderRepository
        extends JpaRepository<DeliveryOrder, UUID>, JpaSpecificationExecutor<DeliveryOrder> {

    boolean existsByOrderCodeAndBranch_Id(String orderCode, UUID branchId);

    /** Eager-loads branch → restaurant for authorization checks. */
    @Query("""
            SELECT d FROM DeliveryOrder d
            JOIN FETCH d.branch b
            JOIN FETCH b.restaurant
            WHERE d.id = :id
            """)
    Optional<DeliveryOrder> findByIdWithBranch(@Param("id") UUID id);

    /** All orders belonging to the given branches (restaurant data isolation). */
    @Query("""
            SELECT d FROM DeliveryOrder d
            JOIN FETCH d.branch b
            JOIN FETCH b.restaurant
            WHERE b.id IN :branchIds
            ORDER BY d.createdAt DESC
            """)
    List<DeliveryOrder> findAllByBranchIds(@Param("branchIds") List<UUID> branchIds);

    /**
     * Dispatch queue — READY_FOR_DISPATCH, priority-sorted then oldest first.
     * URGENT → HIGH → NORMAL, then submittedAt ASC within each tier.
     * Native query used because JPQL CASE+enum ordering is not portable across Hibernate versions.
     */
    default List<DeliveryOrder> findReadyForDispatchOrdered() {
        return findReadyForDispatchOrdered(
                DeliveryOrderStatus.READY_FOR_DISPATCH,
                OrderPriority.URGENT,
                OrderPriority.HIGH
        );
    }

    @Query("""
            SELECT d FROM DeliveryOrder d
            JOIN FETCH d.branch b
            JOIN FETCH b.restaurant
            WHERE d.status = :status
            ORDER BY
              CASE
                WHEN d.priority = :urgent THEN 0
                WHEN d.priority = :high THEN 1
                ELSE 2
              END ASC,
              d.submittedAt ASC
            """)
    List<DeliveryOrder> findReadyForDispatchOrdered(
            @Param("status") DeliveryOrderStatus status,
            @Param("urgent") OrderPriority urgent,
            @Param("high") OrderPriority high
    );

    /** Active orders (ASSIGNED + PICKED_UP), oldest assigned first. */
    @Query("""
            SELECT d FROM DeliveryOrder d
            JOIN FETCH d.branch b
            JOIN FETCH b.restaurant
            WHERE d.status IN :statuses
            ORDER BY d.assignedAt ASC
            """)
    List<DeliveryOrder> findActiveOrders(@Param("statuses") List<DeliveryOrderStatus> statuses);

    @Query("""
            SELECT d.branch.restaurant.id, COUNT(d)
            FROM DeliveryOrder d
            WHERE d.branch.restaurant.id IN :restaurantIds
              AND d.createdAt >= :from
              AND d.createdAt < :to
            GROUP BY d.branch.restaurant.id
            """)
    List<Object[]> countTodayByRestaurantIds(
            @Param("restaurantIds") List<UUID> restaurantIds,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
