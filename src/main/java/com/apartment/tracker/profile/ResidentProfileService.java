package com.apartment.tracker.profile;

import org.springframework.stereotype.Service;

@Service
public class ResidentProfileService {
    private final ResidentProfileRepository repository;

    public ResidentProfileService(ResidentProfileRepository repository) {
        this.repository = repository;
    }

    public ResidentProfile getOrCreate(String username) {
        return repository.findById(username).orElseGet(() -> {
            ResidentProfile profile = new ResidentProfile();
            profile.setUsername(username);
            profile.setFullName(username);
            profile.setPhone("");
            profile.setApartmentNumber("");
            return repository.save(profile);
        });
    }

    public ResidentProfileForm getForm(String username) {
        return ResidentProfileForm.from(getOrCreate(username));
    }

    public void preserveApartmentAddress(String username, ResidentProfileForm form) {
        String currentAddress = getOrCreate(username).getApartmentNumber();
        if (ApartmentAddress.isValid(currentAddress)) {
            String[] address = ApartmentAddress.parse(currentAddress);
            form.setApartmentBlock(address[0]);
            form.setApartmentUnit(Integer.valueOf(address[1]));
        }
    }

    public void update(String username, ResidentProfileForm form) {
        ResidentProfile profile = getOrCreate(username);
        if (!ApartmentAddress.isValid(profile.getApartmentNumber())) {
            if (!ApartmentAddress.isValidSelection(form.getApartmentBlock(), form.getApartmentUnit())) {
                throw new IllegalArgumentException("Choose a block from A to Z and an apartment from 1 to 8.");
            }
            profile.setApartmentNumber(ApartmentAddress.format(form.getApartmentBlock(), form.getApartmentUnit()));
        }
        profile.setFullName(form.getFullName().trim());
        profile.setPhone(form.getPhone().trim());
        profile.setEmail(normalizeOptional(form.getEmail()));
        profile.setEmergencyContactName(normalizeOptional(form.getEmergencyContactName()));
        profile.setEmergencyContactPhone(normalizeOptional(form.getEmergencyContactPhone()));
        repository.save(profile);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
