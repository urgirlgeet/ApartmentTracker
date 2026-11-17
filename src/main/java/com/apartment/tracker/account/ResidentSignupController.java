package com.apartment.tracker.account;

import jakarta.validation.Valid;
import com.apartment.tracker.profile.ApartmentAddress;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResidentSignupController {
    private final ResidentRegistrationService registrationService;

    public ResidentSignupController(ResidentRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        addApartmentOptions(model);
        if (!model.containsAttribute("signupForm")) {
            model.addAttribute("signupForm", new ResidentSignupForm());
        }
        return "signup";
    }

    @PostMapping("/signup")
    public String register(
        @Valid @ModelAttribute("signupForm") ResidentSignupForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes) {
        addApartmentOptions(model);
        if (form.getProfile().getEmail() == null || form.getProfile().getEmail().isBlank()) {
            bindingResult.rejectValue("profile.email", "NotBlank", "Enter your email address.");
        }
        if (!ApartmentAddress.isValidSelection(
            form.getProfile().getApartmentBlock(), form.getProfile().getApartmentUnit())) {
            bindingResult.rejectValue(
                "profile.apartmentBlock", "Invalid", "Choose a block from A to Z and an apartment from 1 to 8.");
            bindingResult.rejectValue(
                "profile.apartmentUnit", "Invalid", "Choose a block from A to Z and an apartment from 1 to 8.");
        }
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            registrationService.register(form);
        } catch (DuplicateRegistrationException exception) {
            if ("registration".equals(exception.getField())) {
                bindingResult.reject("duplicate", exception.getMessage());
            } else {
                bindingResult.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            }
            return "signup";
        }

        redirectAttributes.addFlashAttribute("registered", true);
        return "redirect:/login";
    }

    private void addApartmentOptions(Model model) {
        model.addAttribute("apartmentBlocks", ApartmentAddress.blocks());
        model.addAttribute("apartmentNumbers", ApartmentAddress.apartmentNumbers());
    }
}
