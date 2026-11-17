package com.apartment.tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.apartment.tracker.account.AccountRole;
import com.apartment.tracker.account.UserAccount;
import com.apartment.tracker.account.UserAccountRepository;
import com.apartment.tracker.maintenance.MaintenanceDepartment;
import com.apartment.tracker.profile.ResidentProfile;
import com.apartment.tracker.profile.ResidentProfileRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class ResidentSignupIntegrationTest {
    private static final String USERNAME = "new-resident";
    private static final String EMAIL = "new-resident@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository accountRepository;

    @Autowired
    private ResidentProfileRepository profileRepository;

    @BeforeEach
    void cleanTestAccount() {
        profileRepository.deleteById(USERNAME);
        accountRepository.deleteById(USERNAME);
    }

    @Test
    void registrationStoresHashedCredentialsAndProfileAndAllowsSignIn() throws Exception {
        mockMvc.perform(get("/signup"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Create your account")));

        mockMvc.perform(post("/signup")
                .with(csrf())
                .param("username", USERNAME)
                .param("password", "resident-secure-123")
                .param("confirmPassword", "resident-secure-123")
                .param("profile.fullName", "Sam Resident")
                .param("profile.phone", "+1 555 123 4567")
                .param("profile.apartmentBlock", "A")
                .param("profile.apartmentUnit", "1")
                .param("profile.email", EMAIL)
                .param("profile.emergencyContactName", "Alex Resident")
                .param("profile.emergencyContactPhone", "5551234568"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));

        UserAccount account = accountRepository.findById(USERNAME).orElseThrow();
        assertEquals(AccountRole.RESIDENT.name(), account.getRoleName());
        assertNotEquals("resident-secure-123", account.getPassword());
        assertTrue(account.getPassword().startsWith("$2a$"));

        ResidentProfile profile = profileRepository.findById(USERNAME).orElseThrow();
        assertEquals("Sam Resident", profile.getFullName());
        assertEquals("+1 555 123 4567", profile.getPhone());
        assertEquals("A-1", profile.getApartmentNumber());
        assertEquals(EMAIL, profile.getEmail());

        MvcResult loginResult = mockMvc.perform(post("/login")
                .with(csrf())
                .param("username", USERNAME)
                .param("password", "resident-secure-123"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        mockMvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Hello, Sam Resident")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("A-1")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Maintenance bills")));

        mockMvc.perform(get("/profile").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(EMAIL)))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("name=\"fullName\""))));
        mockMvc.perform(get("/signup"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose a block (A-Z)")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose an apartment (1-8)")));

        mockMvc.perform(get("/profile/edit").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"fullName\"")));
        mockMvc.perform(get("/profile/edit").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Registered apartment")))
            .andExpect(content().string(org.hamcrest.Matchers.not(
                org.hamcrest.Matchers.containsString("name=\"apartmentBlock\""))));
    }

    @Test
    void rejectsDuplicateUsernamesEmailsAndMismatchedPasswords() throws Exception {
        register(USERNAME, EMAIL, "resident-secure-123", "resident-secure-123")
            .andExpect(status().is3xxRedirection());

        register("another-resident", EMAIL.toUpperCase(), "resident-secure-123", "resident-secure-123")
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("already uses that email")));

        register(USERNAME, "another@example.com", "resident-secure-123", "different-password")
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Passwords do not match.")));
    }

    @Test
    void departmentDemoAccountsAreSeededAndCanSignIn() throws Exception {
        Map<String, String> accounts = Map.of(
            "electrical", "electrical123",
            "plumbing", "plumbing123",
            "water", "water12345",
            "cleaning", "cleaning123",
            "lift", "lift12345",
            "security", "security123",
            "general", "general123");

        for (Map.Entry<String, String> demoAccount : accounts.entrySet()) {
            UserAccount account = accountRepository.findById(demoAccount.getKey()).orElseThrow();
            assertEquals(AccountRole.MAINTENANCE_HEAD.name(), account.getRoleName());
            assertEquals(MaintenanceDepartment.valueOf(demoAccount.getKey().toUpperCase()), account.getDepartment());

            mockMvc.perform(post("/login")
                    .with(csrf())
                    .param("username", demoAccount.getKey())
                    .param("password", demoAccount.getValue()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
        }

        assertTrue(accountRepository.findById("another-resident").isEmpty());
    }

    @Test
    void rejectsPasswordsThatExceedTheEncoderByteLimit() throws Exception {
        String longMultibytePassword = "é".repeat(37);
        register("long-password-user", "long-password@example.com",
                longMultibytePassword, longMultibytePassword)
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("72 bytes or less")));
        assertTrue(accountRepository.findById("long-password-user").isEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions register(
        String username,
        String email,
        String password,
        String confirmation) throws Exception {
        return mockMvc.perform(post("/signup")
            .with(csrf())
            .param("username", username)
            .param("password", password)
            .param("confirmPassword", confirmation)
            .param("profile.fullName", "Test Resident")
            .param("profile.phone", "5551234567")
            .param("profile.apartmentBlock", "A")
            .param("profile.apartmentUnit", "1")
            .param("profile.email", email));
    }
}
