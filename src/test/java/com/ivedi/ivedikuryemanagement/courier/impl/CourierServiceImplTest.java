package com.ivedi.ivedikuryemanagement.courier.impl;

import com.ivedi.ivedikuryemanagement.auth.api.UserRole;
import com.ivedi.ivedikuryemanagement.auth.api.UserStatus;
import com.ivedi.ivedikuryemanagement.auth.impl.User;
import com.ivedi.ivedikuryemanagement.auth.impl.UserRepository;
import com.ivedi.ivedikuryemanagement.common.exception.BusinessException;
import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.common.security.UserPrincipal;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.CourierDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierMapper;
import com.ivedi.ivedikuryemanagement.courier.api.CourierResponse;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.courier.web.CourierApprovalRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierAvailabilityRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierLocationRequest;
import com.ivedi.ivedikuryemanagement.courier.web.CourierRegistrationRequest;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentMapper;
import com.ivedi.ivedikuryemanagement.dispatch.impl.DeliveryAssignmentRepository;
import com.ivedi.ivedikuryemanagement.realtime.api.RealtimePublisher;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import com.ivedi.ivedikuryemanagement.region.impl.Region;
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
class CourierServiceImplTest {

    @Mock private CourierRepository courierRepository;
    @Mock private CourierMapper courierMapper;
    @Mock private UserRepository userRepository;
    @Mock private RealtimePublisher realtimePublisher;
    @Mock private DeliveryAssignmentRepository assignmentRepository;
    @Mock private DeliveryAssignmentMapper assignmentMapper;

    @InjectMocks
    private CourierServiceImpl courierService;

    private UUID courierId;
    private UUID userId;
    private User courierUser;
    private Courier sampleCourier;
    private CourierDto sampleDto;

    @BeforeEach
    void setUp() {
        courierId = UUID.randomUUID();
        userId    = UUID.randomUUID();

        courierUser = User.builder()
                .id(userId).fullName("Ali Kaya").email("ali@test.com")
                .role(UserRole.COURIER).status(UserStatus.ACTIVE).build();

        sampleCourier = Courier.builder()
                .id(courierId).user(courierUser)
                .vehicleType(VehicleType.MOTORBIKE)
                .availabilityStatus(AvailabilityStatus.OFFLINE)
                .approvalStatus(ApprovalStatus.PENDING)
                .activeAssignmentCount(0)
                .rating(BigDecimal.ZERO)
                .build();

        sampleDto = new CourierDto(courierId, userId, "Ali Kaya", "ali@test.com", null,
                VehicleType.MOTORBIKE, AvailabilityStatus.OFFLINE, ApprovalStatus.PENDING,
                null, null, null, BigDecimal.ZERO, 0, null, null);
    }

    void setUpSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext ctx = mock(SecurityContext.class);
        lenient().when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @Nested
    @DisplayName("frontend responses")
    class FrontendResponseTests {

        @Test
        @DisplayName("getResponseById maps frontend fields and aggregate metrics")
        void getResponseById_mapsFrontendFieldsAndMetrics() {
            UUID regionId = UUID.randomUUID();
            Region region = Region.builder()
                    .id(regionId)
                    .name("Merkez")
                    .city("Ankara")
                    .district("Cankaya")
                    .status(RegionStatus.ACTIVE)
                    .build();
            sampleCourier.setRegion(region);
            sampleCourier.setPlateNumber("06 ABC 123");
            sampleCourier.setAvatarUrl("https://example.com/avatar.png");
            sampleCourier.setCurrentLatitude(new BigDecimal("39.9208000"));
            sampleCourier.setCurrentLongitude(new BigDecimal("32.8541000"));

            when(courierRepository.findByIdWithUser(courierId)).thenReturn(Optional.of(sampleCourier));
            when(assignmentRepository.countByCourierIdsAndStatusIn(
                    List.of(courierId),
                    List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.ACCEPTED, AssignmentStatus.PICKED_UP)
            )).thenReturn(List.<Object[]>of(new Object[]{courierId, 2L}));
            when(assignmentRepository.countByCourierIdsAndStatusIn(
                    List.of(courierId),
                    List.of(AssignmentStatus.DELIVERED)
            )).thenReturn(List.<Object[]>of(new Object[]{courierId, 9L}));

            CourierResponse response = courierService.getResponseById(courierId);

            assertThat(response.fullName()).isEqualTo("Ali Kaya");
            assertThat(response.email()).isEqualTo("ali@test.com");
            assertThat(response.phone()).isNull();
            assertThat(response.availability()).isEqualTo(AvailabilityStatus.OFFLINE);
            assertThat(response.lastLocationLat()).isEqualByComparingTo("39.9208000");
            assertThat(response.lastLocationLng()).isEqualByComparingTo("32.8541000");
            assertThat(response.regionId()).isEqualTo(regionId);
            assertThat(response.regionName()).isEqualTo("Merkez");
            assertThat(response.plateNumber()).isEqualTo("06 ABC 123");
            assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
            assertThat(response.activeAssignmentCount()).isEqualTo(2);
            assertThat(response.totalDeliveries()).isEqualTo(9);
        }

