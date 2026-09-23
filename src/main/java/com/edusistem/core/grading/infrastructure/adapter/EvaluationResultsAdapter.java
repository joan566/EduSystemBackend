package com.edusistem.core.grading.infrastructure.adapter;

import com.edusistem.core.grading.domain.outputports.EvaluationResultsPort;
import com.edusistem.core.grading.domain.vo.EvaluationResult;
import com.edusistem.core.shared.infrastructure.adapter.SqlSupport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Une en una sola consulta los resultados de las tres especializaciones de evaluación para los estudiantes activos
 * del grupo del teaching period. Una fila por (estudiante, evaluación).
 */
@Component
public class EvaluationResultsAdapter implements EvaluationResultsPort {

    private static final String RESULTS_SQL = """
            select sg.student_id, e.id, e.evaluation_category_id, es.score, e.maximum_score, false
            from teaching_periods tp
            join teaching_assignments ta on ta.id = tp.teaching_assignment_id
            join student_groups sg on sg.group_id = ta.group_id and sg.active
            join evaluations e on e.teaching_period_id = tp.id
            join exams x on x.evaluation_id = e.id
            left join exam_submissions es on es.exam_id = x.id and es.student_id = sg.student_id
            where tp.id = :tp
            union all
            select sg.student_id, e.id, e.evaluation_category_id, ag.grade, e.maximum_score, false
            from teaching_periods tp
            join teaching_assignments ta on ta.id = tp.teaching_assignment_id
            join student_groups sg on sg.group_id = ta.group_id and sg.active
            join evaluations e on e.teaching_period_id = tp.id
            join activities a on a.evaluation_id = e.id
            left join activity_grades ag on ag.activity_id = a.id and ag.student_id = sg.student_id
            where tp.id = :tp
            union all
            select sg.student_id, e.id, e.evaluation_category_id,
                   case ar.status when 'PRESENT' then e.maximum_score when 'ABSENT' then cast(0 as numeric(6,2)) end,
                   e.maximum_score,
                   (ar.status is null or ar.status = 'EXCUSED')
            from teaching_periods tp
            join teaching_assignments ta on ta.id = tp.teaching_assignment_id
            join student_groups sg on sg.group_id = ta.group_id and sg.active
            join evaluations e on e.teaching_period_id = tp.id
            join attendance_sessions s on s.evaluation_id = e.id
            left join attendance_records ar on ar.attendance_session_id = s.id and ar.student_id = sg.student_id
            where tp.id = :tp
            """;

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<EvaluationResult> findByTeachingPeriodId(Long teachingPeriodId) {
        List<Object[]> rows = em.createNativeQuery(RESULTS_SQL).setParameter("tp", teachingPeriodId).getResultList();
        return rows.stream().map(r -> new EvaluationResult(((Number) r[0]).longValue(), ((Number) r[1]).longValue(),
                ((Number) r[2]).longValue(), (BigDecimal) r[3], (BigDecimal) r[4], (Boolean) r[5])).toList();
    }

    @Override
    public boolean hasGradedExamResults(Long teachingPeriodId) {
        return SqlSupport.exists(em, """
                select 1 from exam_submissions es
                join exams x on x.id = es.exam_id
                join evaluations e on e.id = x.evaluation_id
                where e.teaching_period_id = :tp and es.final_grade is not null""", "tp", teachingPeriodId);
    }
}
