package com.notsureance.claims.user;

import com.notsureance.claims.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "entra_object_id", unique = true)
    private String entraObjectId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    protected AppUser() {
    }

    public AppUser(String entraObjectId, String email, String fullName, Role role) {
        this.entraObjectId = entraObjectId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    void syncFromDirectory(String objectId, String name, Role directoryRole) {
        this.entraObjectId = objectId;
        this.fullName = name;
        this.role = directoryRole;
    }

    public String getEmail() {
        return email;
    }

    public String getEntraObjectId() {
        return entraObjectId;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }
}
