package com.apartment.tracker.maintenance;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceStaffCodeInitializer implements ApplicationRunner {
    private final MaintenanceStaffCodeService codeService;

    public MaintenanceStaffCodeInitializer(MaintenanceStaffCodeService codeService) {
        this.codeService = codeService;
    }

    @Override
    public void run(ApplicationArguments args) {
        codeService.backfillExistingCodes();
    }
}
