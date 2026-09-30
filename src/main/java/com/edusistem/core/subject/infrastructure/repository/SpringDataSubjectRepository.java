package com.edusistem.core.subject.infrastructure.repository;

import com.edusistem.core.subject.infrastructure.entity.SubjectEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSubjectRepository extends JpaRepository<SubjectEntity, Long> {

    Optional<SubjectEntity> findByTeacherIdAndName(Long teacherId, String name);

    @Query("select s from SubjectEntity s where s.teacherId = :teacherId and lower(s.name) like :pattern")
    Page<SubjectEntity> search(@Param("teacherId") Long teacherId, @Param("pattern") String pattern, Pageable pageable);
}
