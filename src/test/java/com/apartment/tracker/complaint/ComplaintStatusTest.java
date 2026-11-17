package com.apartment.tracker.complaint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ComplaintStatusTest {
    @Test
    void advancesThroughTheComplaintLifecycle() {
        assertEquals(ComplaintStatus.ASSIGNED, ComplaintStatus.SUBMITTED.next());
        assertEquals(ComplaintStatus.IN_PROGRESS, ComplaintStatus.ASSIGNED.next());
        assertEquals(ComplaintStatus.RESOLVED, ComplaintStatus.IN_PROGRESS.next());
        assertEquals(ComplaintStatus.CLOSED, ComplaintStatus.RESOLVED.next());
        assertEquals(ComplaintStatus.IN_PROGRESS, ComplaintStatus.REOPENED.next());
    }

    @Test
    void closedComplaintDoesNotAdvanceFurther() {
        assertEquals(ComplaintStatus.CLOSED, ComplaintStatus.CLOSED.next());
        org.junit.jupiter.api.Assertions.assertTrue(ComplaintStatus.CLOSED.isTerminal());
    }
}
