package com.apartment.tracker.billing;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    columnNames = {"apartment_number", "billing_month"}))
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "apartment_number", nullable = false, length = 4)
    private String apartmentNumber;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private Set<BillItem> items = new LinkedHashSet<>();

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("recordedAt DESC")
    private Set<BillPayment> payments = new LinkedHashSet<>();

    protected Bill() {
    }

    public Bill(String apartmentNumber, String billingMonth, LocalDate dueDate, String createdBy) {
        this.apartmentNumber = apartmentNumber;
        this.billingMonth = billingMonth;
        this.dueDate = dueDate;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public String getBillingMonth() {
        return billingMonth;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Set<BillItem> getItems() {
        return items;
    }

    public Set<BillPayment> getPayments() {
        return payments;
    }

    public BigDecimal getTotalAmount() {
        return items.stream().map(BillItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPaidAmount() {
        return payments.stream().map(BillPayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getBalance() {
        return getTotalAmount().subtract(getPaidAmount()).max(BigDecimal.ZERO);
    }

    public String getStatus() {
        if (getBalance().signum() == 0) {
            return "PAID";
        }
        if (dueDate.isBefore(LocalDate.now())) {
            return getPaidAmount().signum() > 0 ? "PARTIALLY PAID · OVERDUE" : "OVERDUE";
        }
        if (getPaidAmount().signum() > 0) {
            return "PARTIALLY PAID";
        }
        return "UNPAID";
    }

    public String getPaymentStatus() {
        if (getBalance().signum() == 0) {
            return "PAID";
        }
        return getPaidAmount().signum() > 0 ? "PARTIALLY_PAID" : "UNPAID";
    }

    public boolean isOverdue() {
        return getBalance().signum() > 0 && dueDate.isBefore(LocalDate.now());
    }

    public void addItem(String description, BigDecimal amount) {
        items.add(new BillItem(this, description, amount));
    }

    public void addPayment(BillPayment payment) {
        payments.add(payment);
    }
}
