package com.edusistem.core.auth.domain.entity;

import java.time.LocalDateTime;
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
public class PasswordResetToken {
    private Long id;
    private Long userId;
    private String codeHash;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private int attempts;
    private LocalDateTime createdAt;

    public boolean isUsable(LocalDateTime now, int maxAttempts) {
        return usedAt == null && now.isBefore(expiresAt) && attempts < maxAttempts;
    }
}
