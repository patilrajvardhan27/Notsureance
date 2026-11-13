package com.notsureance.claims.user;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private final UserRepository users;

    public UserProvisioningService(UserRepository users) {
        this.users = users;
    }

    // Entra decides who someone is and which role they hold; the local row only keeps ownership links stable.
    @Transactional
    public AppUser sync(String objectId, String email, String name, Role role) {
        AppUser user = users.findByEntraObjectId(objectId)
                .or(() -> users.findByEmailIgnoreCase(email))
                .orElseGet(() -> users.save(new AppUser(objectId, email, name, role)));
        boolean changed = !Objects.equals(user.getEntraObjectId(), objectId)
                || !Objects.equals(user.getFullName(), name) || user.getRole() != role;
        if (changed) {
            user.syncFromDirectory(objectId, name, role);
        }
        return user;
    }
}
