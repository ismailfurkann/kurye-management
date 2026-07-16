package com.ivedi.ivedikuryemanagement.branch.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BranchResponseTest {

    @Test
    @DisplayName("from() maps frontend field names")
    void from_mapsFrontendFields() {
        UUID branchId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        BranchDto dto = new BranchDto(
                branchId,
                restaurantId,
                "Restaurant",
                UUID.randomUUID(),
                "Region",
                "Ankara",
                "Cankaya",
                "Merkez",
                "+903121234567",
                "Ataturk Caddesi No:1",
                null,
                null,
                BranchStatus.ACTIVE,
                null,
                null
        );

        BranchResponse response = BranchResponse.from(dto);

        assertThat(response.id()).isEqualTo(branchId);
        assertThat(response.restaurantId()).isEqualTo(restaurantId);
        assertThat(response.address()).isEqualTo("Ataturk Caddesi No:1");
        assertThat(response.city()).isEqualTo("Ankara");
        assertThat(response.district()).isEqualTo("Cankaya");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("from() maps inactive status to isActive false and tolerates null region fields")
    void from_inactiveAndNullRegionFields() {
        BranchDto dto = new BranchDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Restaurant",
                null,
                null,
                null,
                null,
                "Branch",
                null,
                null,
                null,
                null,
                BranchStatus.INACTIVE,
                null,
                null
        );

        BranchResponse response = BranchResponse.from(dto);

        assertThat(response.isActive()).isFalse();
        assertThat(response.city()).isNull();
        assertThat(response.district()).isNull();
    }
}
