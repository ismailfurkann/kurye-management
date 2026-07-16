package com.ivedi.ivedikuryemanagement.auth.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByRole(UserRole role);

    List<User> findAllByStatus(UserStatus status);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    List<User> findAllByRoleAndStatus(@Param("role") UserRole role, @Param("status") UserStatus status);

    long countByRoleAndStatus(UserRole role, UserStatus status);
}
