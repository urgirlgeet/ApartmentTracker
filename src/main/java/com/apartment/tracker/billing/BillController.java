package com.apartment.tracker.billing;

import com.apartment.tracker.profile.ApartmentAddress;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/bills")
public class BillController {
    private final BillService service;

    public BillController(BillService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
        @RequestParam(defaultValue = "ALL") String status,
        Authentication authentication,
        Model model) {
        addPageData(authentication, model, status);
        return "bills";
    }

    @PostMapping
    public String create(
        @Valid @ModelAttribute("billForm") BillForm form,
        BindingResult bindingResult,
        Authentication authentication,
        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("billError", "Please complete the flat, month, and due date.");
            addPageData(authentication, model, "ALL");
            return "bills";
        }
        try {
            service.create(form, authentication.getName());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("billError", exception.getMessage());
            addPageData(authentication, model, "ALL");
            return "bills";
        }
        return "redirect:/bills?created";
    }

    @PostMapping("/{id}/payments")
    public String recordPayment(
        @PathVariable Long id,
        @Valid @ModelAttribute("paymentForm") PaymentForm form,
        BindingResult bindingResult,
        Authentication authentication,
        RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("paymentError", "Enter a valid payment amount and reference.");
            return "redirect:/bills";
        }
        try {
            service.recordSimulatedPayment(id, authentication.getName(), form.getAmount(), form.getReference());
        } catch (IllegalArgumentException | EntityNotFoundException exception) {
            redirectAttributes.addFlashAttribute("paymentError", exception.getMessage());
            return "redirect:/bills";
        }
        redirectAttributes.addFlashAttribute("paymentRecorded", true);
        return "redirect:/bills";
    }

    private void addPageData(Authentication authentication, Model model, String status) {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        List<Bill> bills = service.getBills(authentication.getName(), isAdmin);
        String normalizedStatus = normalizeStatus(status, isAdmin);
        if (isAdmin && !"ALL".equals(normalizedStatus)) {
            bills = bills.stream()
                .filter(bill -> normalizedStatus.equals(bill.getPaymentStatus()))
                .toList();
        }
        model.addAttribute("bills", bills);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("statusFilter", normalizedStatus);
        model.addAttribute("apartments", ApartmentAddress.allApartments());
        model.addAttribute("billForm", model.containsAttribute("billForm") ? model.getAttribute("billForm") : new BillForm());
        model.addAttribute("paymentForm", new PaymentForm());
        List<Bill> allBills = service.getBills(authentication.getName(), isAdmin);
        model.addAttribute("totalBilled", service.totalBilled(allBills));
        model.addAttribute("totalCollected", service.totalCollected(allBills));
        model.addAttribute("totalOutstanding", service.totalOutstanding(allBills));
    }

    private String normalizeStatus(String status, boolean isAdmin) {
        if (!isAdmin || status == null) {
            return "ALL";
        }
        return switch (status.toUpperCase(java.util.Locale.ROOT)) {
            case "PAID" -> "PAID";
            case "UNPAID" -> "UNPAID";
            case "PARTIALLY_PAID" -> "PARTIALLY_PAID";
            default -> "ALL";
        };
    }
}
