package com.apartment.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apartment.tracker.billing.Bill;
import com.apartment.tracker.billing.BillRepository;
import com.apartment.tracker.billing.BillService;
import com.apartment.tracker.profile.ResidentProfile;
import com.apartment.tracker.profile.ResidentProfileRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
class BillingIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillService billService;

    @Autowired
    private ResidentProfileRepository profileRepository;

    @BeforeEach
    void cleanBillingFixtures() {
        billRepository.deleteAll();
        profileRepository.deleteById("bill-resident-a");
        profileRepository.deleteById("bill-resident-b");
        profileRepository.deleteById("bill-resident-z");
        saveProfile("bill-resident-a", "A-1");
        saveProfile("bill-resident-b", "B-2");
        saveProfile("bill-resident-z", "Z-8");
    }

    @Test
    void adminCreatesItemizedBillsAndResidentsSeeAndRecordTheirOwnPayments() throws Exception {
        createBill("A-1", "2026-11", "100.00", "Monthly maintenance")
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bills?created"));
        createBill("B-2", "2026-11", "200.00", "Monthly maintenance")
            .andExpect(status().is3xxRedirection());

        assertEquals(2, billRepository.count());
        Bill billA = billService.getBills("admin", true).stream()
            .filter(bill -> bill.getApartmentNumber().equals("A-1"))
            .findFirst()
            .orElseThrow();
        assertEquals(new BigDecimal("100.00"), billA.getTotalAmount());

        mockMvc.perform(get("/bills").with(user("bill-resident-a").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("A-1")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("B-2"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Monthly maintenance")));

        mockMvc.perform(post("/bills/{id}/payments", billA.getId())
                .with(user("bill-resident-a").roles("RESIDENT"))
                .with(csrf())
                .param("amount", "40.00")
                .param("reference", "DEMO-123"))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/bills/{id}/payments", billA.getId())
                .with(user("bill-resident-b").roles("RESIDENT"))
                .with(csrf())
                .param("amount", "10.00"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/bills").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Billing overview")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("DEMO-123")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Outstanding")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Monthly bills")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Create a monthly bill")));

        Bill updatedBill = billService.getBills("admin", true).stream()
            .filter(bill -> bill.getId().equals(billA.getId()))
            .findFirst()
            .orElseThrow();
        assertEquals(new BigDecimal("40.00"), updatedBill.getPaidAmount());
        assertEquals(new BigDecimal("60.00"), updatedBill.getBalance());
        assertEquals("PARTIALLY PAID", updatedBill.getStatus());
    }

    @Test
    void rejectsDuplicateMonthlyBillsAndNonAdminBillCreation() throws Exception {
        createBill("A-1", "2026-11", "100.00", "Monthly maintenance")
            .andExpect(status().is3xxRedirection());

        createBill("A-1", "2026-11", "100.00", "Duplicate")
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("already exists")));

        createBill("B-2", "2026-11", "100.00", "Forbidden", "bill-resident-a", "RESIDENT")
            .andExpect(status().isForbidden());
    }

    @Test
    void blocksInvalidFlatSelectionAndOverpayment() throws Exception {
        createBill("Z-8", "2026-12", "25.50", "Water")
            .andExpect(status().is3xxRedirection());

        Bill bill = billRepository.findAll().get(0);
        createBill("A-9", "2026-12", "25.50", "Invalid flat")
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose a valid block and apartment")));
        mockMvc.perform(post("/bills/{id}/payments", bill.getId())
                .with(user("bill-resident-z").roles("RESIDENT"))
                .with(csrf())
                .param("amount", "25.51"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bills"));
    }

    @Test
    void adminCanFilterPaidUnpaidAndPartiallyPaidBillsAndOverdueIsRed() throws Exception {
        createBill("A-1", "2026-10", "100.00", "Monthly maintenance")
            .andExpect(status().is3xxRedirection());
        createBill("B-2", "2026-10", "100.00", "Monthly maintenance")
            .andExpect(status().is3xxRedirection());
        createBill("Z-8", "2026-10", "100.00", "Monthly maintenance",
            LocalDate.now().minusDays(1))
            .andExpect(status().is3xxRedirection());

        Bill billA = billService.getBills("admin", true).stream()
            .filter(bill -> bill.getApartmentNumber().equals("A-1")).findFirst().orElseThrow();
        Bill billB = billService.getBills("admin", true).stream()
            .filter(bill -> bill.getApartmentNumber().equals("B-2")).findFirst().orElseThrow();
        recordPayment(billA, "bill-resident-a", "100.00");
        recordPayment(billB, "bill-resident-b", "35.00");

        mockMvc.perform(get("/bills").param("status", "PAID").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>A-1</strong>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("<strong>B-2</strong>"))));

        mockMvc.perform(get("/bills").param("status", "PARTIALLY_PAID").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<strong>B-2</strong>")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("<strong>A-1</strong>"))));

        mockMvc.perform(get("/bills").param("status", "UNPAID").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Z-8")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("status-overdue")));
    }

    private org.springframework.test.web.servlet.ResultActions createBill(
        String apartment, String month, String amount, String description) throws Exception {
        return createBill(apartment, month, amount, description, LocalDate.now().plusDays(20));
    }

    private org.springframework.test.web.servlet.ResultActions createBill(
        String apartment, String month, String amount, String description, LocalDate dueDate) throws Exception {
        return createBill(apartment, month, amount, description, dueDate, "admin", "ADMIN");
    }

    private org.springframework.test.web.servlet.ResultActions createBill(
        String apartment,
        String month,
        String amount,
        String description,
        String username,
        String role) throws Exception {
        return createBill(apartment, month, amount, description, LocalDate.now().plusDays(20), username, role);
    }

    private org.springframework.test.web.servlet.ResultActions createBill(
        String apartment,
        String month,
        String amount,
        String description,
        LocalDate dueDate,
        String username,
        String role) throws Exception {
        return mockMvc.perform(post("/bills")
            .with(user(username).roles(role))
            .with(csrf())
            .param("apartmentNumber", apartment)
            .param("billingMonth", month)
            .param("dueDate", dueDate.toString())
            .param("items[0].description", description)
            .param("items[0].amount", amount));
    }

    private void recordPayment(Bill bill, String username, String amount) throws Exception {
        mockMvc.perform(post("/bills/{id}/payments", bill.getId())
                .with(user(username).roles("RESIDENT"))
                .with(csrf())
                .param("amount", amount))
            .andExpect(status().is3xxRedirection());
    }

    private void saveProfile(String username, String apartment) {
        ResidentProfile profile = new ResidentProfile();
        profile.setUsername(username);
        profile.setFullName(username);
        profile.setPhone("5551234567");
        profile.setApartmentNumber(apartment);
        profile.setEmail(username + "@example.com");
        profileRepository.save(profile);
    }
}
