package com.apartment.tracker.billing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class PaymentForm {
    @DecimalMin(value = "0.01", message = "Enter a payment amount greater than zero.")
    @Digits(integer = 8, fraction = 2, message = "Use an amount with at most two decimal places.")
    private BigDecimal amount;

    @Size(max = 100, message = "Reference must be at most 100 characters.")
    private String reference;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
