package com.ivedi.ivedikuryemanagement.restaurant.impl;

import com.ivedi.ivedikuryemanagement.restaurant.api.RestaurantSearchDto;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class RestaurantSpecification {

    private RestaurantSpecification() {}

    public static Specification<Restaurant> buildFrom(RestaurantSearchDto dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("owner", JoinType.LEFT);
            }

            if (StringUtils.hasText(dto.name())) {
                String pattern = "%" + dto.name().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("legalName")), pattern)
                ));
            }

            if (dto.status() != null) {
                predicates.add(cb.equal(root.get("status"), dto.status()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
