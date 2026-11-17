package com.apartment.tracker.complaint;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<Complaint> findByCategoryOrderByIdAsc(ComplaintCategory category);

    List<Complaint> findByAssignedStaffIsNotNullOrderByCreatedAtAsc();

    @Query("""
        select complaint from Complaint complaint
        left join complaint.assignedStaff assignedStaff
        where complaint.category in :categories
          and (assignedStaff.headUsername = :headUsername
               or (assignedStaff is null and complaint.status <> :closedStatus))
        order by complaint.createdAt asc
        """)
    List<Complaint> findVisibleToMaintenanceHead(
        @Param("headUsername") String headUsername,
        @Param("closedStatus") ComplaintStatus closedStatus,
        @Param("categories") List<ComplaintCategory> categories);
}