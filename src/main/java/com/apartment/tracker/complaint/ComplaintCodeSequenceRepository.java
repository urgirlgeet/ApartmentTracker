package com.apartment.tracker.complaint;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintCodeSequenceRepository extends JpaRepository<ComplaintCodeSequence, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from ComplaintCodeSequence sequence where sequence.category = :category")
    Optional<ComplaintCodeSequence> findByCategoryForUpdate(@Param("category") String category);
}
