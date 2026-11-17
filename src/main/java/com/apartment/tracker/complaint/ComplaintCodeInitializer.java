package com.apartment.tracker.complaint;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ComplaintCodeInitializer implements ApplicationRunner {
    private final ComplaintCodeService complaintCodeService;

    public ComplaintCodeInitializer(ComplaintCodeService complaintCodeService) {
        this.complaintCodeService = complaintCodeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        complaintCodeService.backfillExistingCodes();
    }
}
