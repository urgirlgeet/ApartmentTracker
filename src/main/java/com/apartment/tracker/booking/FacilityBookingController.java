package com.apartment.tracker.booking;

import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bookings")
public class FacilityBookingController {
    private final FacilityBookingService service;

    public FacilityBookingController(FacilityBookingService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model, Authentication authentication) {
        addPageData(model, new FacilityBookingForm(), authentication);
        return "bookings";
    }

    @PostMapping
    public String book(
        @Validated @ModelAttribute("bookingForm") FacilityBookingForm form,
        BindingResult bindingResult,
        Model model,
        Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addPageData(model, form, authentication);
            return "bookings";
        }
        try {
            service.book(form, authentication.getName());
        } catch (IllegalArgumentException | EntityNotFoundException exception) {
            model.addAttribute("bookingError", exception.getMessage());
            addPageData(model, form, authentication);
            return "bookings";
        }
        return "redirect:/bookings?created";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
        @PathVariable Long id,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
        service.cancel(id, authentication.getName(), hasRole(authentication, "ROLE_ADMIN"));
        redirectAttributes.addFlashAttribute("bookingCancelled", true);
        return "redirect:/bookings";
    }

    private void addPageData(Model model, FacilityBookingForm form, Authentication authentication) {
        boolean isAdmin = hasRole(authentication, "ROLE_ADMIN");
        model.addAttribute("bookingForm", form);
        model.addAttribute("facilities", service.getFacilities());
        model.addAttribute("bookings", service.getBookings(authentication.getName(), isAdmin));
        model.addAttribute("isAdmin", isAdmin);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}
