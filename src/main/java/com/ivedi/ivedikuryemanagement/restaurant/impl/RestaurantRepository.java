package com.ivedi.ivedikuryemanagement.restaurant.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID>, JpaSpecificationExecutor<Restaurant> {

    boolean existsByTaxNumber(String taxNumber);

    boolean existsByTaxNumberAndIdNot(String taxNumber, UUID id);

    List<Restaurant> findAllByOwner_IdOrderByNameAsc(UUID ownerId);

    List<Restaurant> findAllByOwner_IdIn(List<UUID> ownerIds);

    Optional<Restaurant> findByIdAndOwner_Id(UUID id, UUID ownerId);

    @Query("SELECT r FROM Restaurant r JOIN FETCH r.owner WHERE r.id = :id")
    Optional<Restaurant> findByIdWithOwner(@Param("id") UUID id);
}
