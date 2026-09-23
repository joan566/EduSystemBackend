package com.edusistem.core.student.infrastructure.repository;

import com.edusistem.core.student.infrastructure.entity.StudentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataStudentRepository extends JpaRepository<StudentEntity, Long> {

    Optional<StudentEntity> findByStudentCode(String studentCode);

    Optional<StudentEntity> findByIdentificationNumber(String identificationNumber);

    @Query(value = """
            select s from StudentEntity s
            where exists (
                select 1 from StudentGroupEntity sg join TeachingAssignmentEntity ta on ta.groupId = sg.groupId
                where sg.studentId = s.id and ta.teacherId = :teacherId
                  and (:groupId is null or sg.groupId = :groupId))
              and (lower(s.firstName) like :pattern or lower(s.lastName) like :pattern
                   or lower(s.studentCode) like :pattern or lower(coalesce(s.identificationNumber, '')) like :pattern)
            order by s.lastName, s.firstName, s.id""",
            countQuery = """
                    select count(s) from StudentEntity s
                    where exists (
                        select 1 from StudentGroupEntity sg join TeachingAssignmentEntity ta on ta.groupId = sg.groupId
                        where sg.studentId = s.id and ta.teacherId = :teacherId
                          and (:groupId is null or sg.groupId = :groupId))
                      and (lower(s.firstName) like :pattern or lower(s.lastName) like :pattern
                           or lower(s.studentCode) like :pattern or lower(coalesce(s.identificationNumber, '')) like :pattern)""")
    Page<StudentEntity> searchByTeacher(@Param("teacherId") Long teacherId, @Param("groupId") Long groupId,
                                        @Param("pattern") String pattern, Pageable pageable);

    @Query("""
            select s from StudentEntity s join StudentGroupEntity sg on sg.studentId = s.id
            where sg.groupId = :groupId and sg.active = true
            order by s.lastName, s.firstName, s.id""")
    List<StudentEntity> findActiveByGroupId(@Param("groupId") Long groupId);
}
