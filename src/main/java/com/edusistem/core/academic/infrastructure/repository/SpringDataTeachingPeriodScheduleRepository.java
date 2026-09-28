package com.edusistem.core.academic.infrastructure.repository;

import com.edusistem.core.academic.domain.vo.ScheduledClassView;
import com.edusistem.core.academic.infrastructure.entity.TeachingPeriodScheduleEntity;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTeachingPeriodScheduleRepository extends JpaRepository<TeachingPeriodScheduleEntity, Long> {

    String VIEW_SELECT = """
            select new com.edusistem.core.academic.domain.vo.ScheduledClassView(s.id, s.teachingPeriodId, s.dayOfWeek,
                s.startTime, s.endTime, s.room, ta.subjectId, sub.name, ta.groupId, gr.name, g.name, tp.academicPeriodId,
                ap.name, ap.startDate, ap.endDate)
            from TeachingPeriodScheduleEntity s
            join TeachingPeriodEntity tp on tp.id = s.teachingPeriodId
            join TeachingAssignmentEntity ta on ta.id = tp.teachingAssignmentId
            join GroupEntity g on g.id = ta.groupId
            join GradeEntity gr on gr.id = g.gradeId
            join SubjectEntity sub on sub.id = ta.subjectId
            join AcademicPeriodEntity ap on ap.id = tp.academicPeriodId
            """;

    @Query(VIEW_SELECT + " where s.id = :id")
    Optional<ScheduledClassView> findViewById(@Param("id") Long id);

    @Query(VIEW_SELECT + " where s.teachingPeriodId = :teachingPeriodId")
    List<ScheduledClassView> findViewsByTeachingPeriodId(@Param("teachingPeriodId") Long teachingPeriodId);

    @Query(VIEW_SELECT + """
            where ta.teacherId = :teacherId and ta.active = true
              and s.dayOfWeek = :day and s.startTime < :endTime and s.endTime > :startTime
              and ap.startDate <= :periodEnd and ap.endDate >= :periodStart
              and s.id <> :excludeId
            order by s.startTime""")
    List<ScheduledClassView> findConflicts(@Param("teacherId") Long teacherId, @Param("day") DayOfWeek day,
                                           @Param("startTime") LocalTime startTime,
                                           @Param("endTime") LocalTime endTime,
                                           @Param("periodStart") LocalDate periodStart,
                                           @Param("periodEnd") LocalDate periodEnd,
                                           @Param("excludeId") Long excludeId);

    @Query(VIEW_SELECT + """
            where ta.teacherId = :teacherId and ta.active = true
              and ap.startDate <= :to and ap.endDate >= :from""")
    List<ScheduledClassView> findViewsByTeacher(@Param("teacherId") Long teacherId, @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);
}
