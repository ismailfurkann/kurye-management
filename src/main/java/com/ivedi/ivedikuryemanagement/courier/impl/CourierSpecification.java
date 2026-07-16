package com.ivedi.ivedikuryemanagement.courier.impl;

import com.ivedi.ivedikuryemanagement.courier.api.CourierSearchDto;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CourierSpecification {

    private CourierSpecification() {}

    public static Specification<Courier> buildFrom(CourierSearchDto dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("user", JoinType.LEFT);
                root.fetch("region", JoinType.LEFT);
            }

            if (dto.availabilityStatus() != null) {
                predicates.add(cb.equal(root.get("availabilityStatus"), dto.availabilityStatus()));
            }

            if (dto.approvalStatus() != null) {
                predicates.add(cb.equal(root.get("approvalStatus"), dto.approvalStatus()));
            }

            if (dto.vehicleType() != null) {
                predicates.add(cb.equal(root.get("vehicleType"), dto.vehicleType()));
            }

            if (dto.regionId() != null) {
                predicates.add(cb.equal(root.get("region").get("id"), dto.regionId()));
            }

            if (dto.search() != null && !dto.search().isBlank()) {
                String like = "%" + dto.search().trim().toLowerCase() + "%";
                var user = root.join("user", JoinType.LEFT);
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("fullName")), like),
                        cb.like(cb.lower(user.get("email")), like),
                        cb.like(cb.lower(user.get("phone")), like),
                        cb.like(cb.lower(root.get("plateNumber")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
