package com.apartment.tracker.complaint;

public enum ComplaintStatus { 
    SUBMITTED, 
    ASSIGNED, 
    IN_PROGRESS, 
    RESOLVED,
    REOPENED,
    CLOSED;

    public ComplaintStatus next() {
        return switch (this) {
            case SUBMITTED -> ASSIGNED;
            case ASSIGNED -> IN_PROGRESS;
            case IN_PROGRESS -> RESOLVED;
            case RESOLVED -> CLOSED;
            case REOPENED -> IN_PROGRESS;
            case CLOSED -> CLOSED;
        };
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}