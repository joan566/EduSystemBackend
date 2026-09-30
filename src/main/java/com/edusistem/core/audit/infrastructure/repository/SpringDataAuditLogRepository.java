package com.edusistem.core.audit.infrastructure.repository;

import com.edusistem.core.audit.infrastructure.entity.AuditLogEntity;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAuditLogRepository
        extends JpaRepository<AuditLogEntity, Long>, JpaSpecificationExecutor<AuditLogEntity> {

    @Modifying
    @Query("delete from AuditLogEntity a where a.createdAt < :limit")
    int deleteCreatedBefore(@Param("limit") LocalDateTime limit);

    /** Entradas sin usuario (logins fallidos con correos que no existen, bloqueos...). */
    @Modifying
    @Query("delete from AuditLogEntity a where a.userId is null and a.createdAt < :limit")
    int deleteAnonymousCreatedBefore(@Param("limit") LocalDateTime limit);
}
