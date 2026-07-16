package com.ivedi.ivedikuryemanagement.courier.impl;

import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourierRepository extends JpaRepository<Courier, UUID>, JpaSpecificationExecutor<Courier> {

    boolean existsByUser_Id(UUID userId);

    Optional<Courier> findByUser_Id(UUID userId);

    List<Courier> findAllByUser_IdIn(List<UUID> userIds);

    /** Dispatch panel: APPROVED + AVAILABLE, highest rated first. */
    @Query("""
            SELECT c FROM Courier c
            JOIN FETCH c.user
            WHERE c.approvalStatus    = :approval
              AND c.availabilityStatus = :availability
            ORDER BY c.rating DESC, c.activeAssignmentCount ASC
            """)
    List<Courier> findAvailableForDispatch(
            @Param("approval")     ApprovalStatus     approval,
            @Param("availability") AvailabilityStatus availability
    );

    /** Eager-load user for single-entity reads. */
    @Query("SELECT c FROM Courier c JOIN FETCH c.user LEFT JOIN FETCH c.region WHERE c.id = :id")
    Optional<Courier> findByIdWithUser(@Param("id") UUID id);

    /** Eager-load user for the current courier's own profile. */
    @Query("SELECT c FROM Courier c JOIN FETCH c.user LEFT JOIN FETCH c.region WHERE c.user.id = :userId")
    Optional<Courier> findByUserIdWithUser(@Param("userId") UUID userId);
}
