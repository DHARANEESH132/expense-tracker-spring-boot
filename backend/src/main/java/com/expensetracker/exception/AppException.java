package com.expensetracker.exception;

import org.springframework.http.HttpStatus;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final List<Map<String, String>> errors;

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errors = Collections.emptyList();
    }

    public AppException(String message, HttpStatus status, List<Map<String, String>> errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public List<Map<String, String>> getErrors() {
        return errors;
    }
}
