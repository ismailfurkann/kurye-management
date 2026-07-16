package com.ivedi.ivedikuryemanagement.branch.web;

import com.ivedi.ivedikuryemanagement.branch.api.BranchResponse;
import com.ivedi.ivedikuryemanagement.branch.api.BranchSearchDto;
import com.ivedi.ivedikuryemanagement.branch.api.BranchService;
import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BranchControllerTest {

    private final BranchService branchService = mock(BranchService.class);
    private final BranchController controller = new BranchController(branchService);

    @Test
    @DisplayName("GET /api/v1/branches returns PageResponse and forwards filters")
    void searchAlias_returnsPageResponse() {
        UUID restaurantId = UUID.randomUUID();
        BranchResponse item = response(restaurantId);
        when(branchService.searchResponses(new BranchSearchDto(restaurantId, null, "merkez", null, 0, 20)))
                .thenReturn(new PageImpl<>(List.of(item)));

        ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> response =
                controller.searchAlias(restaurantId, "merkez", 0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).containsExactly(item);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);

        ArgumentCaptor<BranchSearchDto> captor = ArgumentCaptor.forClass(BranchSearchDto.class);
        verify(branchService).searchResponses(captor.capture());
        assertThat(captor.getValue().restaurantId()).isEqualTo(restaurantId);
        assertThat(captor.getValue().name()).isEqualTo("merkez");
    }

    @Test
    @DisplayName("legacy admin list endpoint still works with PageResponse")
    void adminSearch_stillWorks() {
        when(branchService.searchResponses(new BranchSearchDto(null, null, "old", null, 0, 20)))
                .thenReturn(new PageImpl<>(List.of(response(UUID.randomUUID()))));

        ResponseEntity<ApiResponse<PageResponse<BranchResponse>>> response =
                controller.search(null, null, "old", null, 0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("branch aliases delegate to service and path restaurantId wins on create")
    void aliases_delegate() {
        UUID restaurantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        BranchResponse item = response(restaurantId);
        BranchRequest body = new BranchRequest(UUID.randomUUID(), UUID.randomUUID(), "Merkez", null, null, null, null);

        when(branchService.createResponse(org.mockito.ArgumentMatchers.any(BranchRequest.class))).thenReturn(item);
        when(branchService.updateResponse(branchId, body)).thenReturn(item);
        when(branchService.toggleStatus(branchId)).thenReturn(item);
        when(branchService.getResponseById(branchId)).thenReturn(item);
        when(branchService.getByRestaurantIdResponsesForCurrentUser(restaurantId)).thenReturn(List.of(item));

        assertThat(controller.createAlias(restaurantId, body).getBody().getData()).isEqualTo(item);
        assertThat(controller.updateAlias(branchId, body).getBody().getData()).isEqualTo(item);
        assertThat(controller.toggleStatusAlias(branchId).getBody().getData()).isEqualTo(item);
        assertThat(controller.getByIdAlias(branchId).getBody().getData()).isEqualTo(item);
        assertThat(controller.getByRestaurantAlias(restaurantId).getBody().getData()).containsExactly(item);

        ArgumentCaptor<BranchRequest> captor = ArgumentCaptor.forClass(BranchRequest.class);
        verify(branchService).createResponse(captor.capture());
        assertThat(captor.getValue().restaurantId()).isEqualTo(restaurantId);
    }

    private BranchResponse response(UUID restaurantId) {
        return new BranchResponse(
                UUID.randomUUID(),
                "Merkez",
                restaurantId,
                "Restaurant",
                "Address",
                "Cankaya",
                "Ankara",
                null,
                true,
                null,
                null
        );
    }
}
