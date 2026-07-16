package com.ivedi.ivedikuryemanagement.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    @DisplayName("from() copies only frontend pagination fields")
    void from_copiesPaginationFields() {
        Page<String> page = new PageImpl<>(
                List.of("a", "b"),
                PageRequest.of(1, 2),
                5
        );

        PageResponse<String> response = PageResponse.from(page);

        assertThat(response.content()).containsExactly("a", "b");
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.number()).isEqualTo(1);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isFalse();
    }
}
