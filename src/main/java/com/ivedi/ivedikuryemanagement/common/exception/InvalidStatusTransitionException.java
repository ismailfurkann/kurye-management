package com.ivedi.ivedikuryemanagement.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends BaseException {

    public InvalidStatusTransitionException(String fromStatus, String toStatus) {
        super(
            "Invalid status transition: " + fromStatus + " → " + toStatus,
            HttpStatus.UNPROCESSABLE_ENTITY,
            "INVALID_STATUS_TRANSITION"
        );
    }

    public InvalidStatusTransitionException(Enum<?> from, Enum<?> to) {
        this(from.name(), to.name());
    }
}
