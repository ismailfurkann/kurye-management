package com.ivedi.ivedikuryemanagement.auth.impl;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserBranchAssignmentId implements Serializable {
    private UUID userId;
    private UUID branchId;
}
