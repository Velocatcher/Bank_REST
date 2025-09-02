package com.example.bankcards.exception;

import com.example.bankcards.dto.error.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import io.jsonwebtoken.JwtException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(ex.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    // 403 (твои доменные)
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(ex.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    // 400 (твои доменные)
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    // 400 (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ApiError(msg, req.getRequestURI(), OffsetDateTime.now()));
    }

    // ===== Security-specific =====

    // 401 — неправильный логин/пароль
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        log.debug("Bad credentials on {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("bad credentials", req.getRequestURI(), OffsetDateTime.now()));
    }

    // 401 — учётка заблокирована/отключена/просрочена и т.п.
    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiError> handleAccountStatus(AccountStatusException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(ex.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

    // 401 — нет/некорректные учётные данные в контексте
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ApiError> handleAuthCredsMissing(AuthenticationCredentialsNotFoundException ex,
                                                           HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("authentication required", req.getRequestURI(), OffsetDateTime.now()));
    }

    // 401 — любые прочие AuthenticationException (например, Provider/Token ошибки)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, HttpServletRequest req) {
        log.debug("Authentication failure on {}: {}", req.getRequestURI(), ex.getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("authentication failed", req.getRequestURI(), OffsetDateTime.now()));
    }

    // 401 — невалидный/просроченный JWT
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwt(JwtException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("invalid token", req.getRequestURI(), OffsetDateTime.now()));
    }

    // 403 — из Spring Security (у тебя уже был)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("access denied", req.getRequestURI(), OffsetDateTime.now()));
    }

    // 500 — всё остальное
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on request {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("internal error", req.getRequestURI(), OffsetDateTime.now()));
    }

    // 400 — для кривых запросов
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(ex.getMessage(), req.getRequestURI(), OffsetDateTime.now()));
    }

}
