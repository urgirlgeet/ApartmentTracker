package com.apartment.tracker.complaint;

import com.apartment.tracker.account.UserAccount;
import com.apartment.tracker.account.UserAccountRepository;
import com.apartment.tracker.maintenance.MaintenanceStaff;
import com.apartment.tracker.maintenance.MaintenanceStaffRepository;
import com.apartment.tracker.maintenance.MaintenanceDepartment;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComplaintService {
    private final ComplaintRepository repo;
    private final MaintenanceStaffRepository staffRepository;
    private final UserAccountRepository accountRepository;
    private final ComplaintImageStorage imageStorage;
    private final ComplaintHistoryRepository historyRepository;
    private final ComplaintCodeService complaintCodeService;

    public ComplaintService(
        ComplaintRepository repo,
        MaintenanceStaffRepository staffRepository,
        UserAccountRepository accountRepository,
        ComplaintImageStorage imageStorage,
        ComplaintHistoryRepository historyRepository,
        ComplaintCodeService complaintCodeService) {
        this.repo = repo;
        this.staffRepository = staffRepository;
        this.accountRepository = accountRepository;
        this.imageStorage = imageStorage;
        this.historyRepository = historyRepository;
        this.complaintCodeService = complaintCodeService;
    }

    public List<Complaint> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    public List<Complaint> getForResident(String username) {
        return repo.findByCreatedByOrderByCreatedAtDesc(username);
    }

    public Map<Long, List<ComplaintHistory>> getHistoryFor(List<Complaint> complaints) {
        if (complaints.isEmpty()) {
            return Map.of();
        }
        return historyRepository.findByComplaintInOrderByOccurredAtAsc(complaints).stream()
            .collect(Collectors.groupingBy(ComplaintHistory::getComplaintId));
    }

    public List<Complaint> getWorkForMaintenanceHead(String username) {
        UserAccount head = getMaintenanceHead(username);
        List<ComplaintCategory> categories = head.getDepartment() == null
            ? List.of(ComplaintCategory.values())
            : head.getDepartment().getComplaintCategories();
        return repo.findVisibleToMaintenanceHead(username, ComplaintStatus.CLOSED, categories);
    }

    @Transactional
    public void raise(
        ComplaintCategory category,
        String description,
        String createdBy,
        MultipartFile image) throws IOException {
        Complaint complaint = new Complaint();
        complaint.setCategory(category);
        complaint.setComplaintCode(complaintCodeService.nextCode(category));
        complaint.setDescription(description);
        complaint.setCreatedBy(createdBy);
        ComplaintImageStorage.StoredImage storedImage = imageStorage.store(image);
        if (storedImage != null) {
            complaint.setImageFilename(storedImage.filename());
            complaint.setImageContentType(storedImage.contentType());
        }
        try {
            Complaint savedComplaint = repo.save(complaint);
            recordHistory(savedComplaint, "Complaint submitted", createdBy);
        } catch (RuntimeException exception) {
            if (storedImage != null) {
                imageStorage.delete(storedImage.filename());
            }
            throw exception;
        }
    }

    @Transactional
    public void assign(Long complaintId, Long staffId, String username, boolean isAdmin) {
        Complaint complaint = findComplaint(complaintId);
        if (complaint.getStatus() == ComplaintStatus.RESOLVED || complaint.getStatus().isTerminal()) {
            throw new IllegalStateException("Resolved or closed complaints cannot be reassigned.");
        }
        if (!isAdmin && complaint.getAssignedStaff() != null
            && !username.equals(complaint.getAssignedStaff().getHeadUsername())) {
            throw new AccessDeniedException("Only the complaint's maintenance team can reassign this work.");
        }
        MaintenanceStaff staff = (isAdmin
            ? staffRepository.findByIdAndActiveTrue(staffId)
            : staffRepository.findByIdAndHeadUsernameAndActiveTrue(staffId, username))
            .orElseThrow(() -> new EntityNotFoundException("Active maintenance staff not found: " + staffId));
        UserAccount staffHead = accountRepository.findById(staff.getHeadUsername())
            .orElseThrow(() -> new EntityNotFoundException(
                "Maintenance account not found: " + staff.getHeadUsername()));
        if (staffHead.getDepartment() != complaint.getCategory().getDepartment()) {
            throw new IllegalStateException("Choose staff from the department for this complaint category.");
        }
        if (!isAdmin && complaint.getCategory().getDepartment() != getMaintenanceHead(username).getDepartment()) {
            throw new IllegalStateException("This complaint belongs to another maintenance department.");
        }
        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setAssignedStaff(staff);
        if (complaint.getStatus() == ComplaintStatus.SUBMITTED) {
            complaint.setStatus(ComplaintStatus.ASSIGNED);
        }
        repo.save(complaint);
        String description = "Assigned to " + staff.getName();
        if (previousStatus != complaint.getStatus()) {
            description += " (" + displayStatus(previousStatus) + " to " + displayStatus(complaint.getStatus()) + ")";
        }
        recordHistory(complaint, description, username);
    }

    @Transactional
    public void advanceForMaintenance(Long complaintId, String username, boolean isAdmin) {
        Complaint complaint = findComplaint(complaintId);
        if (complaint.getAssignedStaff() == null || complaint.getStatus().isTerminal()
            || complaint.getStatus() == ComplaintStatus.SUBMITTED
            || complaint.getStatus() == ComplaintStatus.RESOLVED) {
            throw new IllegalStateException("This complaint is not ready for a maintenance status update.");
        }
        if (!isAdmin && !username.equals(complaint.getAssignedStaff().getHeadUsername())) {
            throw new AccessDeniedException("This complaint is not assigned to your maintenance team.");
        }
        ComplaintStatus previousStatus = complaint.getStatus();
        complaint.setStatus(complaint.getStatus().next());
        repo.save(complaint);
        recordHistory(complaint,
            "Status changed from " + displayStatus(previousStatus) + " to " + displayStatus(complaint.getStatus()),
            username);
    }

    @Transactional
    public void advance(Long id, String username) {
        Complaint complaint = findComplaint(id);
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new IllegalStateException("Only a resolved complaint can be closed by the administrator.");
        }
        complaint.setStatus(ComplaintStatus.CLOSED);
        repo.save(complaint);
        recordHistory(complaint, "Complaint closed", username);
    }

    @Transactional
    public void confirmResolved(Long complaintId, String residentUsername) {
        Complaint complaint = findComplaint(complaintId);
        ensureResidentOwns(complaint, residentUsername);
        if (complaint.getStatus() != ComplaintStatus.RESOLVED) {
            throw new IllegalStateException("Only a staff-resolved complaint can be confirmed.");
        }
        complaint.setResidentFeedback(null);
        complaint.setStatus(ComplaintStatus.CLOSED);
        repo.save(complaint);
        recordHistory(complaint, "Resident confirmed the resolution and closed the complaint", residentUsername);
    }

    @Transactional
    public void reopen(Long complaintId, String residentUsername, String reason) {
        Complaint complaint = findComplaint(complaintId);
        ensureResidentOwns(complaint, residentUsername);
        if (complaint.getStatus() != ComplaintStatus.RESOLVED
            && complaint.getStatus() != ComplaintStatus.CLOSED) {
            throw new IllegalStateException("Only a resolved or closed complaint can be reopened.");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() > 500) {
            throw new IllegalArgumentException("Enter a reason of 1 to 500 characters to reopen this complaint.");
        }
        complaint.setResidentFeedback(reason.trim());
        complaint.setStatus(ComplaintStatus.REOPENED);
        repo.save(complaint);
        recordHistory(complaint, "Resident reopened the complaint: " + reason.trim(), residentUsername);
    }

    public ComplaintImageStorage.StoredImageFile getImage(Long complaintId, String username, boolean canViewAll) {
        Complaint complaint = findComplaint(complaintId);
        if (!canViewAll && !complaint.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("You cannot view this complaint image.");
        }
        if (complaint.getImageFilename() == null) {
            throw new EntityNotFoundException("This complaint does not have an image.");
        }
        return imageStorage.load(complaint.getImageFilename(), complaint.getImageContentType());
    }

    private Complaint findComplaint(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Complaint not found: " + id));
    }

    private void recordHistory(Complaint complaint, String description, String actorUsername) {
        historyRepository.save(new ComplaintHistory(complaint, description, actorUsername));
    }

    private String displayStatus(ComplaintStatus status) {
        return status.name().replace('_', ' ');
    }

    private UserAccount getMaintenanceHead(String username) {
        return accountRepository.findById(username)
            .orElseThrow(() -> new EntityNotFoundException("Maintenance account not found: " + username));
    }

    private void ensureResidentOwns(Complaint complaint, String username) {
        if (!complaint.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("You can only update your own complaints.");
        }
    }
}