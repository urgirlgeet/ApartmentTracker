package com.apartment.tracker.maintenance;

import com.apartment.tracker.account.UserAccount;
import com.apartment.tracker.account.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaintenanceStaffCodeService {
    private static final Pattern CODE_NUMBER = Pattern.compile("^S[A-Z](\\d+)$");
    private static final Pattern LEGACY_CODE = Pattern.compile("^ST-\\d+$");

    private final MaintenanceStaffCodeSequenceRepository sequenceRepository;
    private final MaintenanceStaffRepository staffRepository;
    private final UserAccountRepository accountRepository;

    public MaintenanceStaffCodeService(
        MaintenanceStaffCodeSequenceRepository sequenceRepository,
        MaintenanceStaffRepository staffRepository,
        UserAccountRepository accountRepository) {
        this.sequenceRepository = sequenceRepository;
        this.staffRepository = staffRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public String nextCode(String headUsername) {
        MaintenanceDepartment department = getDepartment(headUsername);
        MaintenanceStaffCodeSequence sequence = sequenceRepository.findByDepartmentForUpdate(department)
            .orElseThrow(() -> new IllegalStateException("Maintenance staff code sequence is not initialized."));
        int number = sequence.getNextNumber();
        sequence.setNextNumber(number + 1);
        return department.getStaffCodePrefix() + number;
    }

    @Transactional
    public void backfillExistingCodes() {
        for (MaintenanceDepartment department : MaintenanceDepartment.values()) {
            MaintenanceStaffCodeSequence sequence = sequenceRepository.findByDepartmentForUpdate(department)
                .orElseGet(() -> sequenceRepository.save(new MaintenanceStaffCodeSequence(department, 1)));
            int nextNumber = sequence.getNextNumber();
            var staffMembers = staffRepository.findAllByOrderByNameAsc();
            for (MaintenanceStaff staff : staffMembers) {
                if (staff.getStaffCode() != null) {
                    Matcher matcher = CODE_NUMBER.matcher(staff.getStaffCode());
                    if (matcher.matches() && staff.getStaffCode().startsWith(department.getStaffCodePrefix())) {
                        nextNumber = Math.max(nextNumber, Integer.parseInt(matcher.group(1)) + 1);
                    }
                }
            }
            for (MaintenanceStaff staff : staffMembers) {
                boolean belongsToDepartment = staff.getHeadUsername() != null
                    && accountRepository.findById(staff.getHeadUsername())
                        .map(UserAccount::getDepartment)
                        .map(value -> value == null ? MaintenanceDepartment.GENERAL : value)
                        .map(department::equals)
                        .orElse(false);
                boolean needsCode = staff.getStaffCode() == null
                    || LEGACY_CODE.matcher(staff.getStaffCode()).matches();
                if (belongsToDepartment && needsCode) {
                    staff.setStaffCode(department.getStaffCodePrefix() + nextNumber++);
                }
            }
            sequence.setNextNumber(nextNumber);
        }
    }

    private MaintenanceDepartment getDepartment(String username) {
        UserAccount head = accountRepository.findById(username)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance account not found: " + username));
        return head.getDepartment() == null ? MaintenanceDepartment.GENERAL : head.getDepartment();
    }
}
