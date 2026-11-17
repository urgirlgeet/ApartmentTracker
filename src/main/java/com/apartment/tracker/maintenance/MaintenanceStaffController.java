package com.apartment.tracker.maintenance;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceStaffController {
    private final MaintenanceStaffService service;

    public MaintenanceStaffController(MaintenanceStaffService service) {
        this.service = service;
    }

    @GetMapping("/staff")
    public String staff(Model model, Authentication authentication) {
        addStaffPageData(model, new MaintenanceStaffForm(), authentication);
        return "maintenance-staff";
    }

    @PostMapping("/staff")
    public String add(
        @Validated @ModelAttribute("staffForm") MaintenanceStaffForm form,
        BindingResult bindingResult,
        Model model,
        Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addStaffPageData(model, form, authentication);
            return "maintenance-staff";
        }
        service.add(form, authentication.getName());
        return "redirect:/maintenance/staff";
    }

    @PostMapping("/staff/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication authentication) {
        service.deactivate(id, authentication.getName());
        return "redirect:/maintenance/staff";
    }

    private void addStaffPageData(Model model, MaintenanceStaffForm form, Authentication authentication) {
        model.addAttribute("staffForm", form);
        model.addAttribute("staffMembers", service.getAllWithDepartments());
        model.addAttribute("isMaintenanceHead", true);
        model.addAttribute("managerName", authentication.getName());
        model.addAttribute("departmentName", service.getDepartmentName(authentication.getName()));
    }
}
