package com.ivedi.ivedikuryemanagement.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends BaseException {

    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }

    public BusinessException(String message, String errorCode, HttpStatus status) {
        super(message, status, errorCode);
    }
}
