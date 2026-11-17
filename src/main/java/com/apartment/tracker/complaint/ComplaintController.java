package com.apartment.tracker.complaint;

import com.apartment.tracker.maintenance.MaintenanceStaffService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/complaints")
public class ComplaintController {
    private final ComplaintService service;
    private final MaintenanceStaffService staffService;

    public ComplaintController(ComplaintService service, MaintenanceStaffService staffService) {
        this.service = service;
        this.staffService = staffService;
    }

    @GetMapping
    public String list(
        @RequestParam(required = false, defaultValue = "") String q,
        Model model,
        Authentication authentication) {
        addPageData(model, new ComplaintForm(), authentication, null, q);
        return "complaints";
    }

    @PostMapping
    public String create(
        @Validated @ModelAttribute("complaintForm") ComplaintForm form,
        BindingResult bindingResult,
        Model model,
        Authentication authentication) throws IOException {
        if (bindingResult.hasErrors()) {
            addPageData(model, form, authentication, null, "");
            return "complaints";
        }

        try {
            service.raise(form.getCategory(), form.getDescription().trim(), authentication.getName(), form.getImage());
        } catch (IllegalArgumentException exception) {
            addPageData(model, form, authentication, exception.getMessage(), "");
            return "complaints";
        }
        return "redirect:/complaints";
    }

    @PostMapping("/{id}/advance")
    public String advance(
        @PathVariable Long id,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
        try {
            service.advance(id, authentication.getName());
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("complaintError", exception.getMessage());
        }
        return "redirect:/complaints";
    }

    @PostMapping("/{id}/assign")
    public String assign(
        @PathVariable Long id,
        @RequestParam Long staffId,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
        try {
            service.assign(id, staffId, authentication.getName(), hasRole(authentication, "ROLE_ADMIN"));
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("complaintError", exception.getMessage());
        }
        return "redirect:/complaints";
    }

    @PostMapping("/{id}/status")
    public String advanceForMaintenance(
        @PathVariable Long id,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
        try {
            service.advanceForMaintenance(id, authentication.getName(), hasRole(authentication, "ROLE_ADMIN"));
        } catch (IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("complaintError", exception.getMessage());
        }
        return "redirect:/complaints";
    }

    @PostMapping("/{id}/confirm")
    public String confirmResolved(@PathVariable Long id, Authentication authentication) {
        service.confirmResolved(id, authentication.getName());
        return "redirect:/complaints";
    }

    @PostMapping("/{id}/reopen")
    public String reopen(
        @PathVariable Long id,
        @RequestParam String reason,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
        try {
            service.reopen(id, authentication.getName(), reason);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("reopenError", exception.getMessage());
        }
        return "redirect:/complaints";
    }

    private void addPageData(
        Model model,
        ComplaintForm form,
        Authentication authentication,
        String uploadError,
        String searchQuery) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        boolean isMaintenance = hasRole(authentication, "ROLE_MAINTENANCE_HEAD");
        model.addAttribute("complaintForm", form);
        model.addAttribute("categories", ComplaintCategory.values());
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("isMaintenance", isMaintenance);
        model.addAttribute("uploadError", uploadError);
        model.addAttribute("complaintError", model.getAttribute("complaintError"));
        List<Complaint> visibleComplaints = isAdmin ? service.getAll()
            : isMaintenance ? service.getWorkForMaintenanceHead(authentication.getName())
            : service.getForResident(authentication.getName());
        List<Complaint> complaints = filterComplaints(visibleComplaints, searchQuery, isAdmin || isMaintenance);
        model.addAttribute("complaints", complaints);
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("staffByComplaint", complaints.stream().collect(Collectors.toMap(
            Complaint::getId,
            complaint -> isAdmin || isMaintenance
                ? staffService.getActiveForComplaint(
                    complaint.getCategory(), authentication.getName(), isAdmin)
                : java.util.List.of())));
        Map<Long, List<ComplaintHistory>> historyByComplaint = service.getHistoryFor(complaints);
        complaints.forEach(complaint ->
            historyByComplaint.computeIfAbsent(complaint.getId(), ignored -> List.of()));
        model.addAttribute("historyByComplaint", historyByComplaint);
    }

    private List<Complaint> filterComplaints(List<Complaint> complaints, String query, boolean searchable) {
        if (!searchable || query == null || query.isBlank()) {
            return complaints;
        }
        String normalizedQuery = query.trim().toLowerCase(java.util.Locale.ROOT);
        return complaints.stream()
            .filter(complaint -> matches(complaint, normalizedQuery))
            .toList();
    }

    private boolean matches(Complaint complaint, String query) {
        return contains(complaint.getComplaintCode(), query)
            || contains(complaint.getCategory().getDisplayName(), query)
            || contains(complaint.getDescription(), query)
            || contains(complaint.getStatus().name(), query)
            || contains(complaint.getCreatedBy(), query)
            || contains(complaint.getResidentFeedback(), query)
            || (complaint.getAssignedStaff() != null
                && (contains(complaint.getAssignedStaff().getName(), query)
                    || contains(complaint.getAssignedStaff().getStaffCode(), query)
                    || contains(complaint.getAssignedStaff().getSpecialization(), query)));
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains(query);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
