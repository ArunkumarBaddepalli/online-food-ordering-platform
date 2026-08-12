package com.app.fooddelivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    /** A refused action is a 403, not a 400 — the request was well formed. */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<String> handleForbidden(ForbiddenException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.FORBIDDEN);
    }

    /**
     * An unknown URL is a 404, not a server fault. Without this the catch-all
     * below turned every mistyped path into a 500.
     */
    @ExceptionHandler({ org.springframework.web.servlet.NoHandlerFoundException.class,
            org.springframework.web.servlet.resource.NoResourceFoundException.class })
    public ResponseEntity<String> handleNotFound(Exception ex) {
        return new ResponseEntity<>("Not found", HttpStatus.NOT_FOUND);
    }

    /**
     * A missing or unreadable parameter is the caller's mistake, so it is a 400.
     * The catch-all below reported these as 500, which reads as "the server is
     * broken" when in fact the request was incomplete.
     */
    @ExceptionHandler({ org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class })
    public ResponseEntity<String> handleBadRequest(Exception ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        return new ResponseEntity<>("An unexpected error occurred: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
