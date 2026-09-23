package com.edusistem.core.academic.infrastructure.repository;

import com.edusistem.core.academic.infrastructure.entity.GradeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataGradeRepository extends JpaRepository<GradeEntity, Long> {

    Optional<GradeEntity> findByName(String name);

    List<GradeEntity> findAllByOrderByNameAsc();
}
