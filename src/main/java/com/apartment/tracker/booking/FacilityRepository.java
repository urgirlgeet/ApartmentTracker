package com.apartment.tracker.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    boolean existsByName(String name);

    List<Facility> findAllByOrderByNameAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select facility from Facility facility where facility.id = :id")
    Optional<Facility> findByIdForUpdate(@Param("id") Long id);
}
