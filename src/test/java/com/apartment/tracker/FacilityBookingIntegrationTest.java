package com.apartment.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apartment.tracker.booking.Facility;
import com.apartment.tracker.booking.FacilityBookingRepository;
import com.apartment.tracker.booking.FacilityBookingStatus;
import com.apartment.tracker.booking.FacilityRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FacilityBookingIntegrationTest {
    private static final DateTimeFormatter INPUT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FacilityRepository facilityRepository;

    @Autowired
    private FacilityBookingRepository bookingRepository;

    @BeforeEach
    void cleanBookings() {
        bookingRepository.deleteAll();
    }

    @Test
    void preventsOverlapsButAllowsAdjacentSlots() throws Exception {
        Facility gym = getFacility("Gym");
        LocalDateTime start = LocalDateTime.now().plusDays(2).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(1);

        book("resident-one", gym, start, end)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/bookings?created"));

        book("resident-two", gym, start.plusMinutes(30), end.plusMinutes(30))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("overlaps an existing booking")));

        book("resident-two", gym, end, end.plusHours(1))
            .andExpect(status().is3xxRedirection());

        assertEquals(2, bookingRepository.count());
    }

    @Test
    void residentsCanOnlySeeAndCancelTheirOwnBookings() throws Exception {
        Facility pool = getFacility("Swimming Pool");
        LocalDateTime start = LocalDateTime.now().plusDays(4).withSecond(0).withNano(0);
        book("resident-one", pool, start, start.plusHours(1))
            .andExpect(status().is3xxRedirection());
        Long bookingId = bookingRepository.findAll().get(0).getId();

        mockMvc.perform(get("/bookings").with(user("resident-two").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("resident-one"))));

        mockMvc.perform(post("/bookings/{id}/cancel", bookingId)
                .with(user("resident-two").roles("RESIDENT"))
                .with(csrf()))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/bookings/{id}/cancel", bookingId)
                .with(user("resident-one").roles("RESIDENT"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());

        assertEquals(FacilityBookingStatus.CANCELLED, bookingRepository.findById(bookingId).orElseThrow().getStatus());
    }

    @Test
    void bookingPageIsLimitedToResidentsAndAdministrators() throws Exception {
        mockMvc.perform(get("/bookings").with(user("maintenance").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/bookings").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("All facility bookings")));

        mockMvc.perform(post("/bookings")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    private Facility getFacility(String name) {
        return facilityRepository.findAllByOrderByNameAsc().stream()
            .filter(facility -> facility.getName().equals(name))
            .findFirst()
            .orElseThrow();
    }

    private org.springframework.test.web.servlet.ResultActions book(
        String username,
        Facility facility,
        LocalDateTime start,
        LocalDateTime end) throws Exception {
        return mockMvc.perform(post("/bookings")
            .with(user(username).roles("RESIDENT"))
            .with(csrf())
            .param("facilityId", facility.getId().toString())
            .param("startAt", start.format(INPUT_FORMAT))
            .param("endAt", end.format(INPUT_FORMAT)));
    }
}
