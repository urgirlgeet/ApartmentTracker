package com.apartment.tracker.account;

import com.apartment.tracker.maintenance.MaintenanceDepartment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Table(name = "user_accounts")
public class UserAccount implements UserDetails {
    @Id
    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private MaintenanceDepartment department;

    protected UserAccount() {}

    public UserAccount(String username, String passwordHash, AccountRole role) {
        this(username, passwordHash, role, null);
    }

    public UserAccount(
        String username,
        String passwordHash,
        AccountRole role,
        MaintenanceDepartment department) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.department = department;
    }

    public String getRoleName() {
        return role.name();
    }

    public MaintenanceDepartment getDepartment() {
        return department;
    }

    public void setDepartment(MaintenanceDepartment department) {
        this.department = department;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
