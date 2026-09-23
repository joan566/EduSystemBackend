package com.edusistem.core.auth.infrastructure.repository;

import com.edusistem.core.auth.infrastructure.entity.PasswordResetTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
