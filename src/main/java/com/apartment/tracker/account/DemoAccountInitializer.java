package com.apartment.tracker.account;

import com.apartment.tracker.maintenance.MaintenanceDepartment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class DemoAccountInitializer implements ApplicationRunner {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String maintenanceUsername;
    private final String maintenancePassword;
    private final String electricalPassword;
    private final String plumbingPassword;
    private final String waterPassword;
    private final String cleaningPassword;
    private final String liftPassword;
    private final String securityPassword;
    private final String generalPassword;

    public DemoAccountInitializer(
        UserAccountRepository repository,
        PasswordEncoder passwordEncoder,
        @Value("${app.demo.admin.username:admin}") String adminUsername,
        @Value("${app.demo.admin.password:admin123}") String adminPassword,
        @Value("${app.demo.maintenance.username:maintenance}") String maintenanceUsername,
        @Value("${app.demo.maintenance.password:maintenance123}") String maintenancePassword,
        @Value("${app.demo.departments.electrical.password:electrical123}") String electricalPassword,
        @Value("${app.demo.departments.plumbing.password:plumbing123}") String plumbingPassword,
        @Value("${app.demo.departments.water.password:water12345}") String waterPassword,
        @Value("${app.demo.departments.cleaning.password:cleaning123}") String cleaningPassword,
        @Value("${app.demo.departments.lift.password:lift12345}") String liftPassword,
        @Value("${app.demo.departments.security.password:security123}") String securityPassword,
        @Value("${app.demo.departments.general.password:general123}") String generalPassword) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.maintenanceUsername = maintenanceUsername;
        this.maintenancePassword = maintenancePassword;
        this.electricalPassword = electricalPassword;
        this.plumbingPassword = plumbingPassword;
        this.waterPassword = waterPassword;
        this.cleaningPassword = cleaningPassword;
        this.liftPassword = liftPassword;
        this.securityPassword = securityPassword;
        this.generalPassword = generalPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfMissing(adminUsername, adminPassword, AccountRole.ADMIN);
        createIfMissing(maintenanceUsername, maintenancePassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.GENERAL);
        createIfMissing("electrical", electricalPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.ELECTRICAL);
        createIfMissing("plumbing", plumbingPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.PLUMBING);
        createIfMissing("water", waterPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.WATER);
        createIfMissing("cleaning", cleaningPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.CLEANING);
        createIfMissing("lift", liftPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.LIFT);
        createIfMissing("security", securityPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.SECURITY);
        createIfMissing("general", generalPassword, AccountRole.MAINTENANCE_HEAD, MaintenanceDepartment.GENERAL);
    }

    private void createIfMissing(String username, String password, AccountRole role) {
        createIfMissing(username, password, role, null);
    }

    private void createIfMissing(
        String username,
        String password,
        AccountRole role,
        MaintenanceDepartment department) {
        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        UserAccount existing = repository.findById(normalizedUsername).orElse(null);
        if (existing == null) {
            repository.save(new UserAccount(
                normalizedUsername,
                passwordEncoder.encode(password),
                role,
                department));
        } else if (role == AccountRole.MAINTENANCE_HEAD && existing.getDepartment() == null) {
            existing.setDepartment(department);
            repository.save(existing);
        }
    }
}
