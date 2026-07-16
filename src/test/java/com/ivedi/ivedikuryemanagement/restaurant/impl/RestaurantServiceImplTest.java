package com.ivedi.ivedikuryemanagement.restaurant.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.auth.impl.UserRepository;
import com.ivedi.ivedikuryemanagement.branch.impl.BranchRepository;
import com.ivedi.ivedikuryemanagement.commission.impl.CommissionRepository;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.delivery.impl.DeliveryOrderRepository;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantMapper;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantResponse;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantSearchDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantStatus;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantOwnerRequest;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantRequest;
import com.ivedi.ivedikuryemanagement.restaurant.web.RestaurantStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceImplTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RestaurantMapper restaurantMapper;
    @Mock private UserRepository userRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private DeliveryOrderRepository deliveryOrderRepository;
    @Mock private CommissionRepository commissionRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private UUID restaurantId;
    private UUID ownerId;
    private User owner;
    private Restaurant sampleRestaurant;
    private RestaurantDto sampleDto;

    @BeforeEach
    void setUp() {
        restaurantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId)
                .fullName("Ahmet Yılmaz")
                .email("ahmet@example.com")
                .role(UserRole.RESTAURANT_OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        sampleRestaurant = Restaurant.builder()
                .id(restaurantId)
                .name("Lezzet Lokantası")
                .owner(owner)
                .status(RestaurantStatus.PENDING)
                .build();

        sampleDto = new RestaurantDto(
                restaurantId, "Lezzet Lokantası", null, null, null, null,
                ownerId, "Ahmet Yılmaz", "ahmet@example.com", RestaurantStatus.PENDING, null, null
        );
    }

    @Nested
    @DisplayName("frontend responses")
    class FrontendResponseTests {

        @Test
        @DisplayName("searchResponses maps owner fields and defaults missing metrics to zero")
        void searchResponses_mapsFrontendContract() {
            sampleRestaurant.setPhone("+905551234567");
            sampleRestaurant.setEmail("info@example.com");
            sampleRestaurant.setLogoUrl(null);
            Page<Restaurant> page = new PageImpl<>(List.of(sampleRestaurant));

            when(restaurantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(branchRepository.countByRestaurantIds(List.of(restaurantId))).thenReturn(List.of());
            when(deliveryOrderRepository.countTodayByRestaurantIds(eq(List.of(restaurantId)), any(), any())).thenReturn(List.of());
            when(commissionRepository.sumNetAmountByRestaurantIds(List.of(restaurantId))).thenReturn(List.of());

            Page<RestaurantResponse> result = restaurantService.searchResponses(
                    new RestaurantSearchDto("lezzet", RestaurantStatus.PENDING, 0, 20)
            );

            RestaurantResponse response = result.getContent().get(0);
            assertThat(response.ownerUserId()).isEqualTo(ownerId);
            assertThat(response.ownerName()).isEqualTo("Ahmet Yılmaz");
            assertThat(response.branchCount()).isZero();
            assertThat(response.activeCourierCount()).isZero();
            assertThat(response.todayOrderCount()).isZero();
            assertThat(response.totalCommission()).isZero();
            assertThat(response.logoUrl()).isNull();
        }

        @Test
        @DisplayName("searchResponses applies aggregate metrics by restaurant id")
        void searchResponses_appliesMetrics() {
            Page<Restaurant> page = new PageImpl<>(List.of(sampleRestaurant));
            when(restaurantRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
            when(branchRepository.countByRestaurantIds(List.of(restaurantId))).thenReturn(List.<Object[]>of(new Object[]{restaurantId, 2L}));
            when(deliveryOrderRepository.countTodayByRestaurantIds(eq(List.of(restaurantId)), any(), any()))
                    .thenReturn(List.<Object[]>of(new Object[]{restaurantId, 12L}));
            when(commissionRepository.sumNetAmountByRestaurantIds(List.of(restaurantId)))
                    .thenReturn(List.<Object[]>of(new Object[]{restaurantId, new java.math.BigDecimal("420.50")}));

            RestaurantResponse response = restaurantService.searchResponses(
                    new RestaurantSearchDto(null, null, 0, 20)
            ).getContent().get(0);

            assertThat(response.branchCount()).isEqualTo(2);
            assertThat(response.todayOrderCount()).isEqualTo(12);
            assertThat(response.totalCommission()).isEqualByComparingTo("420.50");
        }
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private final RestaurantRequest validRequest = new RestaurantRequest(
                "Lezzet Lokantası", null, null, null, null, null
        );

        @Test
        @DisplayName("valid request with RESTAURANT_OWNER → creates PENDING restaurant")
        void create_validRequest_createsRestaurantInPendingStatus() {
            RestaurantRequest req = new RestaurantRequest(
                    "Lezzet Lokantası", null, null, null, null, ownerId
            );
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.save(any(Restaurant.class))).thenReturn(sampleRestaurant);
            when(restaurantMapper.toDto(sampleRestaurant)).thenReturn(sampleDto);

            RestaurantDto result = restaurantService.create(req);

            assertThat(result.status()).isEqualTo(RestaurantStatus.PENDING);
            verify(restaurantRepository).save(any(Restaurant.class));
        }

        @Test
        @DisplayName("owner not found → throws ResourceNotFoundException")
        void create_ownerNotFound_throwsNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            RestaurantRequest req = new RestaurantRequest(
                    "Test", null, null, null, null, unknownId
            );
            when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> restaurantService.create(req))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(restaurantRepository, never()).save(any());
        }

        @Test
        @DisplayName("owner has wrong role (e.g. COURIER) → throws BusinessException")
        void create_ownerWrongRole_throwsBusinessException() {
            User courier = User.builder()
                    .id(ownerId).role(UserRole.COURIER).status(UserStatus.ACTIVE).build();
            RestaurantRequest req = new RestaurantRequest(
                    "Test", null, null, null, null, ownerId
            );
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(courier));

            assertThatThrownBy(() -> restaurantService.create(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RESTAURANT_OWNER");
        }

        @Test
        @DisplayName("duplicate tax number → throws DuplicateResourceException")
        void create_duplicateTaxNumber_throwsDuplicateResourceException() {
            RestaurantRequest req = new RestaurantRequest(
                    "Test", null, "1234567890", null, null, ownerId
            );
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.existsByTaxNumber("1234567890")).thenReturn(true);

            assertThatThrownBy(() -> restaurantService.create(req))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("1234567890");
        }

        @Test
        @DisplayName("null tax number → no uniqueness check performed")
        void create_nullTaxNumber_skipsUniquenessCheck() {
            RestaurantRequest req = new RestaurantRequest(
                    "Test", null, null, null, null, ownerId
            );
            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.save(any())).thenReturn(sampleRestaurant);
            when(restaurantMapper.toDto(any())).thenReturn(sampleDto);

            restaurantService.create(req);

            verify(restaurantRepository, never()).existsByTaxNumber(any());
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("valid update → persists changes")
        void update_validRequest_success() {
            RestaurantRequest req = new RestaurantRequest(
                    "Yeni İsim", "Yeni Legal", null, null, null, ownerId
            );
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.save(sampleRestaurant)).thenReturn(sampleRestaurant);
            when(restaurantMapper.toDto(sampleRestaurant)).thenReturn(sampleDto);

            restaurantService.update(restaurantId, req);

            verify(restaurantRepository).save(sampleRestaurant);
            assertThat(sampleRestaurant.getName()).isEqualTo("Yeni İsim");
        }

        @Test
        @DisplayName("non-existent id → throws ResourceNotFoundException")
        void update_nonExistentId_throwsNotFoundException() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());
            RestaurantRequest req = new RestaurantRequest(
                    "Test", null, null, null, null, ownerId
            );

            assertThatThrownBy(() -> restaurantService.update(restaurantId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("tax number conflict with another restaurant → throws DuplicateResourceException")
        void update_taxNumberConflict_throwsDuplicateResourceException() {
            RestaurantRequest req = new RestaurantRequest(
                    "Test", null, "1234567890", null, null, ownerId
            );
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.existsByTaxNumberAndIdNot("1234567890", restaurantId)).thenReturn(true);

            assertThatThrownBy(() -> restaurantService.update(restaurantId, req))
                    .isInstanceOf(DuplicateResourceException.class);
        }
    }

    @Nested
    @DisplayName("setStatus()")
    class SetStatusTests {

        @Test
        @DisplayName("PENDING → ACTIVE changes status")
        void setStatus_pendingToActive_savesNewStatus() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantRepository.save(sampleRestaurant)).thenReturn(sampleRestaurant);
            when(restaurantMapper.toDto(sampleRestaurant)).thenReturn(sampleDto);

            restaurantService.setStatus(restaurantId, new RestaurantStatusRequest(RestaurantStatus.ACTIVE));

            assertThat(sampleRestaurant.getStatus()).isEqualTo(RestaurantStatus.ACTIVE);
            verify(restaurantRepository).save(sampleRestaurant);
        }

        @Test
        @DisplayName("same status → no-op, no save")
        void setStatus_sameStatus_noOp() {
            sampleRestaurant.setStatus(RestaurantStatus.PENDING);
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
            when(restaurantMapper.toDto(sampleRestaurant)).thenReturn(sampleDto);

            restaurantService.setStatus(restaurantId, new RestaurantStatusRequest(RestaurantStatus.PENDING));

            verify(restaurantRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("changeOwner()")
    class ChangeOwnerTests {

        @Test
        @DisplayName("new owner has RESTAURANT_OWNER role → transfers ownership")
        void changeOwner_validNewOwner_success() {
            UUID newOwnerId = UUID.randomUUID();
            User newOwner = User.builder()
                    .id(newOwnerId).role(UserRole.RESTAURANT_OWNER).status(UserStatus.ACTIVE).build();
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
            when(userRepository.findById(newOwnerId)).thenReturn(Optional.of(newOwner));
            when(restaurantRepository.save(sampleRestaurant)).thenReturn(sampleRestaurant);
            when(restaurantMapper.toDto(sampleRestaurant)).thenReturn(sampleDto);

            restaurantService.changeOwner(restaurantId, new RestaurantOwnerRequest(newOwnerId));

            assertThat(sampleRestaurant.getOwner()).isEqualTo(newOwner);
        }

        @Test
        @DisplayName("new owner has PLATFORM_MANAGER role → throws BusinessException")
        void changeOwner_newOwnerWrongRole_throwsBusinessException() {
            UUID managerId = UUID.randomUUID();
            User manager = User.builder()
                    .id(managerId).role(UserRole.PLATFORM_MANAGER).status(UserStatus.ACTIVE).build();
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(sampleRestaurant));
            when(userRepository.findById(managerId)).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> restaurantService.changeOwner(restaurantId, new RestaurantOwnerRequest(managerId)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("RESTAURANT_OWNER");
        }
    }

    @Nested
    @DisplayName("getMyRestaurants()")
    class GetMyRestaurantsTests {

        @BeforeEach
        void setUpSecurityContext() {
            User currentUser = User.builder()
                    .id(ownerId).role(UserRole.RESTAURANT_OWNER).status(UserStatus.ACTIVE).build();
            UserPrincipal principal = new UserPrincipal(currentUser);
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(principal);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);
        }

        @Test
        @DisplayName("returns only restaurants belonging to current user")
        void getMyRestaurants_returnsOwnRestaurantsOnly() {
            when(restaurantRepository.findAllByOwner_IdOrderByNameAsc(ownerId))
                    .thenReturn(List.of(sampleRestaurant));
            when(restaurantMapper.toDtoList(List.of(sampleRestaurant)))
                    .thenReturn(List.of(sampleDto));

            List<RestaurantDto> result = restaurantService.getMyRestaurants();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).ownerId()).isEqualTo(ownerId);
        }

        @Test
        @DisplayName("owner has no restaurants → returns empty list")
        void getMyRestaurants_noRestaurants_returnsEmptyList() {
            when(restaurantRepository.findAllByOwner_IdOrderByNameAsc(ownerId))
                    .thenReturn(List.of());
            when(restaurantMapper.toDtoList(List.of())).thenReturn(List.of());

            List<RestaurantDto> result = restaurantService.getMyRestaurants();

            assertThat(result).isEmpty();
        }
    }
}
