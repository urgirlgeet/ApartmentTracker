package com.apartment.tracker.complaint;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintHistoryRepository extends JpaRepository<ComplaintHistory, Long> {
    List<ComplaintHistory> findByComplaintInOrderByOccurredAtAsc(List<Complaint> complaints);
}
