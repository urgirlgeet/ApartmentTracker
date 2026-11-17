package com.apartment.tracker.booking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityBookingRepository extends JpaRepository<FacilityBooking, Long> {
    List<FacilityBooking> findByResidentUsernameOrderByStartAtAsc(String residentUsername);

    List<FacilityBooking> findAllByOrderByStartAtAsc();

    List<FacilityBooking> findByFacility_IdAndStatusOrderByStartAtAsc(
        Long facilityId,
        FacilityBookingStatus status);
}
