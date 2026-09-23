package com.edusistem.core.grading.infrastructure.repository;

import com.edusistem.core.grading.infrastructure.entity.GradingWeightEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGradingWeightRepository extends JpaRepository<GradingWeightEntity, Long> {

    List<GradingWeightEntity> findByGradingConfigurationIdOrderByIdAsc(Long gradingConfigurationId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from GradingWeightEntity w where w.gradingConfigurationId = :configurationId")
    void deleteByConfigurationId(@Param("configurationId") Long configurationId);
}
