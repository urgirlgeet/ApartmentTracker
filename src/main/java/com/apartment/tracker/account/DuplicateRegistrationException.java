package com.apartment.tracker.account;

public class DuplicateRegistrationException extends RuntimeException {
    private final String field;

    public DuplicateRegistrationException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
