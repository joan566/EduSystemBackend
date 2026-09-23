package com.edusistem.core.auth.domain.outputports;

import com.edusistem.core.auth.domain.entity.PasswordResetToken;
import java.util.Optional;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    /** El token más reciente del usuario; es el único válido. */
    Optional<PasswordResetToken> findLatestByUserId(Long userId);
}
