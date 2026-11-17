package com.apartment.tracker.profile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResidentProfileRepository extends JpaRepository<ResidentProfile, String> {
    boolean existsByEmailIgnoreCase(String email);
}
