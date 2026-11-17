package com.apartment.tracker.account;

import com.apartment.tracker.profile.ResidentProfile;
import com.apartment.tracker.profile.ResidentProfileRepository;
import com.apartment.tracker.profile.ApartmentAddress;
import jakarta.transaction.Transactional;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ResidentRegistrationService {
    private final UserAccountRepository accountRepository;
    private final ResidentProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public ResidentRegistrationService(
        UserAccountRepository accountRepository,
        ResidentProfileRepository profileRepository,
        PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(ResidentSignupForm form) {
        String username = form.getUsername().trim().toLowerCase(Locale.ROOT);
        String email = form.getProfile().getEmail().trim().toLowerCase(Locale.ROOT);
        if (accountRepository.existsById(username)) {
            throw new DuplicateRegistrationException("That username is already taken.", "username");
        }
        if (profileRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateRegistrationException("An account already uses that email address.", "profile.email");
        }

        UserAccount account = new UserAccount(
            username,
            passwordEncoder.encode(form.getPassword()),
            AccountRole.RESIDENT);
        ResidentProfile profile = new ResidentProfile();
        profile.setUsername(username);
        profile.setFullName(form.getProfile().getFullName().trim());
        profile.setPhone(form.getProfile().getPhone().trim());
        profile.setApartmentNumber(ApartmentAddress.format(
            form.getProfile().getApartmentBlock(), form.getProfile().getApartmentUnit()));
        profile.setEmail(email);
        profile.setEmergencyContactName(normalizeOptional(form.getProfile().getEmergencyContactName()));
        profile.setEmergencyContactPhone(normalizeOptional(form.getProfile().getEmergencyContactPhone()));

        try {
            accountRepository.saveAndFlush(account);
            profileRepository.saveAndFlush(profile);
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateRegistrationException(
                "This username or email is already registered. Please check the details and try again.",
                "registration");
        }
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
