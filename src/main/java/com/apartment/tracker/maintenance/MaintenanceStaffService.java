package com.apartment.tracker.maintenance;

import com.apartment.tracker.account.UserAccount;
import com.apartment.tracker.account.UserAccountRepository;
import com.apartment.tracker.complaint.ComplaintCategory;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceStaffService {
    private final MaintenanceStaffRepository repository;
    private final UserAccountRepository accountRepository;
    private final MaintenanceStaffCodeService codeService;

    public MaintenanceStaffService(
        MaintenanceStaffRepository repository,
        UserAccountRepository accountRepository,
        MaintenanceStaffCodeService codeService) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.codeService = codeService;
    }

    public List<MaintenanceStaff> getAllWithDepartments() {
        List<MaintenanceStaff> staffMembers = repository.findAllByOrderByNameAsc();
        for (MaintenanceStaff staff : staffMembers) {
            staff.setDepartmentName(accountRepository.findById(staff.getHeadUsername())
                .map(UserAccount::getDepartment)
                .map(MaintenanceDepartment::getDisplayName)
                .orElse("Department unavailable"));
        }
        return staffMembers;
    }

    public String getDepartmentName(String username) {
        return accountRepository.findById(username)
            .map(UserAccount::getDepartment)
            .map(MaintenanceDepartment::getDisplayName)
            .orElse("General maintenance");
    }

    public List<MaintenanceStaff> getActiveForManager(String username, boolean isAdmin) {
        return isAdmin
            ? repository.findByActiveTrueOrderByNameAsc()
            : repository.findByHeadUsernameAndActiveTrueOrderByNameAsc(username);
    }

    public List<MaintenanceStaff> getActiveForComplaint(
        ComplaintCategory category,
        String username,
        boolean isAdmin) {
        if (!isAdmin) {
            UserAccount head = accountRepository.findById(username)
                .orElseThrow(() -> new EntityNotFoundException("Maintenance account not found: " + username));
            if (head.getDepartment() != category.getDepartment()) {
                return List.of();
            }
            return repository.findByHeadUsernameAndActiveTrueOrderByNameAsc(username);
        }
        return repository.findByActiveTrueOrderByNameAsc().stream()
            .filter(staff -> accountRepository.findById(staff.getHeadUsername())
                .map(UserAccount::getDepartment)
                .filter(category.getDepartment()::equals)
                .isPresent())
            .toList();
    }

    @Transactional
    public void add(MaintenanceStaffForm form, String headUsername) {
        MaintenanceStaff staff = new MaintenanceStaff();
        staff.setName(form.getName().trim());
        staff.setSpecialization(form.getSpecialization().trim());
        staff.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        staff.setHeadUsername(headUsername);
        staff.setStaffCode(codeService.nextCode(headUsername));
        repository.save(staff);
    }

    public void deactivate(Long id, String username) {
        MaintenanceStaff staff = repository.findByIdAndHeadUsernameAndActiveTrue(id, username)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance staff not found: " + id));
        staff.setActive(false);
        repository.save(staff);
    }
}
