package com.apartment.tracker.maintenance;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceStaffCodeSequenceRepository
    extends JpaRepository<MaintenanceStaffCodeSequence, MaintenanceDepartment> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select sequence from MaintenanceStaffCodeSequence sequence where sequence.department = :department")
    Optional<MaintenanceStaffCodeSequence> findByDepartmentForUpdate(
        @Param("department") MaintenanceDepartment department);
}
