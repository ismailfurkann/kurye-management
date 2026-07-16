package com.ivedi.ivedikuryemanagement.dispatch.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import com.ivedi.ivedikuryemanagement.dispatch.api.DispatchService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchControllerTest {

    private final DispatchService dispatchService = mock(DispatchService.class);
    private final DispatchController controller = new DispatchController(dispatchService);

    @Test
    void assignAlias_returnsAssignmentResponse() {
        UUID orderId = UUID.randomUUID();
        UUID courierId = UUID.randomUUID();
        AssignmentResponse assignment = response(orderId, courierId);
        when(dispatchService.assignResponse(orderId, courierId)).thenReturn(assignment);

        ResponseEntity<ApiResponse<AssignmentResponse>> response =
                controller.assignAlias(orderId, courierId);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(assignment);
        verify(dispatchService).assignResponse(orderId, courierId);
    }

    @Test
    void reassignAlias_usesAssignmentIdAndCourierIdBody() {
        UUID assignmentId = UUID.randomUUID();
        UUID courierId = UUID.randomUUID();
        AssignmentReassignRequest request = new AssignmentReassignRequest(courierId, "Unavailable");
        AssignmentResponse assignment = response(UUID.randomUUID(), courierId);
        when(dispatchService.reassignByAssignmentIdResponse(assignmentId, courierId, "Unavailable"))
                .thenReturn(assignment);

        ResponseEntity<ApiResponse<AssignmentResponse>> response =
                controller.reassignAlias(assignmentId, request);

        assertThat(response.getBody().getData()).isEqualTo(assignment);
        verify(dispatchService).reassignByAssignmentIdResponse(assignmentId, courierId, "Unavailable");
    }

    private AssignmentResponse response(UUID orderId, UUID courierId) {
        return new AssignmentResponse(
                UUID.randomUUID(),
                orderId,
                "ORD-1",
                courierId,
                "Kurye",
                "+905551111111",
                null,
                AssignmentStatus.ASSIGNED,
                OffsetDateTime.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                "Restoran",
                "Merkez",
                "Adres",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                PaymentCollectionType.CASH_ON_DELIVERY,
                "Not",
                "Ali",
                "+905551234567"
        );
    }
}
