package com.edusistem.core.academic.infrastructure.adapter;

import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.domain.outputports.TeachingPeriodRepositoryPort;
import com.edusistem.core.academic.domain.vo.TeachingPeriodSummary;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.academic.infrastructure.mapper.AcademicMapper;
import com.edusistem.core.academic.infrastructure.repository.SpringDataTeachingPeriodRepository;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.shared.infrastructure.adapter.PageMapper;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TeachingPeriodRepositoryAdapter implements TeachingPeriodRepositoryPort {

    private final SpringDataTeachingPeriodRepository repository;
    private final AcademicMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public TeachingPeriodRepositoryAdapter(SpringDataTeachingPeriodRepository repository, AcademicMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public TeachingPeriod save(TeachingPeriod teachingPeriod) {
        return mapper.toDomain(repository.save(mapper.toEntity(teachingPeriod)));
    }

    @Override
    public Optional<TeachingPeriod> findById(Long id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<TeachingPeriodView> findViewById(Long id) {
        return repository.findViewById(id);
    }

    @Override
    public Optional<TeachingPeriod> findByTeachingAssignmentIdAndAcademicPeriodId(Long teachingAssignmentId,
                                                                                  Long academicPeriodId) {
        return repository.findByTeachingAssignmentIdAndAcademicPeriodId(teachingAssignmentId, academicPeriodId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<TeachingPeriodView> findViewsByTeacherId(Long teacherId, Long teachingAssignmentId,
                                                               Long academicPeriodId, PageQuery page) {
        return PageMapper.toResult(repository.findViewsByTeacher(teacherId, teachingAssignmentId, academicPeriodId,
                PageMapper.pageable(page)), v -> v);
    }

    /** Sólo cuentan las notas de estudiantes activos del grupo, igual que las esperadas. */
    @Override
    public TeachingPeriodSummary summarize(Long teachingPeriodId) {
        Object[] row = (Object[]) em.createNativeQuery("""
                with active as (
                    select sg.student_id from student_groups sg
                    join teaching_assignments ta on ta.group_id = sg.group_id
                    join teaching_periods tp on tp.teaching_assignment_id = ta.id
                    where tp.id = :id and sg.active = true),
                period_activities as (
                    select a.id from activities a join evaluations e on e.id = a.evaluation_id
                    where e.teaching_period_id = :id),
                period_exams as (
                    select x.id from exams x join evaluations e on e.id = x.evaluation_id
                    where e.teaching_period_id = :id)
                select (select count(*) from active),
                       (select count(*) from period_activities),
                       (select count(*) from period_exams),
                       (select count(*) from activity_grades ag
                        where ag.activity_id in (select id from period_activities)
                          and ag.student_id in (select student_id from active))
                     + (select count(*) from exam_submissions s
                        where s.exam_id in (select id from period_exams) and s.final_grade is not null
                          and s.student_id in (select student_id from active))
                """).setParameter("id", teachingPeriodId).getSingleResult();
        return TeachingPeriodSummary.of(teachingPeriodId, ((Number) row[0]).longValue(), ((Number) row[1]).longValue(),
                ((Number) row[2]).longValue(), ((Number) row[3]).longValue());
    }

    @Override
    public boolean hasDependents(Long teachingPeriodId) {
        return SqlSupport.exists(em, "select 1 from evaluations where teaching_period_id = :id", "id", teachingPeriodId)
                || SqlSupport.exists(em, "select 1 from grading_configurations where teaching_period_id = :id",
                "id", teachingPeriodId);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
