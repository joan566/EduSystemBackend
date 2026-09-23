package com.edusistem.core.user.infrastructure.repository;

import com.edusistem.core.user.infrastructure.entity.UserEntity;
import com.edusistem.core.user.domain.vo.AuthState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("select new com.edusistem.core.user.domain.vo.AuthState(u.active, u.tokenVersion) from UserEntity u where u.id = :id")
    Optional<AuthState> findAuthState(@Param("id") Long id);
}
