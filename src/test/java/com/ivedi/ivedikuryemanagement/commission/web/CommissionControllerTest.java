package com.ivedi.ivedikuryemanagement.commission.web;

import com.ivedi.ivedikuryemanagement.commission.api.CommissionDto;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionResponse;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionService;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionSummaryResponse;
import com.ivedi.ivedikuryemanagement.commission.api.CommissionStatus;
import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommissionControllerTest {

    private final CommissionService commissionService = mock(CommissionService.class);
    private final CommissionController controller = new CommissionController(commissionService);

    @Test
    void myCommissions_returnsPageResponse() {
        CommissionDto item = commission();
        when(commissionService.getMyCommissions(0, 20)).thenReturn(new PageImpl<>(List.of(item)));

        ResponseEntity<ApiResponse<PageResponse<CommissionDto>>> response =
                controller.getMyCommissions(0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).containsExactly(item);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);
        verify(commissionService).getMyCommissions(0, 20);
    }

    @Test
    void adminSearch_returnsPageResponseAndPassesFilters() {
        UUID courierId = UUID.randomUUID();
        CommissionDto item = commission();
        when(commissionService.search(courierId, CommissionStatus.PENDING, "2026-07", 1, 10))
                .thenReturn(new PageImpl<>(List.of(item)));

        ResponseEntity<ApiResponse<PageResponse<CommissionDto>>> response =
                controller.search(courierId, CommissionStatus.PENDING, "2026-07", 1, 10);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).containsExactly(item);
        assertThat(response.getBody().getData().number()).isZero();
        verify(commissionService).search(courierId, CommissionStatus.PENDING, "2026-07", 1, 10);
    }

    @Test
    void frontendAdminSearchAlias_returnsCommissionResponsePageAndPassesFilters() {
        UUID courierId = UUID.randomUUID();
        CommissionResponse item = commissionResponse();
        when(commissionService.searchResponses(courierId, CommissionStatus.PENDING, "2026-07", 0, 20))
                .thenReturn(new PageImpl<>(List.of(item)));

        ResponseEntity<ApiResponse<PageResponse<CommissionResponse>>> response =
                controller.searchAlias(courierId, CommissionStatus.PENDING, "2026-07", 0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).containsExactly(item);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);
        verify(commissionService).searchResponses(courierId, CommissionStatus.PENDING, "2026-07", 0, 20);
    }

    @Test
    void frontendCourierAliases_returnOwnPageAndSummary() {
        CommissionResponse item = commissionResponse();
        CommissionSummaryResponse summary = new CommissionSummaryResponse(
                2, new BigDecimal("10.00"), new BigDecimal("20.00"), new BigDecimal("30.00"), new BigDecimal("30.00"));
        when(commissionService.getMyCommissionResponses(0, 20)).thenReturn(new PageImpl<>(List.of(item)));
        when(commissionService.getMyFrontendSummary()).thenReturn(summary);

        assertThat(controller.getMyCommissionsAlias(0, 20).getBody().getData().content()).containsExactly(item);
        assertThat(controller.getMySummaryAlias().getBody().getData()).isEqualTo(summary);
    }

    @Test
    void frontendGetAndPayAliases_delegateById() {
        UUID id = UUID.randomUUID();
        CommissionResponse item = commissionResponse(id);
        when(commissionService.getResponseById(id)).thenReturn(item);
        when(commissionService.markOneAsPaid(id)).thenReturn(item);

        assertThat(controller.getByIdAlias(id).getBody().getData()).isEqualTo(item);
        assertThat(controller.payOneAlias(id).getBody().getData()).isEqualTo(item);
    }

    private CommissionDto commission() {
        return new CommissionDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ORD-1",
                UUID.randomUUID(),
                "Ali Kaya",
                new BigDecimal("10.00"),
                BigDecimal.ONE,
                new BigDecimal("10.00"),
                CommissionStatus.PENDING,
                "2026-07",
                null,
                OffsetDateTime.now()
        );
    }

    private CommissionResponse commissionResponse() {
        return commissionResponse(UUID.randomUUID());
    }

    private CommissionResponse commissionResponse(UUID id) {
        return new CommissionResponse(
                id,
                UUID.randomUUID(),
                "Ali Kaya",
                UUID.randomUUID(),
                "ORD-1",
                new BigDecimal("10.00"),
                BigDecimal.ONE,
                new BigDecimal("10.00"),
                CommissionStatus.PENDING,
                "2026-07",
                null,
                OffsetDateTime.now()
        );
    }
}
