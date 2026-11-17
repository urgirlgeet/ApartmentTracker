package com.apartment.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apartment.tracker.complaint.Complaint;
import com.apartment.tracker.complaint.ComplaintCodeService;
import com.apartment.tracker.complaint.ComplaintCategory;
import com.apartment.tracker.complaint.ComplaintHistory;
import com.apartment.tracker.complaint.ComplaintHistoryRepository;
import com.apartment.tracker.complaint.ComplaintRepository;
import com.apartment.tracker.complaint.ComplaintStatus;
import com.apartment.tracker.maintenance.MaintenanceStaff;
import com.apartment.tracker.maintenance.MaintenanceStaffRepository;
import com.apartment.tracker.profile.ResidentProfile;
import com.apartment.tracker.profile.ResidentProfileRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.uploads.directory=${java.io.tmpdir}/apartment-tracker-test-uploads")
@AutoConfigureMockMvc
class ComplaintWorkflowIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintCodeService complaintCodeService;

    @Autowired
    private ComplaintHistoryRepository historyRepository;

    @Autowired
    private MaintenanceStaffRepository staffRepository;

    @Autowired
    private ResidentProfileRepository profileRepository;

    @BeforeEach
    void cleanDatabase() {
        historyRepository.deleteAll();
        complaintRepository.deleteAll();
        staffRepository.deleteAll();
        profileRepository.deleteAll();
    }

    @Test
    void residentAdminAndMaintenanceCanCompleteAnImageComplaint() throws Exception {
        mockMvc.perform(post("/maintenance/staff")
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf())
                .param("name", "Taylor Technician")
                .param("specialization", "Plumbing")
                .param("phone", "9876543210"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/maintenance/staff"));

        MockMultipartFile photo = new MockMultipartFile(
            "image", "leak.png", "image/png", pngImage());
        mockMvc.perform(multipart("/complaints")
                .file(photo)
                .param("category", "PLUMBING")
                .param("description", "Water is leaking under the kitchen sink.")
                .with(user("resident").roles("RESIDENT"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/complaints"));

        Complaint complaint = complaintRepository.findAll().get(0);
        assertEquals("resident", complaint.getCreatedBy());
        assertEquals(ComplaintCategory.PLUMBING, complaint.getCategory());
        assertTrue(complaint.getComplaintCode().matches("P\\d+"));
        assertNotNull(complaint.getImageFilename());
        assertEquals(1, historyRepository.findByComplaintInOrderByOccurredAtAsc(java.util.List.of(complaint)).size());

        mockMvc.perform(get("/complaints").with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("/image")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Complaint submitted")));

        mockMvc.perform(get("/maintenance/staff").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/maintenance/staff").with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Taylor Technician")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SP")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Department")));

        mockMvc.perform(get("/dashboard").with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("My maintenance team")));
        mockMvc.perform(get("/dashboard").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Billing dashboard")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("My maintenance team"))));

        MaintenanceStaff staff = staffRepository.findAll().get(0);
        assertTrue(staff.getStaffCode().matches("SP\\d+"));
        mockMvc.perform(get("/complaints").with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<th>History</th>")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("History (1)")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("issue-cell")));
        mockMvc.perform(post("/complaints/{id}/assign", complaint.getId())
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf())
                .param("staffId", staff.getId().toString()))
            .andExpect(status().is3xxRedirection());

        complaint = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(ComplaintStatus.ASSIGNED, complaint.getStatus());
        assertEquals(staff.getId(), complaint.getAssignedStaff().getId());

        mockMvc.perform(get("/complaints").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Taylor Technician")));

        mockMvc.perform(get("/complaints").with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Start work")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SP")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Plumbing")));

        mockMvc.perform(get("/complaints/{id}/image", complaint.getId())
                .with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"));

        mockMvc.perform(post("/complaints/{id}/status", complaint.getId())
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals(ComplaintStatus.IN_PROGRESS,
            complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/complaints/{id}/status", complaint.getId())
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals(ComplaintStatus.RESOLVED,
            complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());

        mockMvc.perform(get("/complaints").with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Awaiting resident confirmation")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Close complaint"))));

        mockMvc.perform(get("/complaints").with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirm resolved")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Reopen")));

        mockMvc.perform(post("/complaints/{id}/confirm", complaint.getId())
                .with(user("resident").roles("RESIDENT"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals(ComplaintStatus.CLOSED,
            complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());
        mockMvc.perform(post("/complaints/{id}/reopen", complaint.getId())
                .with(user("resident").roles("RESIDENT"))
                .with(csrf())
                .param("reason", "The leak is still there."))
            .andExpect(status().is3xxRedirection());
        Complaint reopened = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertEquals(ComplaintStatus.REOPENED, reopened.getStatus());
        assertEquals("The leak is still there.", reopened.getResidentFeedback());
        mockMvc.perform(get("/complaints").with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("The leak is still there.")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Start work")));

        mockMvc.perform(post("/complaints/{id}/status", complaint.getId())
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals(ComplaintStatus.IN_PROGRESS,
            complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/complaints/{id}/status", complaint.getId())
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals(ComplaintStatus.RESOLVED,
            complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());

        mockMvc.perform(post("/complaints/{id}/confirm", complaint.getId())
                .with(user("resident").roles("RESIDENT"))
                .with(csrf()))
            .andExpect(status().is3xxRedirection());
        assertEquals(ComplaintStatus.CLOSED,
            complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());

        mockMvc.perform(get("/complaints")
                .with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Team complaints")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Closed by resident")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Awaiting resident confirmation"))));
        java.util.List<ComplaintHistory> history =
            historyRepository.findByComplaintInOrderByOccurredAtAsc(java.util.List.of(complaint));
        assertTrue(history.stream().anyMatch(event -> event.getDescription().contains("Assigned to Taylor Technician")));
        assertTrue(history.stream().anyMatch(event -> event.getDescription().contains("Status changed from ASSIGNED to IN PROGRESS")));
        assertTrue(history.stream().anyMatch(event -> event.getDescription().contains("Resident reopened the complaint")));
        assertTrue(history.stream().anyMatch(event -> event.getDescription().contains("Resident confirmed the resolution")));
    }

    @Test
    void residentsCannotAssignStaffOrViewAnotherResidentsPhoto() throws Exception {
        MaintenanceStaff staff = new MaintenanceStaff();
        staff.setName("Jordan Electrician");
        staff.setSpecialization("Electrical");
        staff.setHeadUsername("other-head");
        staff = staffRepository.save(staff);

        Complaint complaint = new Complaint();
        complaint.setCategory(ComplaintCategory.ELECTRICAL);
        complaint.setDescription("The corridor light is out.");
        complaint.setCreatedBy("another-resident");
        complaint.setAssignedStaff(staff);
        complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        complaint = complaintRepository.save(complaint);

        mockMvc.perform(post("/complaints/{id}/assign", complaint.getId())
                .with(user("resident").roles("RESIDENT"))
                .with(csrf())
                .param("staffId", staff.getId().toString()))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/complaints/{id}/image", complaint.getId())
                .with(user("resident").roles("RESIDENT")))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/complaints/{id}/reopen", complaint.getId())
                .with(user("resident").roles("RESIDENT"))
                .with(csrf())
                .param("reason", "Still broken"))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/complaints/{id}/status", complaint.getId())
                .with(user("maintenance").roles("MAINTENANCE_HEAD"))
                .with(csrf()))
            .andExpect(status().isForbidden());
    }

    @Test
    void everyComplaintCategoryRoutesOnlyToItsMatchingMaintenanceDepartment() throws Exception {
        for (ComplaintCategory category : ComplaintCategory.values()) {
            assertTrue(category.getDepartment().getComplaintCategories().contains(category));
        }

        Complaint plumbingComplaint = createComplaint(ComplaintCategory.PLUMBING, "Kitchen pipe leak");
        Complaint electricalComplaint = createComplaint(ComplaintCategory.ELECTRICAL, "Hall light is broken");
        assertTrue(plumbingComplaint.getComplaintCode().matches("P\\d+"));
        assertTrue(electricalComplaint.getComplaintCode().matches("E\\d+"));
        assertNotEquals(plumbingComplaint.getComplaintCode(), electricalComplaint.getComplaintCode());
        MaintenanceStaff electrician = new MaintenanceStaff();
        electrician.setName("Avery Electrician");
        electrician.setSpecialization("Electrical");
        electrician.setHeadUsername("electrical");
        electrician = staffRepository.save(electrician);

        mockMvc.perform(get("/complaints").with(user("electrical").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Hall light is broken")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Kitchen pipe leak"))));

        mockMvc.perform(get("/complaints")
                .param("q", plumbingComplaint.getComplaintCode())
                .with(user("electrical").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("Kitchen pipe leak"))));

        mockMvc.perform(get("/complaints")
                .param("q", plumbingComplaint.getComplaintCode())
                .with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Kitchen pipe leak")));

        mockMvc.perform(post("/complaints/{id}/assign", plumbingComplaint.getId())
                .with(user("electrical").roles("MAINTENANCE_HEAD"))
                .with(csrf())
                .param("staffId", electrician.getId().toString()))
            .andExpect(status().is3xxRedirection());

        assertEquals(null, complaintRepository.findById(plumbingComplaint.getId()).orElseThrow().getAssignedStaff());
    }

    @Test
    void staffDirectoryShowsDepartmentSpecificIdsAcrossDepartments() throws Exception {
        mockMvc.perform(post("/maintenance/staff")
                .with(user("plumbing").roles("MAINTENANCE_HEAD"))
                .with(csrf())
                .param("name", "Taylor Technician")
                .param("specialization", "Plumbing"))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/maintenance/staff")
                .with(user("electrical").roles("MAINTENANCE_HEAD"))
                .with(csrf())
                .param("name", "Avery Electrician")
                .param("specialization", "Electrical"))
            .andExpect(status().is3xxRedirection());

        assertTrue(staffRepository.findAll().stream()
            .anyMatch(staff -> staff.getStaffCode().matches("SP\\d+")));
        assertTrue(staffRepository.findAll().stream()
            .anyMatch(staff -> staff.getStaffCode().matches("SE\\d+")));

        mockMvc.perform(get("/maintenance/staff")
                .with(user("plumbing").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Avery Electrician")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Electrical")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("SE")));
    }

    private Complaint createComplaint(ComplaintCategory category, String description) {
        Complaint complaint = new Complaint();
        complaint.setCategory(category);
        complaint.setComplaintCode(complaintCodeService.nextCode(category));
        complaint.setDescription(description);
        complaint.setCreatedBy("resident");
        return complaintRepository.save(complaint);
    }

    @Test
    void residentCanSaveAndViewTheirOwnProfileOnly() throws Exception {
        mockMvc.perform(get("/profile").with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("My profile")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Edit profile")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("name=\"fullName\""))));

        mockMvc.perform(get("/profile/edit").with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"fullName\"")));

        ResidentProfile existingProfile = profileRepository.findById("resident").orElseThrow();
        existingProfile.setApartmentNumber("A-1");
        profileRepository.save(existingProfile);
        mockMvc.perform(post("/profile")
                .with(user("resident").roles("RESIDENT"))
                .with(csrf())
                .param("fullName", "Sam Resident")
                .param("phone", "+1 555 123 4567")
                .param("apartmentBlock", "B")
                .param("apartmentUnit", "2")
                .param("email", "sam@example.com")
                .param("emergencyContactName", "Alex Resident")
                .param("emergencyContactPhone", "5551234568"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"));

        ResidentProfile profile = profileRepository.findById("resident").orElseThrow();
        assertEquals("Sam Resident", profile.getFullName());
        assertEquals("A-1", profile.getApartmentNumber());
        assertEquals("sam@example.com", profile.getEmail());

        mockMvc.perform(get("/profile").with(user("maintenance").roles("MAINTENANCE_HEAD")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/profile").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard").with(user("resident").roles("RESIDENT")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("My profile")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("View my profile"))));
    }

    private byte[] pngImage() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