        @Test
        @DisplayName("getResponseById safely handles null region and missing metrics")
        void getResponseById_nullRegionAndMissingMetrics_safeDefaults() {
            when(courierRepository.findByIdWithUser(courierId)).thenReturn(Optional.of(sampleCourier));
            when(assignmentRepository.countByCourierIdsAndStatusIn(any(), any())).thenReturn(List.of());

            CourierResponse response = courierService.getResponseById(courierId);

            assertThat(response.regionId()).isNull();
            assertThat(response.regionName()).isNull();
            assertThat(response.activeAssignmentCount()).isZero();
            assertThat(response.totalDeliveries()).isZero();
        }

        @Test
        @DisplayName("approve sets approvedAt")
        void setApprovalStatus_approved_setsApprovedAt() {
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.setApprovalStatus(courierId, new CourierApprovalRequest(ApprovalStatus.APPROVED));

            assertThat(sampleCourier.getApprovedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("createProfile()")
    class CreateProfileTests {

        @BeforeEach
        void setup() { setUpSecurityContext(courierUser); }

        @Test
        @DisplayName("COURIER user with no existing profile → creates PENDING profile")
        void createProfile_validCourierUser_createsProfile() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(courierUser));
            when(courierRepository.existsByUser_Id(userId)).thenReturn(false);
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            CourierDto result = courierService.createProfile(new CourierRegistrationRequest(VehicleType.MOTORBIKE));

            assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
            assertThat(result.availabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
            verify(courierRepository).save(any(Courier.class));
        }

        @Test
        @DisplayName("profile already exists → throws DuplicateResourceException")
        void createProfile_alreadyExists_throwsDuplicate() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(courierUser));
            when(courierRepository.existsByUser_Id(userId)).thenReturn(true);

            assertThatThrownBy(() -> courierService.createProfile(new CourierRegistrationRequest(VehicleType.BICYCLE)))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("non-COURIER user tries to create profile → throws BusinessException")
        void createProfile_wrongRole_throwsBusinessException() {
            User manager = User.builder().id(userId).role(UserRole.PLATFORM_MANAGER).build();
            setUpSecurityContext(manager);
            when(userRepository.findById(userId)).thenReturn(Optional.of(manager));

            assertThatThrownBy(() -> courierService.createProfile(new CourierRegistrationRequest(VehicleType.CAR)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("COURIER");
        }
    }

    @Nested
    @DisplayName("updateAvailability()")
    class UpdateAvailabilityTests {

        @BeforeEach
        void setup() { setUpSecurityContext(courierUser); }

        @Test
        @DisplayName("APPROVED courier sets AVAILABLE → persists change")
        void updateAvailability_approvedCourier_success() {
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            when(courierRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.updateAvailability(new CourierAvailabilityRequest(AvailabilityStatus.AVAILABLE));

            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        }

        @Test
        @DisplayName("PENDING courier tries to go online → throws BusinessException")
        void updateAvailability_pendingCourier_throwsBusinessException() {
            sampleCourier.setApprovalStatus(ApprovalStatus.PENDING);
            when(courierRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.updateAvailability(
                    new CourierAvailabilityRequest(AvailabilityStatus.AVAILABLE)))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo("COURIER_NOT_APPROVED"));
        }

        @Test
        @DisplayName("courier tries to manually set BUSY → throws BusinessException")
        void updateAvailability_setBusy_throwsBusinessException() {
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            when(courierRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.updateAvailability(
                    new CourierAvailabilityRequest(AvailabilityStatus.BUSY)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("system");
        }

        @Test
        @DisplayName("BLOCKED courier tries to go online → throws BusinessException")
        void updateAvailability_blockedCourier_throwsBusinessException() {
            sampleCourier.setApprovalStatus(ApprovalStatus.BLOCKED);
            when(courierRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(sampleCourier));

            assertThatThrownBy(() -> courierService.updateAvailability(
                    new CourierAvailabilityRequest(AvailabilityStatus.AVAILABLE)))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("setApprovalStatus()")
    class SetApprovalStatusTests {

        @Test
        @DisplayName("PENDING → APPROVED preserves current availability")
        void setApprovalStatus_pendingToApproved_preservesAvailability() {
            sampleCourier.setApprovalStatus(ApprovalStatus.PENDING);
            sampleCourier.setAvailabilityStatus(AvailabilityStatus.OFFLINE);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.setApprovalStatus(courierId, new CourierApprovalRequest(ApprovalStatus.APPROVED));

            assertThat(sampleCourier.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        }

        @Test
        @DisplayName("APPROVED → BLOCKED forces courier OFFLINE")
        void setApprovalStatus_blocked_forcesOffline() {
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            sampleCourier.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.setApprovalStatus(courierId, new CourierApprovalRequest(ApprovalStatus.BLOCKED));

            assertThat(sampleCourier.getApprovalStatus()).isEqualTo(ApprovalStatus.BLOCKED);
            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        }

        @Test
        @DisplayName("APPROVED → REJECTED forces courier OFFLINE")
        void setApprovalStatus_rejected_forcesOffline() {
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            sampleCourier.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.setApprovalStatus(courierId, new CourierApprovalRequest(ApprovalStatus.REJECTED));

            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.OFFLINE);
        }

        @Test
        @DisplayName("same approval status → no-op, does not call save")
        void setApprovalStatus_sameStatus_noOp() {
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.setApprovalStatus(courierId, new CourierApprovalRequest(ApprovalStatus.APPROVED));

            verify(courierRepository, never()).save(any());
        }

        @Test
        @DisplayName("non-existent courier id → throws ResourceNotFoundException")
        void setApprovalStatus_nonExistentId_throwsNotFoundException() {
            when(courierRepository.findById(courierId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> courierService.setApprovalStatus(
                    courierId, new CourierApprovalRequest(ApprovalStatus.APPROVED)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("incrementActiveCount() / decrementActiveCount()")
    class ActiveCountTests {

        @Test
        @DisplayName("increment → increases count and sets BUSY")
        void incrementActiveCount_increaseCountAndSetsBusy() {
            sampleCourier.setActiveAssignmentCount(0);
            sampleCourier.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);

            courierService.incrementActiveCount(courierId);

            assertThat(sampleCourier.getActiveAssignmentCount()).isEqualTo(1);
            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BUSY);
        }

        @Test
        @DisplayName("decrement from 1 → count=0 and APPROVED courier goes AVAILABLE")
        void decrementActiveCount_lastDelivery_setsAvailableIfApproved() {
            sampleCourier.setActiveAssignmentCount(1);
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            sampleCourier.setAvailabilityStatus(AvailabilityStatus.BUSY);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);

            courierService.decrementActiveCount(courierId);

            assertThat(sampleCourier.getActiveAssignmentCount()).isZero();
            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.AVAILABLE);
        }

        @Test
        @DisplayName("decrement below 0 → count stays at 0 (floor guard)")
        void decrementActiveCount_belowZero_staysAtZero() {
            sampleCourier.setActiveAssignmentCount(0);
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);

            courierService.decrementActiveCount(courierId);

            assertThat(sampleCourier.getActiveAssignmentCount()).isZero();
        }

        @Test
        @DisplayName("decrement from 2 → count=1, stays BUSY (more active deliveries)")
        void decrementActiveCount_stillHasActiveDeliveries_staysBusy() {
            sampleCourier.setActiveAssignmentCount(2);
            sampleCourier.setApprovalStatus(ApprovalStatus.APPROVED);
            sampleCourier.setAvailabilityStatus(AvailabilityStatus.BUSY);
            when(courierRepository.findById(courierId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(sampleCourier)).thenReturn(sampleCourier);

            courierService.decrementActiveCount(courierId);

            assertThat(sampleCourier.getActiveAssignmentCount()).isEqualTo(1);
            assertThat(sampleCourier.getAvailabilityStatus()).isEqualTo(AvailabilityStatus.BUSY);
        }
    }

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocationTests {

        @BeforeEach
        void setup() { setUpSecurityContext(courierUser); }

        @Test
        @DisplayName("valid coordinates → updates lat/lon and lastLocationAt")
        void updateLocation_validCoordinates_updatesFields() {
            BigDecimal lat = new BigDecimal("37.8716700");
            BigDecimal lon = new BigDecimal("32.4843200");
            when(courierRepository.findByUserIdWithUser(userId)).thenReturn(Optional.of(sampleCourier));
            when(courierRepository.save(any(Courier.class))).thenReturn(sampleCourier);
            when(courierMapper.toDto(sampleCourier)).thenReturn(sampleDto);

            courierService.updateLocation(new CourierLocationRequest(lat, lon));

            assertThat(sampleCourier.getCurrentLatitude()).isEqualByComparingTo(lat);
            assertThat(sampleCourier.getCurrentLongitude()).isEqualByComparingTo(lon);
            assertThat(sampleCourier.getLastLocationAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getAvailableForDispatch()")
    class DispatchTests {

        @Test
        @DisplayName("returns APPROVED+AVAILABLE couriers sorted by rating")
        void getAvailableForDispatch_returnsSortedList() {
            when(courierRepository.findAvailableForDispatch(
                    ApprovalStatus.APPROVED, AvailabilityStatus.AVAILABLE))
                    .thenReturn(List.of(sampleCourier));
            when(courierMapper.toDtoList(List.of(sampleCourier))).thenReturn(List.of(sampleDto));

            List<CourierDto> result = courierService.getAvailableForDispatch();

            assertThat(result).hasSize(1);
            verify(courierRepository).findAvailableForDispatch(ApprovalStatus.APPROVED, AvailabilityStatus.AVAILABLE);
        }
    }
}
