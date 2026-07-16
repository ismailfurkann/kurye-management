package com.ivedi.ivedikuryemanagement.region.web;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import com.ivedi.ivedikuryemanagement.region.api.RegionDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionService;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegionControllerTest {

    @Mock private RegionService regionService;

    @Test
    void createAlias_usesRegionService() {
        RegionController controller = new RegionController(regionService);
        RegionRequest request = new RegionRequest("Merkez", "Ankara", "Cankaya", null);
        RegionDto dto = dto(RegionStatus.ACTIVE);
        when(regionService.create(request)).thenReturn(dto);

        ResponseEntity<ApiResponse<RegionDto>> response = controller.createAlias(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody().getData()).isEqualTo(dto);
        verify(regionService).create(request);
    }

    @Test
    void updateAlias_usesRegionService() {
        RegionController controller = new RegionController(regionService);
        UUID id = UUID.randomUUID();
        RegionRequest request = new RegionRequest("Merkez", "Ankara", "Cankaya", null);
        RegionDto dto = dto(RegionStatus.ACTIVE);
        when(regionService.update(id, request)).thenReturn(dto);

        ResponseEntity<ApiResponse<RegionDto>> response = controller.updateAlias(id, request);

        assertThat(response.getBody().getData()).isEqualTo(dto);
        verify(regionService).update(id, request);
    }

    @Test
    void toggleStatusAlias_usesRegionService() {
        RegionController controller = new RegionController(regionService);
        UUID id = UUID.randomUUID();
        RegionDto dto = dto(RegionStatus.INACTIVE);
        when(regionService.toggleStatus(id)).thenReturn(dto);

        ResponseEntity<ApiResponse<RegionDto>> response = controller.toggleStatusAlias(id);

        assertThat(response.getBody().getData().status()).isEqualTo(RegionStatus.INACTIVE);
        verify(regionService).toggleStatus(id);
    }

    private RegionDto dto(RegionStatus status) {
        return new RegionDto(
                UUID.randomUUID(),
                "Merkez",
                "Ankara",
                "Cankaya",
                null,
                status,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
