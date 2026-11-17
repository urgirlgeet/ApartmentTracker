package com.apartment.tracker.booking;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacilityBookingService {
    private final FacilityRepository facilityRepository;
    private final FacilityBookingRepository bookingRepository;

    public FacilityBookingService(
        FacilityRepository facilityRepository,
        FacilityBookingRepository bookingRepository) {
        this.facilityRepository = facilityRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Facility> getFacilities() {
        return facilityRepository.findAllByOrderByNameAsc();
    }

    public List<FacilityBooking> getBookings(String username, boolean isAdmin) {
        return isAdmin
            ? bookingRepository.findAllByOrderByStartAtAsc()
            : bookingRepository.findByResidentUsernameOrderByStartAtAsc(username);
    }

    @Transactional
    public void book(FacilityBookingForm form, String residentUsername) {
        if (form.getStartAt() == null || form.getEndAt() == null
            || !form.getStartAt().isAfter(LocalDateTime.now())
            || !form.getEndAt().isAfter(form.getStartAt())) {
            throw new IllegalArgumentException("Choose a future start time and an end time after it.");
        }

        Facility facility = facilityRepository.findByIdForUpdate(form.getFacilityId())
            .orElseThrow(() -> new EntityNotFoundException("Facility not found: " + form.getFacilityId()));
        boolean overlaps = bookingRepository
            .findByFacility_IdAndStatusOrderByStartAtAsc(facility.getId(), FacilityBookingStatus.BOOKED)
            .stream()
            .anyMatch(existing -> form.getStartAt().isBefore(existing.getEndAt())
                && form.getEndAt().isAfter(existing.getStartAt()));
        if (overlaps) {
            throw new IllegalArgumentException("That time overlaps an existing booking. Choose another slot.");
        }

        bookingRepository.save(new FacilityBooking(
            facility, residentUsername, form.getStartAt(), form.getEndAt()));
    }

    @Transactional
    public void cancel(Long bookingId, String username, boolean isAdmin) {
        FacilityBooking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (!isAdmin && !booking.getResidentUsername().equals(username)) {
            throw new AccessDeniedException("You can only cancel your own bookings.");
        }
        if (booking.getStatus() != FacilityBookingStatus.BOOKED) {
            throw new IllegalStateException("This booking has already been cancelled.");
        }
        booking.cancel();
    }
}
