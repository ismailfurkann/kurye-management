package com.ivedi.ivedikuryemanagement.common.exception;

import com.ivedi.ivedikuryemanagement.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateActiveAssignmentConstraint_returns409AssignmentConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_da_one_active_per_order\"");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ASSIGNMENT_CONFLICT");
        assertThat(response.getBody().getMessage()).doesNotContain("uq_da_one_active_per_order");
    }

    @Test
    void duplicateEmailConstraint_returns409EmailConflict() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "duplicate key value violates unique constraint \"uq_users_email\"");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void unknownDataIntegrity_returnsGeneric409WithoutSqlDetails() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("database constraint failed");

        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrity(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("DATA_INTEGRITY_CONFLICT");
        assertThat(response.getBody().getMessage()).isEqualTo("Data integrity conflict");
    }

    @Test
    void malformedBody_returnsGeneric400Message() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error",
                mock(HttpInputMessage.class)
        );

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnreadableMessage(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("MALFORMED_REQUEST_BODY");
        assertThat(response.getBody().getMessage()).isEqualTo("Request body is invalid or malformed");
    }

    @Test
    void authenticationFailure_returnsGenericMessage() {
        ResponseEntity<ApiResponse<Void>> response =
                handler.handleAuthentication(new BadCredentialsException("Bad credentials for test@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getErrorCode()).isEqualTo("AUTHENTICATION_FAILED");
        assertThat(response.getBody().getMessage()).isEqualTo("Authentication failed");
    }
}
