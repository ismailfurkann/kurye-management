package com.ivedi.ivedikuryemanagement.auth.impl;

import com.ivedi.ivedikuryemanagement.audit.api.AuditService;
import com.ivedi.ivedikuryemanagement.auth.api.*;
import com.ivedi.ivedikuryemanagement.auth.web.ManagedUserRequest;
import com.ivedi.ivedikuryemanagement.auth.web.ResetPasswordRequest;
import com.ivedi.ivedikuryemanagement.auth.web.UserStatusRequest;
import com.ivedi.ivedikuryemanagement.branch.impl.Branch;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.exception.UnauthorizedException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.impl.Courier;
import com.ivedi.ivedikuryemanagement.courier.impl.CourierRepository;
import com.ivedi.ivedikuryemanagement.region.impl.Region;
import com.ivedi.ivedikuryemanagement.region.impl.RegionRepository;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final CourierRepository courierRepository;
    private final RegionRepository regionRepository;
    private final UserBranchAssignmentRepository branchAssignmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementResponse> search(UserRole role, UserStatus status, String search, UUID restaurantId, int page, int size) {
        UserPrincipal actor = currentPrincipal();
        Specification<User> spec = buildSpec(actor, role, status, search, restaurantId);
        Page<User> users = userRepository.findAll(spec, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        return mapPage(users);
    }

    @Override
    @Transactional(readOnly = true)
    public UserManagementResponse getById(UUID id) {
        User target = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        ensureVisible(currentPrincipal(), target);
        return toResponse(target);
    }

    @Override
    public UserManagementResponse create(ManagedUserRequest request) {
        UserPrincipal actor = currentPrincipal();
        validateCreatePermission(actor, request);
        validatePassword(request.email(), request.temporaryPassword());
        String email = request.email().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email is already registered: " + email);
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(trimOrNull(request.phone()))
                .passwordHash(passwordEncoder.encode(request.temporaryPassword()))
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .mustChangePassword(true)
                .tokenVersion(0)
                .build();
        User saved = userRepository.save(user);

        switch (request.role()) {
            case RESTAURANT_OWNER -> assignRestaurantOwner(actor, saved, request.restaurantId());
            case RESTAURANT_STAFF -> replaceBranchAssignments(actor, saved, request.branchIds());
            case COURIER -> createCourierProfile(saved, request);
            default -> { }
        }

        audit(actor.getId(), "USER_CREATED", saved.getId(), "role=" + saved.getRole());
        return toResponse(saved);
    }

    @Override
    public UserManagementResponse update(UUID id, ManagedUserRequest request) {
        UserPrincipal actor = currentPrincipal();
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        ensureManageable(actor, user);
        if (user.getRole() != request.role()) {
            throw new BusinessException("Changing user role is not supported", "ROLE_CHANGE_NOT_ALLOWED");
        }
        user.setFullName(request.fullName().trim());
        user.setPhone(trimOrNull(request.phone()));
        if (user.getRole() == UserRole.RESTAURANT_STAFF) {
            replaceBranchAssignments(actor, user, request.branchIds());
        }
        User saved = userRepository.save(user);
        audit(actor.getId(), "USER_UPDATED", saved.getId(), "role=" + saved.getRole());
        return toResponse(saved);
    }

    @Override
    public UserManagementResponse setStatus(UUID id, UserStatusRequest request) {
        UserPrincipal actor = currentPrincipal();
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        ensureManageable(actor, user);
        if (user.getId().equals(actor.getId()) && request.status() != UserStatus.ACTIVE) {
            throw new BusinessException("You cannot deactivate your own account", "SELF_DEACTIVATION_NOT_ALLOWED");
        }
        if (user.getRole() == UserRole.PLATFORM_ADMIN
                && user.getStatus() == UserStatus.ACTIVE
                && request.status() != UserStatus.ACTIVE
                && userRepository.countByRoleAndStatus(UserRole.PLATFORM_ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new BusinessException("Last active platform admin cannot be deactivated", "LAST_ADMIN_DEACTIVATION_NOT_ALLOWED");
        }
        user.setStatus(request.status());
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);
        audit(actor.getId(), "USER_STATUS_CHANGED", saved.getId(), "status=" + request.status());
        return toResponse(saved);
    }

    @Override
    public UserManagementResponse resetPassword(UUID id, ResetPasswordRequest request) {
        UserPrincipal actor = currentPrincipal();
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        ensureManageable(actor, user);
        validatePassword(user.getEmail(), request.temporaryPassword());
        user.setPasswordHash(passwordEncoder.encode(request.temporaryPassword()));
        user.setMustChangePassword(true);
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);
        audit(actor.getId(), "USER_PASSWORD_RESET", saved.getId(), "passwordReset=true");
        return toResponse(saved);
    }

    private void validateCreatePermission(UserPrincipal actor, ManagedUserRequest request) {
        UserRole actorRole = actor.getRole();
        UserRole targetRole = request.role();
        if (actorRole == UserRole.PLATFORM_ADMIN) {
            return;
        }
        if (actorRole == UserRole.PLATFORM_MANAGER && targetRole == UserRole.COURIER) {
            return;
        }
        if (actorRole == UserRole.RESTAURANT_OWNER && targetRole == UserRole.RESTAURANT_STAFF) {
            return;
        }
        throw new UnauthorizedException("You are not allowed to create this user");
    }

    private void ensureManageable(UserPrincipal actor, User target) {
        if (actor.getRole() == UserRole.PLATFORM_ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PLATFORM_MANAGER && target.getRole() == UserRole.COURIER) {
            return;
        }
        if (actor.getRole() == UserRole.RESTAURANT_OWNER && target.getRole() == UserRole.RESTAURANT_STAFF) {
            List<UserBranchAssignment> assignments = branchAssignmentRepository.findAllByUserIdWithBranch(target.getId());
            boolean allOwned = !assignments.isEmpty() && assignments.stream()
                    .allMatch(a -> a.getBranch().getRestaurant().getOwner().getId().equals(actor.getId()));
            if (allOwned) {
                return;
            }
        }
        throw new UnauthorizedException("You are not allowed to manage this user");
    }

    private void ensureVisible(UserPrincipal actor, User target) {
        if (actor.getRole() == UserRole.PLATFORM_ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.PLATFORM_MANAGER && target.getRole() == UserRole.COURIER) {
            return;
        }
        if (actor.getRole() == UserRole.RESTAURANT_OWNER && target.getRole() == UserRole.RESTAURANT_STAFF) {
            ensureManageable(actor, target);
            return;
        }
        throw new UnauthorizedException("You are not allowed to view this user");
    }

    private void assignRestaurantOwner(UserPrincipal actor, User owner, UUID restaurantId) {
        if (restaurantId == null) {
            return;
        }
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (restaurant.getOwner() != null && !restaurant.getOwner().getId().equals(owner.getId())) {
            throw new BusinessException("Restaurant already has an owner", "RESTAURANT_OWNER_CONFLICT");
        }
        restaurant.setOwner(owner);
        restaurantRepository.save(restaurant);
        audit(actor.getId(), "RESTAURANT_OWNER_ASSIGNED", owner.getId(), "restaurantId=" + restaurantId);
    }

    private void createCourierProfile(User user, ManagedUserRequest request) {
        if (request.courier() == null) {
            throw new BusinessException("Courier profile details are required", "COURIER_PROFILE_REQUIRED");
        }
        Region region = request.courier().regionId() != null
                ? regionRepository.findById(request.courier().regionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region", request.courier().regionId()))
                : null;
        Courier courier = Courier.builder()
                .user(user)
                .vehicleType(request.courier().vehicleType())
                .plateNumber(trimOrNull(request.courier().plateNumber()))
                .region(region)
                .approvalStatus(ApprovalStatus.APPROVED)
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .build();
        courierRepository.save(courier);
        audit(currentPrincipal().getId(), "COURIER_PROFILE_CREATED_BY_ADMIN", user.getId(), "approval=APPROVED");
    }

    private void replaceBranchAssignments(UserPrincipal actor, User user, List<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            throw new BusinessException("Restaurant staff must be assigned to at least one branch", "STAFF_BRANCH_REQUIRED");
        }
        List<Branch> branches = branchIds.stream()
                .distinct()
                .map(id -> branchRepository.findByIdWithDetails(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Branch", id)))
                .toList();
        UUID restaurantId = branches.getFirst().getRestaurant().getId();
        boolean sameRestaurant = branches.stream().allMatch(b -> b.getRestaurant().getId().equals(restaurantId));
        if (!sameRestaurant) {
            throw new BusinessException("Staff branches must belong to the same restaurant", "STAFF_BRANCH_RESTAURANT_MISMATCH");
        }
        if (actor.getRole() == UserRole.RESTAURANT_OWNER) {
            boolean owned = branches.stream().allMatch(b -> b.getRestaurant().getOwner().getId().equals(actor.getId()));
            if (!owned) {
                throw new UnauthorizedException("You cannot assign staff to another restaurant's branch");
            }
        }
        branchAssignmentRepository.deleteAllByUser_Id(user.getId());
        List<UserBranchAssignment> assignments = branches.stream()
                .map(branch -> UserBranchAssignment.builder()
                        .id(new UserBranchAssignmentId(user.getId(), branch.getId()))
                        .user(user)
                        .branch(branch)
                        .build())
                .toList();
        branchAssignmentRepository.saveAll(assignments);
        audit(actor.getId(), "USER_BRANCH_ASSIGNMENTS_CHANGED", user.getId(), "branchCount=" + assignments.size());
    }

    private Page<UserManagementResponse> mapPage(Page<User> users) {
        return users.map(this::toResponse);
    }

    private UserManagementResponse toResponse(User user) {
        List<UserBranchAssignment> assignments = branchAssignmentRepository.findAllByUserIdWithBranch(user.getId());
        List<UUID> branchIds = assignments.stream().map(a -> a.getBranch().getId()).toList();
        List<String> branchNames = assignments.stream().map(a -> a.getBranch().getName()).toList();
        Restaurant restaurant = user.getRole() == UserRole.RESTAURANT_OWNER
                ? restaurantRepository.findAllByOwner_IdOrderByNameAsc(user.getId()).stream().findFirst().orElse(null)
                : assignments.stream().findFirst().map(a -> a.getBranch().getRestaurant()).orElse(null);
        Courier courier = user.getRole() == UserRole.COURIER
                ? courierRepository.findByUser_Id(user.getId()).orElse(null)
                : null;
        return new UserManagementResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                restaurant != null ? restaurant.getId() : null,
                restaurant != null ? restaurant.getName() : null,
                branchIds,
                branchNames,
                courier != null ? courier.getId() : null,
                user.isMustChangePassword(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private Specification<User> buildSpec(UserPrincipal actor, UserRole role, UserStatus status, String search, UUID restaurantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (role != null) predicates.add(cb.equal(root.get("role"), role));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern)
                ));
            }
            if (actor.getRole() == UserRole.PLATFORM_MANAGER) {
                predicates.add(cb.equal(root.get("role"), UserRole.COURIER));
            } else if (actor.getRole() == UserRole.RESTAURANT_OWNER) {
                predicates.add(cb.equal(root.get("role"), UserRole.RESTAURANT_STAFF));
                Subquery<UUID> sub = query.subquery(UUID.class);
                var uba = sub.from(UserBranchAssignment.class);
                Join<UserBranchAssignment, Branch> branch = uba.join("branch", JoinType.INNER);
                sub.select(uba.get("user").get("id"));
                sub.where(cb.equal(branch.get("restaurant").get("owner").get("id"), actor.getId()));
                predicates.add(root.get("id").in(sub));
            }
            if (restaurantId != null) {
                Subquery<UUID> staffSub = query.subquery(UUID.class);
                var uba = staffSub.from(UserBranchAssignment.class);
                Join<UserBranchAssignment, Branch> branch = uba.join("branch", JoinType.INNER);
                staffSub.select(uba.get("user").get("id"));
                staffSub.where(cb.equal(branch.get("restaurant").get("id"), restaurantId));

                Subquery<UUID> ownerSub = query.subquery(UUID.class);
                var restaurant = ownerSub.from(Restaurant.class);
                ownerSub.select(restaurant.get("owner").get("id"));
                ownerSub.where(cb.equal(restaurant.get("id"), restaurantId));
                predicates.add(cb.or(root.get("id").in(staffSub), root.get("id").in(ownerSub)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validatePassword(String email, String password) {
        if (password.equalsIgnoreCase(email)) {
            throw new BusinessException("Temporary password cannot equal email", "WEAK_TEMPORARY_PASSWORD");
        }
    }

    private void audit(UUID actorId, String action, UUID targetUserId, String details) {
        auditService.record(actorId, action, "USER", targetUserId, details);
    }

    private UserPrincipal currentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
