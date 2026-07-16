package com.ivedi.ivedikuryemanagement.auth.impl;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserBranchAssignmentRepository extends JpaRepository<UserBranchAssignment, UserBranchAssignmentId> {

    @Query("""
            SELECT a FROM UserBranchAssignment a
            JOIN FETCH a.branch b
            JOIN FETCH b.restaurant
            WHERE a.user.id = :userId
            ORDER BY b.name ASC
            """)
    List<UserBranchAssignment> findAllByUserIdWithBranch(@Param("userId") UUID userId);

    @Query("""
            SELECT a FROM UserBranchAssignment a
            JOIN FETCH a.user
            JOIN FETCH a.branch b
            JOIN FETCH b.restaurant
            WHERE a.user.id IN :userIds
            ORDER BY b.name ASC
            """)
    List<UserBranchAssignment> findAllByUserIdsWithBranch(@Param("userIds") List<UUID> userIds);

    @Modifying
    void deleteAllByUser_Id(UUID userId);
}
