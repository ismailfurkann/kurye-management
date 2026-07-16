package com.ivedi.ivedikuryemanagement.region.impl;

import com.ivedi.ivedikuryemanagement.region.api.RegionSearchDto;
import com.ivedi.ivedikuryemanagement.region.api.RegionStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class RegionSpecification {

    private RegionSpecification() {}

    public static Specification<Region> buildFrom(RegionSearchDto dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(dto.city())) {
                predicates.add(cb.like(
                    cb.lower(root.get("city")),
                    "%" + dto.city().trim().toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(dto.district())) {
                predicates.add(cb.like(
                    cb.lower(root.get("district")),
                    "%" + dto.district().trim().toLowerCase() + "%"
                ));
            }

            if (dto.status() != null) {
                predicates.add(cb.equal(root.get("status"), dto.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
