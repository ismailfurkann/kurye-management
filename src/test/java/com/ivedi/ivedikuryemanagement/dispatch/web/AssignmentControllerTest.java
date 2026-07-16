package com.ivedi.ivedikuryemanagement.dispatch.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.delivery.api.PaymentCollectionType;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentResponse;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentService;
import com.ivedi.ivedikuryemanagement.dispatch.api.AssignmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssignmentControllerTest {

    private final AssignmentService assignmentService = mock(AssignmentService.class);
    private final AssignmentController controller = new AssignmentController(assignmentService);

    @Test
    void listAliases_returnAssignmentResponses() {
        AssignmentResponse assignment = response(AssignmentStatus.ASSIGNED);
        when(assignmentService.getMyActiveAssignmentResponses()).thenReturn(List.of(assignment));

        assertThat(controller.getMyAssignmentsAlias().getBody().getData()).containsExactly(assignment);
        assertThat(controller.getActiveAssignmentsAlias().getBody().getData()).containsExactly(assignment);
        verify(assignmentService, org.mockito.Mockito.times(2)).getMyActiveAssignmentResponses();
    }

    @Test
    void actionAliases_delegateToResponseMethods() {
        UUID id = UUID.randomUUID();
        AssignmentResponse accepted = response(AssignmentStatus.ACCEPTED);
        AssignmentResponse rejected = response(AssignmentStatus.REJECTED);
        AssignmentResponse pickedUp = response(AssignmentStatus.PICKED_UP);
        AssignmentResponse delivered = response(AssignmentStatus.DELIVERED);
        AssignmentResponse failed = response(AssignmentStatus.FAILED);

        when(assignmentService.acceptResponse(id)).thenReturn(accepted);
        when(assignmentService.rejectResponse(id, "No answer")).thenReturn(rejected);
        when(assignmentService.confirmPickupResponse(id)).thenReturn(pickedUp);
        when(assignmentService.confirmDeliveryResponse(id)).thenReturn(delivered);
        when(assignmentService.reportFailureResponse(id, "Address not found")).thenReturn(failed);

        assertThat(controller.acceptAlias(id).getBody().getData()).isEqualTo(accepted);
        assertThat(controller.rejectAlias(id, new RejectionRequest("No answer")).getBody().getData()).isEqualTo(rejected);
        assertThat(controller.pickupAlias(id).getBody().getData()).isEqualTo(pickedUp);
        assertThat(controller.deliverAlias(id).getBody().getData()).isEqualTo(delivered);

        ResponseEntity<ApiResponse<AssignmentResponse>> failResponse =
                controller.failAlias(id, new FailureReportRequest("Address not found"));
        assertThat(failResponse.getBody().getData()).isEqualTo(failed);
    }

    private AssignmentResponse response(AssignmentStatus status) {
        return new AssignmentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORD-1",
                UUID.randomUUID(),
                "Kurye",
                "+905551111111",
                null,
                status,
                OffsetDateTime.now(),
                status == AssignmentStatus.ACCEPTED ? OffsetDateTime.now() : null,
                status == AssignmentStatus.PICKED_UP ? OffsetDateTime.now() : null,
                status == AssignmentStatus.DELIVERED ? OffsetDateTime.now() : null,
                status == AssignmentStatus.FAILED ? OffsetDateTime.now() : null,
                null,
                status == AssignmentStatus.FAILED ? "Address not found" : null,
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
