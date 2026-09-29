package com.edusistem.core.gradebook.infrastructure.adapter;

import com.edusistem.core.gradebook.domain.outputports.GradebookQueryPort;
import com.edusistem.core.gradebook.domain.vo.EvaluationType;
import com.edusistem.core.gradebook.domain.vo.GradebookRow;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Las evaluaciones de un teaching period con el resultado de un estudiante, uniendo las tres especializaciones con
 * las mismas reglas que {@code EvaluationResultsAdapter} (la nota del periodo): examen = puntaje del envío, actividad
 * = nota registrada, asistencia = máximo si presente, 0 si ausente y no cuenta con excusa o sin registro.
 */
@Component
public class GradebookQueryAdapter implements GradebookQueryPort {

    private static final String ROWS_SQL = """
            select e.id, e.name, e.description, e.evaluation_date, e.maximum_score, e.evaluation_category_id,
                   case when x.id is not null then 'EXAM' when a.id is not null then 'ACTIVITY' else 'ATTENDANCE' end,
                   a.activity_type,
                   case when x.id is not null then es.score
                        when a.id is not null then ag.grade
                        when ar.status = 'PRESENT' then e.maximum_score
                        when ar.status = 'ABSENT' then cast(0 as numeric(6,2)) end,
                   (s.id is not null and (ar.status is null or ar.status = 'EXCUSED')),
                   ag.comment,
                   coalesce(ag.graded_at, es.processed_at),
                   a.id, x.id, es.id
            from evaluations e
            left join exams x on x.evaluation_id = e.id
            left join exam_submissions es on es.exam_id = x.id and es.student_id = :student
            left join activities a on a.evaluation_id = e.id
            left join activity_grades ag on ag.activity_id = a.id and ag.student_id = :student
            left join attendance_sessions s on s.evaluation_id = e.id
            left join attendance_records ar on ar.attendance_session_id = s.id and ar.student_id = :student
            where e.teaching_period_id = :tp and (x.id is not null or a.id is not null or s.id is not null)
            order by e.evaluation_date nulls last, e.id
            """;

    private static final String KIND_SQL = """
            select e.id, e.teaching_period_id,
                   case when x.id is not null then 'EXAM' when a.id is not null then 'ACTIVITY' else 'ATTENDANCE' end,
                   e.maximum_score, a.id
            from evaluations e
            left join exams x on x.evaluation_id = e.id
            left join activities a on a.evaluation_id = e.id
            left join attendance_sessions s on s.evaluation_id = e.id
            where e.id = :id and (x.id is not null or a.id is not null or s.id is not null)
            """;

    @PersistenceContext
    private EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public List<GradebookRow> findRows(Long teachingPeriodId, Long studentId) {
        List<Object[]> rows = em.createNativeQuery(ROWS_SQL).setParameter("tp", teachingPeriodId)
                .setParameter("student", studentId).getResultList();
        return rows.stream().map(r -> new GradebookRow(id(r[0]), (String) r[1], (String) r[2], time(r[3]),
                (BigDecimal) r[4], id(r[5]), EvaluationType.valueOf((String) r[6]), (String) r[7], (BigDecimal) r[8],
                Boolean.TRUE.equals(r[9]), (String) r[10], time(r[11]), id(r[12]), id(r[13]), id(r[14]))).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<EvaluationKind> findKind(Long evaluationId) {
        List<Object[]> rows = em.createNativeQuery(KIND_SQL).setParameter("id", evaluationId).getResultList();
        return rows.stream().findFirst().map(r -> new EvaluationKind(id(r[0]), id(r[1]),
                EvaluationType.valueOf((String) r[2]), (BigDecimal) r[3], id(r[4])));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Set<Long> evaluationIdsWithRubric(Long teachingPeriodId) {
        List<Number> ids = em.createNativeQuery("""
                select distinct c.evaluation_id from rubric_criteria c
                join evaluations e on e.id = c.evaluation_id
                where e.teaching_period_id = :tp""").setParameter("tp", teachingPeriodId).getResultList();
        Set<Long> result = new HashSet<>();
        ids.forEach(n -> result.add(n.longValue()));
        return result;
    }

    private static Long id(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static LocalDateTime time(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof Timestamp t ? t.toLocalDateTime() : (LocalDateTime) value;
    }
}
