package com.apartment.tracker.maintenance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceStaffRepository extends JpaRepository<MaintenanceStaff, Long> {
    List<MaintenanceStaff> findAllByOrderByNameAsc();

    List<MaintenanceStaff> findByActiveTrueOrderByNameAsc();

    List<MaintenanceStaff> findByHeadUsernameOrderByNameAsc(String headUsername);

    List<MaintenanceStaff> findByHeadUsernameAndActiveTrueOrderByNameAsc(String headUsername);

    Optional<MaintenanceStaff> findByIdAndActiveTrue(Long id);

    Optional<MaintenanceStaff> findByIdAndHeadUsernameAndActiveTrue(Long id, String headUsername);
}
