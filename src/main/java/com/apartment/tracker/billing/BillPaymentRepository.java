package com.apartment.tracker.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByBill_IdOrderByRecordedAtDesc(Long billId);
}
