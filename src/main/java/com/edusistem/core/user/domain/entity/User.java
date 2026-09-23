package com.edusistem.core.user.domain.entity;

import com.edusistem.core.authorization.domain.enums.RoleName;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private boolean active;
    /** Se incrementa para invalidar todos los JWT emitidos antes. */
    private int tokenVersion;
    @Builder.Default
    private Set<RoleName> roles = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void updateProfile(String firstName, String lastName) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public void revokeTokens() {
        this.tokenVersion++;
    }

    public void changePasswordHash(String newHash) {
        this.passwordHash = newHash;
    }
}
