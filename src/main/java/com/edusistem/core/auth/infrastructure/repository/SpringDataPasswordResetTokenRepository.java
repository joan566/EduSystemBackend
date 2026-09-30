package com.edusistem.core.auth.infrastructure.repository;

import com.edusistem.core.auth.infrastructure.entity.PasswordResetTokenEntity;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {

    Optional<PasswordResetTokenEntity> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    @Modifying
    @Query("delete from PasswordResetTokenEntity t where t.expiresAt < :limit")
    int deleteExpiredBefore(@Param("limit") LocalDateTime limit);
}
