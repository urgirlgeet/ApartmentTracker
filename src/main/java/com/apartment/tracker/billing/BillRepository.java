package com.apartment.tracker.billing;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
    @EntityGraph(attributePaths = {"items", "payments"})
    List<Bill> findAllByOrderByBillingMonthDescApartmentNumberAsc();

    @EntityGraph(attributePaths = {"items", "payments"})
    List<Bill> findByApartmentNumberOrderByBillingMonthDesc(String apartmentNumber);

    boolean existsByApartmentNumberAndBillingMonth(String apartmentNumber, String billingMonth);
}
