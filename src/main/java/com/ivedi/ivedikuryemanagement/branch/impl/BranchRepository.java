package com.ivedi.ivedikuryemanagement.branch.impl;

import com.ivedi.ivedikuryemanagement.branch.api.BranchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID>, JpaSpecificationExecutor<Branch> {

    boolean existsByNameAndRestaurant_Id(String name, UUID restaurantId);

    boolean existsByNameAndRestaurant_IdAndIdNot(String name, UUID restaurantId, UUID id);

    List<Branch> findAllByRestaurant_IdOrderByNameAsc(UUID restaurantId);

    List<Branch> findAllByRestaurant_IdAndStatusOrderByNameAsc(UUID restaurantId, BranchStatus status);

    @Query("""
            SELECT b FROM Branch b
            JOIN FETCH b.restaurant r
            LEFT JOIN FETCH b.region
            WHERE r.owner.id = :ownerId
            ORDER BY b.name ASC
            """)
    List<Branch> findAllByRestaurantOwnerIdWithDetails(@Param("ownerId") UUID ownerId);

    List<Branch> findAllByRegion_IdOrderByNameAsc(UUID regionId);

    @Query("""
            SELECT b.restaurant.id, COUNT(b)
            FROM Branch b
            WHERE b.restaurant.id IN :restaurantIds
            GROUP BY b.restaurant.id
            """)
    List<Object[]> countByRestaurantIds(@Param("restaurantIds") List<UUID> restaurantIds);

    /** Eager-loads restaurant and region to avoid N+1 on single-entity reads. */
    @Query("""
            SELECT b FROM Branch b
            JOIN FETCH b.restaurant
            JOIN FETCH b.region
            WHERE b.id = :id
            """)
    Optional<Branch> findByIdWithDetails(@Param("id") UUID id);

    /** Used for owner data isolation: returns branch only if it belongs to the given restaurant. */
    @Query("""
            SELECT b FROM Branch b
            JOIN FETCH b.restaurant r
            JOIN FETCH b.region
            WHERE b.id = :branchId AND r.id = :restaurantId
            """)
    Optional<Branch> findByIdAndRestaurant_Id(
            @Param("branchId") UUID branchId,
            @Param("restaurantId") UUID restaurantId
    );
}
