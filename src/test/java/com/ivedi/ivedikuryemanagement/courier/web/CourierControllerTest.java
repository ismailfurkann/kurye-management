package com.ivedi.ivedikuryemanagement.courier.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.courier.api.ApprovalStatus;
import com.ivedi.ivedikuryemanagement.courier.api.AvailabilityStatus;
import com.ivedi.ivedikuryemanagement.courier.api.CourierDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierResponse;
import com.ivedi.ivedikuryemanagement.courier.api.CourierSearchDto;
import com.ivedi.ivedikuryemanagement.courier.api.CourierService;
import com.ivedi.ivedikuryemanagement.courier.api.VehicleType;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DeliveryAssignmentDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourierControllerTest {

    @Mock private CourierService courierService;

    @Test
    void searchAlias_returnsPageResponseAndPassesFilters() {
        CourierController controller = new CourierController(courierService);
        UUID regionId = UUID.randomUUID();
        CourierResponse courier = response();
        when(courierService.searchResponses(new CourierSearchDto(
                AvailabilityStatus.AVAILABLE,
                ApprovalStatus.APPROVED,
                VehicleType.MOTORBIKE,
                regionId,
                "ali",
                0,
                20
        ))).thenReturn(new PageImpl<>(List.of(courier)));

        ResponseEntity<ApiResponse<PageResponse<CourierResponse>>> response = controller.searchAlias(
                AvailabilityStatus.AVAILABLE,
                ApprovalStatus.APPROVED,
                VehicleType.MOTORBIKE,
                regionId,
                "ali",
                0,
                20
        );

        assertThat(response.getBody().getData().content()).containsExactly(courier);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);
        assertThat(response.getBody().getData().first()).isTrue();
    }

    @Test
    void selfAliases_delegateToService() {
        CourierController controller = new CourierController(courierService);
        CourierResponse courier = response();
        CourierAvailabilityRequest availability = new CourierAvailabilityRequest(AvailabilityStatus.AVAILABLE);
        CourierLocationRequest location = new CourierLocationRequest(
                new BigDecimal("39.9208000"),
                new BigDecimal("32.8541000")
        );
        when(courierService.getMyProfileResponse()).thenReturn(courier);
        when(courierService.updateAvailabilityResponse(availability)).thenReturn(courier);
        when(courierService.updateLocationResponse(location)).thenReturn(courier);

        assertThat(controller.getMeAlias().getBody().getData()).isEqualTo(courier);
        assertThat(controller.updateAvailabilityAlias(availability).getBody().getData()).isEqualTo(courier);
        assertThat(controller.updateLocationAlias(location).getBody().getData()).isEqualTo(courier);
    }

    @Test
    void approvalAliases_createExpectedRequests() {
        CourierController controller = new CourierController(courierService);
        UUID id = UUID.randomUUID();
        CourierResponse courier = response();
        when(courierService.setApprovalStatusResponse(org.mockito.Mockito.eq(id), org.mockito.Mockito.any()))
                .thenReturn(courier);

        controller.approveAlias(id);
        controller.rejectAlias(id);
        controller.blockAlias(id);

        ArgumentCaptor<CourierApprovalRequest> captor = ArgumentCaptor.forClass(CourierApprovalRequest.class);
        verify(courierService, org.mockito.Mockito.times(3)).setApprovalStatusResponse(org.mockito.Mockito.eq(id), captor.capture());
        assertThat(captor.getAllValues()).extracting(CourierApprovalRequest::status)
                .containsExactly(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED, ApprovalStatus.BLOCKED);
    }

    @Test
    void availableAliasAndOldEndpointContinueToWork() {
        CourierController controller = new CourierController(courierService);
        CourierResponse courier = response();
        CourierDto dto = new CourierDto(
                courier.id(),
                courier.userId(),
                courier.fullName(),
                courier.email(),
                courier.phone(),
                courier.vehicleType(),
                courier.availability(),
                courier.approvalStatus(),
                courier.lastLocationLat(),
                courier.lastLocationLng(),
                courier.lastLocationAt(),
                courier.rating(),
                0,
                courier.createdAt(),
                courier.createdAt()
        );
        when(courierService.getAvailableResponsesForDispatch()).thenReturn(List.of(courier));
        when(courierService.getAvailableForDispatch()).thenReturn(List.of(dto));

        assertThat(controller.getAvailableForDispatchAlias().getBody().getData()).containsExactly(courier);
        assertThat(controller.getAvailableForDispatch().getBody().getData()).containsExactly(dto);
    }

    @Test
    void assignmentsAlias_returnsPageResponse() {
        CourierController controller = new CourierController(courierService);
        UUID courierId = UUID.randomUUID();
        DeliveryAssignmentDto assignment = new DeliveryAssignmentDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORD-1",
                courierId,
                "Ali Kaya",
                AssignmentStatus.ASSIGNED,
                UUID.randomUUID(),
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Page<DeliveryAssignmentDto> page = new PageImpl<>(List.of(assignment));
        when(courierService.getAssignments(courierId, AssignmentStatus.ASSIGNED, 0, 20)).thenReturn(page);

        ResponseEntity<ApiResponse<PageResponse<DeliveryAssignmentDto>>> response =
                controller.getAssignmentsAlias(courierId, AssignmentStatus.ASSIGNED, 0, 20);

        assertThat(response.getBody().getData().content()).containsExactly(assignment);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);
    }

    private CourierResponse response() {
        UUID courierId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        return new CourierResponse(
                courierId,
                userId,
                "Ali Kaya",
                "ali@test.com",
                "+905551234567",
                VehicleType.MOTORBIKE,
                "06 ABC 123",
                AvailabilityStatus.AVAILABLE,
                ApprovalStatus.APPROVED,
                BigDecimal.ZERO,
                0,
                0,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
