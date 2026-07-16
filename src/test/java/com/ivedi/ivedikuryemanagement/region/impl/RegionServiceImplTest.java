package com.ivedi.ivedikuryemanagement.region.impl;

import com.ivedi.ivedikuryemanagement.common.exception.DuplicateResourceException;
import com.ivedi.ivedikuryemanagement.common.exception.ResourceNotFoundException;
import com.ivedi.ivedikuryemanagement.region.api.RegionDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionMapper;
import com.ivedi.ivedikuryemanagement.region.api.RegionSearchDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import com.ivedi.ivedikuryemanagement.region.web.RegionRequest;
import com.ivedi.ivedikuryemanagement.region.web.RegionStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionServiceImplTest {

    @Mock private RegionRepository regionRepository;
    @Mock private RegionMapper regionMapper;

    @InjectMocks
    private RegionServiceImpl regionService;

    private Region sampleRegion;
    private RegionDto sampleDto;
    private UUID regionId;

    @BeforeEach
    void setUp() {
        regionId = UUID.randomUUID();
        sampleRegion = Region.builder()
                .id(regionId)
                .name("Selçuklu")
                .city("Konya")
                .district("Selçuklu")
                .status(RegionStatus.ACTIVE)
                .build();

        sampleDto = new RegionDto(regionId, "Selçuklu", "Konya", "Selçuklu", null, RegionStatus.ACTIVE, null, null);
    }

    @Nested
    @DisplayName("create()")
    class CreateTests {

        private final RegionRequest validRequest =
                new RegionRequest("Selçuklu", "Konya", "Selçuklu", null);

        @Test
        @DisplayName("unique name in area → persists and returns DTO")
        void create_uniqueRegion_success() {
            when(regionRepository.existsByNameAndCityAndDistrict("Selçuklu", "Konya", "Selçuklu"))
                    .thenReturn(false);
            when(regionRepository.save(any(Region.class))).thenReturn(sampleRegion);
            when(regionMapper.toDto(sampleRegion)).thenReturn(sampleDto);

            RegionDto result = regionService.create(validRequest);

            assertThat(result.name()).isEqualTo("Selçuklu");
            verify(regionRepository).save(any(Region.class));
        }

        @Test
        @DisplayName("trims whitespace before duplicate check and persist")
        void create_trimsWhitespace_beforeCheck() {
            RegionRequest requestWithSpaces = new RegionRequest("  Selçuklu  ", "  Konya  ", "  Selçuklu  ", null);
            when(regionRepository.existsByNameAndCityAndDistrict("Selçuklu", "Konya", "Selçuklu"))
                    .thenReturn(false);
            when(regionRepository.save(any(Region.class))).thenReturn(sampleRegion);
            when(regionMapper.toDto(any())).thenReturn(sampleDto);

            regionService.create(requestWithSpaces);

            ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
            verify(regionRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Selçuklu");
            assertThat(captor.getValue().getCity()).isEqualTo("Konya");
        }

        @Test
        @DisplayName("duplicate name in same city+district → throws DuplicateResourceException")
        void create_duplicateInArea_throwsDuplicateResourceException() {
            when(regionRepository.existsByNameAndCityAndDistrict("Selçuklu", "Konya", "Selçuklu"))
                    .thenReturn(true);

            assertThatThrownBy(() -> regionService.create(validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("Selçuklu");

            verify(regionRepository, never()).save(any());
        }

        @Test
        @DisplayName("same name in different district → allowed")
        void create_sameNameDifferentDistrict_success() {
            RegionRequest differentDistrict = new RegionRequest("Merkez", "Konya", "Meram", null);
            when(regionRepository.existsByNameAndCityAndDistrict("Merkez", "Konya", "Meram"))
                    .thenReturn(false);
            when(regionRepository.save(any())).thenReturn(sampleRegion);
            when(regionMapper.toDto(any())).thenReturn(sampleDto);

            RegionDto result = regionService.create(differentDistrict);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("update()")
    class UpdateTests {

        private final RegionRequest updateRequest =
                new RegionRequest("Yeni İsim", "Konya", "Selçuklu", "updated desc");

        @Test
        @DisplayName("valid update on existing region → returns updated DTO")
        void update_existingRegion_success() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(sampleRegion));
            when(regionRepository.existsByNameAndCityAndDistrictAndIdNot("Yeni İsim", "Konya", "Selçuklu", regionId))
                    .thenReturn(false);
            when(regionRepository.save(sampleRegion)).thenReturn(sampleRegion);
            when(regionMapper.toDto(sampleRegion)).thenReturn(sampleDto);

            RegionDto result = regionService.update(regionId, updateRequest);

            assertThat(result).isNotNull();
            verify(regionRepository).save(sampleRegion);
        }

        @Test
        @DisplayName("non-existent id → throws ResourceNotFoundException")
        void update_nonExistentId_throwsNotFoundException() {
            UUID unknownId = UUID.randomUUID();
            when(regionRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.update(unknownId, updateRequest))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("name conflicts with another region in same area → throws DuplicateResourceException")
        void update_nameConflict_throwsDuplicateResourceException() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(sampleRegion));
            when(regionRepository.existsByNameAndCityAndDistrictAndIdNot("Yeni İsim", "Konya", "Selçuklu", regionId))
                    .thenReturn(true);

            assertThatThrownBy(() -> regionService.update(regionId, updateRequest))
                    .isInstanceOf(DuplicateResourceException.class);

            verify(regionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("existing id → returns DTO")
        void getById_existingId_returnsDto() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(sampleRegion));
            when(regionMapper.toDto(sampleRegion)).thenReturn(sampleDto);

            RegionDto result = regionService.getById(regionId);

            assertThat(result.id()).isEqualTo(regionId);
        }

        @Test
        @DisplayName("non-existent id → throws ResourceNotFoundException")
        void getById_nonExistentId_throwsNotFoundException() {
            UUID unknown = UUID.randomUUID();
            when(regionRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.getById(unknown))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(unknown.toString());
        }
    }

    @Nested
    @DisplayName("search()")
    class SearchTests {

        @Test
        @DisplayName("search with filters → delegates to specification executor")
        void search_withFilters_returnsPage() {
            RegionSearchDto searchDto = new RegionSearchDto("Konya", null, RegionStatus.ACTIVE, 0, 20);
            Page<Region> regionPage = new PageImpl<>(List.of(sampleRegion));

            when(regionRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(regionPage);
            when(regionMapper.toDto(sampleRegion)).thenReturn(sampleDto);

            Page<RegionDto> result = regionService.search(searchDto);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).city()).isEqualTo("Konya");
        }
    }

    @Nested
    @DisplayName("getAllActive()")
    class GetAllActiveTests {

        @Test
        @DisplayName("returns only ACTIVE regions sorted by city + name")
        void getAllActive_returnsOnlyActiveRegions() {
            when(regionRepository.findAllByStatusOrderByCityAscNameAsc(RegionStatus.ACTIVE))
                    .thenReturn(List.of(sampleRegion));
            when(regionMapper.toDtoList(List.of(sampleRegion))).thenReturn(List.of(sampleDto));

            List<RegionDto> result = regionService.getAllActive();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo(RegionStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("setStatus()")
    class SetStatusTests {

        @Test
        @DisplayName("ACTIVE → INACTIVE changes status and saves")
        void setStatus_activeToInactive_savesNewStatus() {
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(sampleRegion));
            when(regionRepository.save(sampleRegion)).thenReturn(sampleRegion);
            when(regionMapper.toDto(sampleRegion)).thenReturn(sampleDto);

            regionService.setStatus(regionId, new RegionStatusRequest(RegionStatus.INACTIVE));

            verify(regionRepository).save(sampleRegion);
            assertThat(sampleRegion.getStatus()).isEqualTo(RegionStatus.INACTIVE);
        }

        @Test
        @DisplayName("same status → no-op, does not call save")
        void setStatus_sameStatus_noOp() {
            sampleRegion.setStatus(RegionStatus.ACTIVE);
            when(regionRepository.findById(regionId)).thenReturn(Optional.of(sampleRegion));
            when(regionMapper.toDto(sampleRegion)).thenReturn(sampleDto);

            regionService.setStatus(regionId, new RegionStatusRequest(RegionStatus.ACTIVE));

            verify(regionRepository, never()).save(any());
        }

        @Test
        @DisplayName("non-existent id → throws ResourceNotFoundException")
        void setStatus_nonExistentId_throwsNotFoundException() {
            UUID unknown = UUID.randomUUID();
            when(regionRepository.findById(unknown)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> regionService.setStatus(unknown, new RegionStatusRequest(RegionStatus.INACTIVE)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
