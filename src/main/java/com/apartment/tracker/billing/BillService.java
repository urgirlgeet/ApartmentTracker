package com.apartment.tracker.billing;

import com.apartment.tracker.profile.ApartmentAddress;
import com.apartment.tracker.profile.ResidentProfile;
import com.apartment.tracker.profile.ResidentProfileRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillService {
    private final BillRepository billRepository;
    private final ResidentProfileRepository profileRepository;

    public BillService(BillRepository billRepository, ResidentProfileRepository profileRepository) {
        this.billRepository = billRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional(readOnly = true)
    public List<Bill> getBills(String username, boolean isAdmin) {
        if (isAdmin) {
            return billRepository.findAllByOrderByBillingMonthDescApartmentNumberAsc();
        }
        ResidentProfile profile = profileRepository.findById(username)
            .orElseThrow(() -> new EntityNotFoundException("Resident profile not found: " + username));
        if (!ApartmentAddress.isValid(profile.getApartmentNumber())) {
            return List.of();
        }
        return billRepository.findByApartmentNumberOrderByBillingMonthDesc(profile.getApartmentNumber());
    }

    @Transactional
    public void create(BillForm form, String adminUsername) {
        if (!ApartmentAddress.isValid(form.getApartmentNumber())) {
            throw new IllegalArgumentException("Choose a valid block and apartment.");
        }
        if (form.getDueDate() == null || form.getBillingMonth() == null || form.getBillingMonth().isBlank()) {
            throw new IllegalArgumentException("Choose a billing month and due date.");
        }
        if (billRepository.existsByApartmentNumberAndBillingMonth(
            form.getApartmentNumber(), form.getBillingMonth())) {
            throw new IllegalArgumentException("A bill already exists for that flat and month.");
        }

        Bill bill = new Bill(form.getApartmentNumber(), form.getBillingMonth(), form.getDueDate(), adminUsername);
        int itemCount = 0;
        for (BillItemForm item : form.getItems()) {
            String description = item.getDescription() == null ? "" : item.getDescription().trim();
            BigDecimal amount = item.getAmount();
            if (description.isEmpty() && amount == null) {
                continue;
            }
            if (description.isEmpty() || amount == null || amount.signum() <= 0
                || amount.scale() > 2 || description.length() > 120) {
                throw new IllegalArgumentException(
                    "Each charge needs a description (up to 120 characters) and a positive amount with at most two decimals.");
            }
            bill.addItem(description, amount);
            itemCount++;
        }
        if (itemCount == 0) {
            throw new IllegalArgumentException("Add at least one itemized charge.");
        }
        try {
            billRepository.saveAndFlush(bill);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("A bill already exists for that flat and month.");
        }
    }

    @Transactional
    public void recordSimulatedPayment(Long billId, String username, BigDecimal amount, String reference) {
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new EntityNotFoundException("Bill not found: " + billId));
        ResidentProfile profile = profileRepository.findById(username)
            .orElseThrow(() -> new EntityNotFoundException("Resident profile not found: " + username));
        if (!bill.getApartmentNumber().equals(profile.getApartmentNumber())) {
            throw new org.springframework.security.access.AccessDeniedException(
                "You can only record a payment for a bill associated with your flat.");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new IllegalArgumentException("Enter a positive payment amount with at most two decimals.");
        }
        if (amount.compareTo(bill.getBalance()) > 0) {
            throw new IllegalArgumentException("The payment cannot be greater than the outstanding balance.");
        }
        String normalizedReference = reference == null || reference.isBlank()
            ? null
            : reference.trim();
        if (normalizedReference != null && normalizedReference.length() > 100) {
            throw new IllegalArgumentException("Payment reference must be at most 100 characters.");
        }
        bill.addPayment(new BillPayment(bill, username, amount, normalizedReference));
    }

    @Transactional(readOnly = true)
    public Bill getBill(Long id) {
        return billRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Bill not found: " + id));
    }

    public BigDecimal totalBilled(List<Bill> bills) {
        return bills.stream().map(Bill::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCollected(List<Bill> bills) {
        return bills.stream().map(Bill::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalOutstanding(List<Bill> bills) {
        return bills.stream().map(Bill::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
