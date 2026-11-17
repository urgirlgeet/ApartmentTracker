package com.apartment.tracker.profile;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ResidentProfileController {
    private final ResidentProfileService service;

    public ResidentProfileController(ResidentProfileService service) {
        this.service = service;
    }

    @GetMapping
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("profile", service.getOrCreate(authentication.getName()));
        model.addAttribute("username", authentication.getName());
        return "profile";
    }

    @GetMapping("/edit")
    public String edit(Authentication authentication, Model model) {
        model.addAttribute("profileForm", service.getForm(authentication.getName()));
        model.addAttribute("username", authentication.getName());
        addApartmentPageData(authentication.getName(), model);
        addApartmentOptions(model);
        return "profile-edit";
    }

    @PostMapping
    public String update(
        @Validated @ModelAttribute("profileForm") ResidentProfileForm form,
        BindingResult bindingResult,
        Authentication authentication,
        Model model,
        RedirectAttributes redirectAttributes) {
        service.preserveApartmentAddress(authentication.getName(), form);
        if (!ApartmentAddress.isValid(service.getOrCreate(authentication.getName()).getApartmentNumber())
            && !ApartmentAddress.isValidSelection(form.getApartmentBlock(), form.getApartmentUnit())) {
            bindingResult.rejectValue(
                "apartmentBlock", "Invalid", "Choose a block from A to Z and an apartment from 1 to 8.");
            bindingResult.rejectValue(
                "apartmentUnit", "Invalid", "Choose a block from A to Z and an apartment from 1 to 8.");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("username", authentication.getName());
            addApartmentPageData(authentication.getName(), model);
            addApartmentOptions(model);
            return "profile-edit";
        }

        try {
            service.update(authentication.getName(), form);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("apartment", exception.getMessage());
            model.addAttribute("username", authentication.getName());
            addApartmentPageData(authentication.getName(), model);
            addApartmentOptions(model);
            return "profile-edit";
        }
        redirectAttributes.addFlashAttribute("profileSaved", true);
        return "redirect:/profile";
    }

    private void addApartmentOptions(Model model) {
        model.addAttribute("apartmentBlocks", ApartmentAddress.blocks());
        model.addAttribute("apartmentNumbers", ApartmentAddress.apartmentNumbers());
    }

    private void addApartmentPageData(String username, Model model) {
        String apartmentNumber = service.getOrCreate(username).getApartmentNumber();
        model.addAttribute("flatAddress", apartmentNumber);
        model.addAttribute("apartmentNeedsSelection", !ApartmentAddress.isValid(apartmentNumber));
    }
}
