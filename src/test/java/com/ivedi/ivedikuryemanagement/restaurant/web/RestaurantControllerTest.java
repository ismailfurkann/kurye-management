package com.ivedi.ivedikuryemanagement.restaurant.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.common.response.PageResponse;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantResponse;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantSearchDto;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantService;
import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RestaurantControllerTest {

    private final RestaurantService restaurantService = mock(RestaurantService.class);
    private final RestaurantController controller = new RestaurantController(restaurantService);

    @Test
    @DisplayName("GET /api/v1/restaurants returns PageResponse and forwards search/status")
    void searchAlias_returnsPageResponse() {
        RestaurantResponse item = response();
        when(restaurantService.searchResponses(new RestaurantSearchDto("restoran", RestaurantStatus.ACTIVE, 0, 20)))
                .thenReturn(new PageImpl<>(List.of(item)));

        ResponseEntity<ApiResponse<PageResponse<RestaurantResponse>>> response =
                controller.searchAlias("restoran", RestaurantStatus.ACTIVE, 0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).containsExactly(item);
        assertThat(response.getBody().getData().totalElements()).isEqualTo(1);
        assertThat(response.getBody().getData().content().get(0).ownerUserId()).isNotNull();

        ArgumentCaptor<RestaurantSearchDto> captor = ArgumentCaptor.forClass(RestaurantSearchDto.class);
        verify(restaurantService).searchResponses(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("restoran");
        assertThat(captor.getValue().status()).isEqualTo(RestaurantStatus.ACTIVE);
    }

    @Test
    @DisplayName("legacy admin list endpoint still delegates to restaurant search")
    void adminSearch_stillWorks() {
        when(restaurantService.searchResponses(new RestaurantSearchDto("old", null, 0, 20)))
                .thenReturn(new PageImpl<>(List.of(response())));

        ResponseEntity<ApiResponse<PageResponse<RestaurantResponse>>> response =
                controller.search("old", null, 0, 20);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        verify(restaurantService).searchResponses(new RestaurantSearchDto("old", null, 0, 20));
    }

    @Test
    @DisplayName("alias write endpoints use frontend response contract")
    void writeAliases_returnRestaurantResponse() {
        RestaurantResponse item = response();
        UUID id = item.id();
        RestaurantRequest request = new RestaurantRequest("R", null, null, null, null, UUID.randomUUID());
        RestaurantOwnerRequest ownerRequest = new RestaurantOwnerRequest(UUID.randomUUID());
        RestaurantStatusRequest statusRequest = new RestaurantStatusRequest(RestaurantStatus.ACTIVE);

        when(restaurantService.createResponse(request)).thenReturn(item);
        when(restaurantService.updateResponse(id, request)).thenReturn(item);
        when(restaurantService.setStatusResponse(id, statusRequest)).thenReturn(item);
        when(restaurantService.changeOwnerResponse(id, ownerRequest)).thenReturn(item);

        assertThat(controller.createAlias(request).getBody().getData()).isEqualTo(item);
        assertThat(controller.updateAlias(id, request).getBody().getData()).isEqualTo(item);
        assertThat(controller.setStatusAlias(id, statusRequest).getBody().getData()).isEqualTo(item);
        assertThat(controller.assignOwnerAlias(id, ownerRequest).getBody().getData()).isEqualTo(item);
    }

    private RestaurantResponse response() {
        return new RestaurantResponse(
                UUID.randomUUID(),
                "Ivedi Restoran",
                "info@example.com",
                "+905551234567",
                RestaurantStatus.ACTIVE,
                UUID.randomUUID(),
                "Owner",
                2,
                0,
                12,
                new BigDecimal("420.50"),
                null,
                null
        );
    }
}
