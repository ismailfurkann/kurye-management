package com.ivedi.ivedikuryemanagement.branch.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.branch.api.BranchDto;
import com.ivedi.ivedikuryemanagement.branch.api.BranchMapper;
import com.ivedi.ivedikuryemanagement.branch.api.BranchStatus;
import com.ivedi.ivedikuryemanagement.branch.web.BranchRequest;
import com.ivedi.ivedikuryemanagement.branch.web.BranchStatusRequest;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import com.ivedi.ivedikuryemanagement.region.impl.Region;
import com.ivedi.ivedikuryemanagement.region.impl.RegionRepository;
import com.ivedi.ivedikuryemanagement.restaurant.impl.Restaurant;
import com.ivedi.ivedikuryemanagement.restaurant.impl.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock private BranchRepository branchRepository;
    @Mock private BranchMapper branchMapper;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RegionRepository regionRepository;

    @InjectMocks
    private BranchServiceImpl branchService;

    private UUID branchId;
    private UUID restaurantId;
    private UUID regionId;
    private UUID ownerId;

    private User owner;
    private Restaurant restaurant;
    private Region activeRegion;
    private Branch sampleBranch;
    private BranchDto sampleDto;

    @BeforeEach
    void setUp() {
        branchId     = UUID.randomUUID();
        restaurantId = UUID.randomUUID();
        regionId     = UUID.randomUUID();
        ownerId      = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId).fullName("Mehmet").email("mehmet@test.com")
                .role(UserRole.RESTAURANT_OWNER).status(UserStatus.ACTIVE).build();

        restaurant = Restaurant.builder()
                .id(restaurantId).name("Test Restoran").owner(owner).build();

        activeRegion = Region.builder()
                .id(regionId).name("Selçuklu").city("Konya").district("Selçuklu")
                .status(RegionStatus.ACTIVE).build();

        sampleBranch = Branch.builder()
                .id(branchId).name("Merkez Şube")
                .restaurant(restaurant).region(activeRegion)
                .status(BranchStatus.ACTIVE).build();

        sampleDto = new BranchDto(branchId, restaurantId, "Test Restoran",
                regionId, "Selçuklu", "Konya", "Selçuklu",
                "Merkez Şube", null, null, null, null,
                BranchStatus.ACTIVE, null, null);
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private BranchRequest validRequest() {
            return new BranchRequest(restaurantId, regionId, "Merkez Şube",
                    null, null, null, null);
        }

        @Test
        @DisplayName("valid request → creates ACTIVE branch")
        void create_validRequest_createsActiveBranch() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(activeRegion));
            when(branchRepository.existsByNameAndRestaurant_Id("Merkez Şube", restaurantId)).thenReturn(false);
            when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);
            when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

            BranchDto result = branchService.create(validRequest());

            assertThat(result.status()).isEqualTo(BranchStatus.ACTIVE);
            verify(branchRepository).save(any(Branch.class));
        }

        @Test
        @DisplayName("restaurant not found → throws ResourceNotFoundException")
        void create_restaurantNotFound_throwsNotFoundException() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.create(validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(restaurantId.toString());
        }

        @Test
        @DisplayName("region not found → throws ResourceNotFoundException")
        void create_regionNotFound_throwsNotFoundException() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(regionRepository.findById(regionId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.create(validRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(regionId.toString());
        }

        @Test
        @DisplayName("INACTIVE region → throws BusinessException with REGION_NOT_ACTIVE code")
        void create_inactiveRegion_throwsBusinessException() {
            Region inactiveRegion = Region.builder()
                    .id(regionId).name("Kapalı Bölge").status(RegionStatus.INACTIVE).build();
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(inactiveRegion));

            assertThatThrownBy(() -> branchService.create(validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("not active");
        }

        @Test
        @DisplayName("duplicate branch name in same restaurant → throws DuplicateResourceException")
        void create_duplicateName_throwsDuplicateResourceException() {
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(activeRegion));
            when(branchRepository.existsByNameAndRestaurant_Id("Merkez Şube", restaurantId)).thenReturn(true);

            assertThatThrownBy(() -> branchService.create(validRequest()))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Merkez Şube");
        }

        @Test
        @DisplayName("trims whitespace from branch name before persistence and uniqueness check")
        void create_trimsName_beforeSave() {
            BranchRequest req = new BranchRequest(restaurantId, regionId, "  Merkez Şube  ",
                    null, null, null, null);
            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(activeRegion));
            when(branchRepository.existsByNameAndRestaurant_Id("Merkez Şube", restaurantId)).thenReturn(false);
            when(branchRepository.save(any(Branch.class))).thenReturn(sampleBranch);
            when(branchMapper.toDto(any())).thenReturn(sampleDto);

            branchService.create(req);

            verify(branchRepository).existsByNameAndRestaurant_Id("Merkez Şube", restaurantId);
        }

        @Test
        @DisplayName("stores lat/lon as BigDecimal without precision loss")
        void create_withCoordinates_storesCorrectly() {
            BigDecimal lat = new BigDecimal("37.8716700");
            BigDecimal lon = new BigDecimal("32.4843200");
            BranchRequest req = new BranchRequest(restaurantId, regionId, "Konum Şube",
                    null, "Test Adres", lat, lon);

            when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(activeRegion));
            when(branchRepository.existsByNameAndRestaurant_Id(any(), any())).thenReturn(false);
            when(branchRepository.save(any(Branch.class))).thenAnswer(inv -> {
                Branch b = inv.getArgument(0);
                assertThat(b.getLatitude()).isEqualByComparingTo(lat);
                assertThat(b.getLongitude()).isEqualByComparingTo(lon);
                return sampleBranch;
            });
            when(branchMapper.toDto(any())).thenReturn(sampleDto);

            branchService.create(req);
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        @Test
        @DisplayName("valid update → saves without changing restaurant")
        void update_validRequest_success() {
            BranchRequest req = new BranchRequest(UUID.randomUUID(), regionId,
                    "Yeni Şube Adı", null, "Yeni Adres", null, null);
            when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(activeRegion));
            when(branchRepository.existsByNameAndRestaurant_IdAndIdNot(
                    "Yeni Şube Adı", restaurantId, branchId)).thenReturn(false);
            when(branchRepository.save(sampleBranch)).thenReturn(sampleBranch);
            when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

            branchService.update(branchId, req);

            // Restaurant must remain unchanged regardless of restaurantId in request
            assertThat(sampleBranch.getRestaurant().getId()).isEqualTo(restaurantId);
            assertThat(sampleBranch.getName()).isEqualTo("Yeni Şube Adı");
        }

        @Test
        @DisplayName("changing to an inactive region → throws BusinessException")
        void update_inactiveRegion_throwsBusinessException() {
            UUID otherRegionId = UUID.randomUUID();
            Region inactive = Region.builder().id(otherRegionId).status(RegionStatus.INACTIVE).build();
            BranchRequest req = new BranchRequest(restaurantId, otherRegionId, "Ad", null, null, null, null);
            when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
            when(regionRepository.findById(otherRegionId)).thenReturn(Optional.of(inactive));

            assertThatThrownBy(() -> branchService.update(branchId, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("non-existent branch → throws ResourceNotFoundException")
        void update_nonExistentBranch_throwsNotFoundException() {
            when(branchRepository.findById(branchId)).thenReturn(Optional.empty());
            BranchRequest req = new BranchRequest(restaurantId, regionId, "Ad", null, null, null, null);

            assertThatThrownBy(() -> branchService.update(branchId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("setStatus()")
    class SetStatusTests {

        @Test
        @DisplayName("ACTIVE → INACTIVE changes status")
        void setStatus_activeToInactive_changesStatus() {
            when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
            when(branchRepository.save(sampleBranch)).thenReturn(sampleBranch);
            when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

            branchService.setStatus(branchId, new BranchStatusRequest(BranchStatus.INACTIVE));

            assertThat(sampleBranch.getStatus()).isEqualTo(BranchStatus.INACTIVE);
        }

        @Test
        @DisplayName("same status → no-op, does not call save")
        void setStatus_sameStatus_noOp() {
            sampleBranch.setStatus(BranchStatus.ACTIVE);
            when(branchRepository.findById(branchId)).thenReturn(Optional.of(sampleBranch));
            when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

            branchService.setStatus(branchId, new BranchStatusRequest(BranchStatus.ACTIVE));

            verify(branchRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMyBranches() — owner data isolation")
    class OwnerDataIsolationTests {

        @BeforeEach
        void setUpSecurityContext() {
            UserPrincipal principal = new UserPrincipal(owner);
            Authentication auth = mock(Authentication.class);
            when(auth.getPrincipal()).thenReturn(principal);
            SecurityContext ctx = mock(SecurityContext.class);
            when(ctx.getAuthentication()).thenReturn(auth);
            SecurityContextHolder.setContext(ctx);
        }

        @Test
        @DisplayName("owner requests own restaurant branches → returns branch list")
        void getMyBranches_ownRestaurant_returnsBranches() {
            when(restaurantRepository.findByIdAndOwner_Id(restaurantId, ownerId))
                    .thenReturn(Optional.of(restaurant));
            when(branchRepository.findAllByRestaurant_IdOrderByNameAsc(restaurantId))
                    .thenReturn(List.of(sampleBranch));
            when(branchMapper.toDtoList(List.of(sampleBranch))).thenReturn(List.of(sampleDto));

            List<BranchDto> result = branchService.getMyBranches(restaurantId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("owner requests another restaurant's branches → throws ResourceNotFoundException")
        void getMyBranches_otherRestaurant_throwsNotFoundException() {
            UUID otherRestaurantId = UUID.randomUUID();
            when(restaurantRepository.findByIdAndOwner_Id(otherRestaurantId, ownerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> branchService.getMyBranches(otherRestaurantId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("owner requests specific branch of own restaurant → returns branch")
        void getMyBranchById_ownBranch_returnsDto() {
            when(restaurantRepository.findByIdAndOwner_Id(restaurantId, ownerId))
                    .thenReturn(Optional.of(restaurant));
            when(branchRepository.findByIdAndRestaurant_Id(branchId, restaurantId))
                    .thenReturn(Optional.of(sampleBranch));
            when(branchMapper.toDto(sampleBranch)).thenReturn(sampleDto);

            BranchDto result = branchService.getMyBranchById(restaurantId, branchId);

            assertThat(result.id()).isEqualTo(branchId);
        }
    }

    @Test
    @DisplayName("RESTAURANT_STAFF /branches/me returns safe empty list while staff-branch scope is not modelled")
    void getMyBranchResponses_staff_returnsEmptyList() {
        User staff = User.builder()
                .id(UUID.randomUUID())
                .fullName("Staff")
                .email("staff@test.com")
                .role(UserRole.RESTAURANT_STAFF)
                .status(UserStatus.ACTIVE)
                .build();
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UserPrincipal(staff));
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        assertThat(branchService.getMyBranchResponses()).isEmpty();
        verifyNoInteractions(branchRepository);
    }
}
