package com.ivedi.ivedikuryemanagement.branch.impl;

import com.ivedi.ivedikuryemanagement.branch.api.BranchSearchDto;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class BranchSpecification {

    private BranchSpecification() {}

    public static Specification<Branch> buildFrom(BranchSearchDto dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Eagerly fetch associations only on data queries, not count queries
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("restaurant", JoinType.LEFT);
                root.fetch("region", JoinType.LEFT);
            }

            if (dto.restaurantId() != null) {
                predicates.add(cb.equal(root.get("restaurant").get("id"), dto.restaurantId()));
            }

            if (dto.regionId() != null) {
                predicates.add(cb.equal(root.get("region").get("id"), dto.regionId()));
            }

            if (StringUtils.hasText(dto.name())) {
                predicates.add(cb.like(
                    cb.lower(root.get("name")),
                    "%" + dto.name().trim().toLowerCase() + "%"
                ));
            }

            if (dto.status() != null) {
                predicates.add(cb.equal(root.get("status"), dto.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
