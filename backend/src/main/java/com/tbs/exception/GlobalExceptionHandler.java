package com.tbs.exception;

import com.tbs.dto.common.ApiErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired(required = false)
    private Environment environment;

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException e) {
        log.warn("Unauthorized access attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("UNAUTHORIZED", "Authentication required")
                ));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("USER_NOT_FOUND", e.getMessage())
                ));
    }

    @ExceptionHandler(com.tbs.exception.UserNotInRankingException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotInRanking(com.tbs.exception.UserNotInRankingException e) {
        log.warn("User not in ranking: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("USER_NOT_IN_RANKING", e.getMessage())
                ));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(BadRequestException e) {
        log.warn("Bad request: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("BAD_REQUEST", e.getMessage())
                ));
    }

    @ExceptionHandler(GameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleGameNotFound(GameNotFoundException e) {
        log.warn("Game not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("GAME_NOT_FOUND", e.getMessage())
                ));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException e) {
        log.warn("Forbidden: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("FORBIDDEN", e.getMessage())
                ));
    }

    @ExceptionHandler(GameNotInProgressException.class)
    public ResponseEntity<ApiErrorResponse> handleGameNotInProgress(GameNotInProgressException e) {
        log.warn("Game not in progress: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("GAME_NOT_IN_PROGRESS", e.getMessage())
                ));
    }

    @ExceptionHandler(InvalidMoveException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidMove(InvalidMoveException e) {
        log.warn("Invalid move: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("INVALID_MOVE", e.getMessage())
                ));
    }

    @ExceptionHandler(InvalidGameTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidGameType(InvalidGameTypeException e) {
        log.warn("Invalid game type: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("INVALID_GAME_TYPE", e.getMessage())
                ));
    }

    @ExceptionHandler(TokenBlacklistException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenBlacklistException(TokenBlacklistException e) {
        log.error("Token blacklist operation failed: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails(
                                "INTERNAL_SERVER_ERROR",
                                "Token management operation failed",
                                null
                        )
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String message = e.getMessage();
        if (e.getCause() != null && e.getCause() instanceof IllegalArgumentException) {
            message = e.getCause().getMessage();
        }
        log.warn("HTTP message not readable: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("INVALID_REQUEST_BODY", message != null ? message : "Invalid request body format")
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("VALIDATION_ERROR", errors)
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String errors = e.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));

        log.warn("Constraint violation: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("VALIDATION_ERROR", errors)
                ));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleDataAccessException(DataAccessException e) {
        log.error("Database error occurred: {}", e.getMessage(), e);
        Throwable cause = e.getCause();
        
        while (cause != null) {
            log.error("Root cause: {}", cause.getMessage(), cause);
            if (cause instanceof org.postgresql.util.PSQLException) {
                break;
            }
            cause = cause.getCause();
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails(
                                "INTERNAL_SERVER_ERROR",
                                "A database error occurred",
                                null
                        )
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMessage(), e);
        
        String errorMessage = "A database constraint violation occurred";
        String errorCode = "DATA_INTEGRITY_VIOLATION";
        
        if (e.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            org.hibernate.exception.ConstraintViolationException cve = (org.hibernate.exception.ConstraintViolationException) e.getCause();
            String constraintName = cve.getConstraintName();
            
            if (constraintName != null) {
                String constraintNameLower = constraintName.toLowerCase();
                if (constraintNameLower.contains("username") || constraintNameLower.contains("users_username_key")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(new ApiErrorResponse(
                                    new ApiErrorResponse.ErrorDetails("USERNAME_EXISTS", "Username already exists")
                            ));
                }
                if (constraintNameLower.contains("email") || constraintNameLower.contains("users_email_key")) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(new ApiErrorResponse(
                                    new ApiErrorResponse.ErrorDetails("EMAIL_EXISTS", "Email already exists")
                            ));
                }
                log.warn("Constraint name: {}", constraintName);
            }
            
            if (cve.getCause() != null && cve.getCause() instanceof org.postgresql.util.PSQLException) {
                org.postgresql.util.PSQLException psqlEx = (org.postgresql.util.PSQLException) cve.getCause();
                if (psqlEx.getServerErrorMessage() != null) {
                    String serverMessage = psqlEx.getServerErrorMessage().getMessage();
                    String detail = psqlEx.getServerErrorMessage().getDetail();
                    log.warn("PostgreSQL constraint violation: {} - Detail: {}", serverMessage, detail);
                    errorMessage = serverMessage != null ? serverMessage : errorMessage;
                    if (detail != null) {
                        errorMessage += " - " + detail;
                    }
                }
            }
        }
        
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails(
                                errorCode,
                                errorMessage,
                                null
                        )
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails(
                                "BAD_REQUEST",
                                e.getMessage(),
                                null
                        )
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalStateException(IllegalStateException e) {
        log.error("Illegal state: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails(
                                "INTERNAL_SERVER_ERROR",
                                e.getMessage(),
                                null
                        )
                ));
    }

    @ExceptionHandler(UserAlreadyInQueueException.class)
    public ResponseEntity<ApiErrorResponse> handleUserAlreadyInQueue(UserAlreadyInQueueException e) {
        log.warn("User already in queue: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("USER_ALREADY_IN_QUEUE", e.getMessage())
                ));
    }

    @ExceptionHandler(UserNotInQueueException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotInQueue(UserNotInQueueException e) {
        log.warn("User not in queue: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("USER_NOT_IN_QUEUE", e.getMessage())
                ));
    }

    @ExceptionHandler(UserHasActiveGameException.class)
    public ResponseEntity<ApiErrorResponse> handleUserHasActiveGame(UserHasActiveGameException e) {
        log.warn("User has active game: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("USER_HAS_ACTIVE_GAME", e.getMessage())
                ));
    }

    @ExceptionHandler(CannotChallengeSelfException.class)
    public ResponseEntity<ApiErrorResponse> handleCannotChallengeSelf(CannotChallengeSelfException e) {
        log.warn("Cannot challenge self: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("CANNOT_CHALLENGE_SELF", e.getMessage())
                ));
    }

    @ExceptionHandler(UserUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleUserUnavailable(UserUnavailableException e) {
        log.warn("User unavailable: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("USER_UNAVAILABLE", e.getMessage())
                ));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException e) {
        log.warn("Conflict: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("CONFLICT", e.getMessage())
                ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceeded(RateLimitExceededException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Remaining", String.valueOf(e.getRemainingRequests()))
                .header("X-RateLimit-Reset", String.valueOf(e.getTimeToResetSeconds()))
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails("RATE_LIMIT_EXCEEDED", e.getMessage())
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception e) {
        log.error("Unexpected error occurred: {}", e.getMessage(), e);
        
        String message = "An unexpected error occurred";
        if (e.getCause() != null) {
            log.error("Root cause: {}", e.getCause().getMessage(), e.getCause());
        }
        
        boolean isProduction = isProductionEnvironment();
        if (!isProduction && e.getMessage() != null) {
            message += ": " + e.getMessage();
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        new ApiErrorResponse.ErrorDetails(
                                "INTERNAL_SERVER_ERROR",
                                message,
                                null
                        )
                ));
    }

    private boolean isProductionEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles == null || activeProfiles.length == 0) {
            String defaultProfile = environment.getProperty("spring.profiles.default", "dev");
            return "prod".equals(defaultProfile) || "production".equals(defaultProfile);
        }
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }
}

