package com.apartment.tracker.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

public class BillForm {
    @NotBlank(message = "Choose a flat.")
    @Pattern(regexp = "^[A-Z]-[1-8]$", message = "Choose a valid block and apartment.")
    private String apartmentNumber;

    @NotBlank(message = "Choose a billing month.")
    @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "Choose a valid billing month.")
    private String billingMonth;

    @NotNull(message = "Choose a due date.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    @Valid
    @Size(max = 6)
    private List<BillItemForm> items = new ArrayList<>(
        List.of(new BillItemForm(), new BillItemForm(), new BillItemForm(), new BillItemForm()));

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public String getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(String billingMonth) {
        this.billingMonth = billingMonth;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public List<BillItemForm> getItems() {
        return items;
    }

    public void setItems(List<BillItemForm> items) {
        this.items = items;
    }
}
