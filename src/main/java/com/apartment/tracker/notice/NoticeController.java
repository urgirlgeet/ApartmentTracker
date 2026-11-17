package com.apartment.tracker.notice;

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

@Controller
@RequestMapping("/notices")
public class NoticeController {
    private final NoticeService service;

    public NoticeController(NoticeService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        addPageData(model, new NoticeForm());
        return "notices";
    }

    @PostMapping
    public String publish(
        @Validated @ModelAttribute("noticeForm") NoticeForm form,
        BindingResult bindingResult,
        Model model,
        Authentication authentication) {
        if (bindingResult.hasErrors()) {
            addPageData(model, form);
            return "notices";
        }

        service.publish(form.getTitle(), form.getDescription(), authentication.getName());
        return "redirect:/notices";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/notices";
    }

    private void addPageData(Model model, NoticeForm form) {
        model.addAttribute("noticeForm", form);
        model.addAttribute("notices", service.getAll());
    }
}
