package com.apartment.tracker.booking;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class FacilityInitializer implements ApplicationRunner {
    private final FacilityRepository facilityRepository;

    public FacilityInitializer(FacilityRepository facilityRepository) {
        this.facilityRepository = facilityRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        addIfMissing("Gym", "Fitness room for resident use.");
        addIfMissing("Clubhouse", "Shared lounge and activity space.");
        addIfMissing("Swimming Pool", "Society swimming pool.");
        addIfMissing("Community Hall", "Hall for resident events and gatherings.");
        addIfMissing("Tennis Court", "Outdoor tennis court.");
    }

    private void addIfMissing(String name, String description) {
        if (!facilityRepository.existsByName(name)) {
            facilityRepository.save(new Facility(name, description));
        }
    }
}
