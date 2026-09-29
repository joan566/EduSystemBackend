-- Liga cada registro de auditoría a su clase (actividad reciente) y guarda un nombre legible de la entidad.
-- Sin FK: la auditoría debe sobrevivir al borrado del periodo de enseñanza.
ALTER TABLE audit_logs ADD COLUMN teaching_period_id BIGINT;
ALTER TABLE audit_logs ADD COLUMN entity_label VARCHAR(255);

CREATE INDEX idx_audit_logs_teaching_period_created ON audit_logs (teaching_period_id, created_at DESC);

-- Relleno de los registros existentes cuya entidad aún existe.
UPDATE audit_logs al
SET teaching_period_id = e.teaching_period_id, entity_label = e.name
FROM exams x
JOIN evaluations e ON e.id = x.evaluation_id
WHERE al.entity_type IN ('Exam', 'AnswerSheet', 'QuestionBooklet') AND al.entity_id = x.id;

UPDATE audit_logs al
SET teaching_period_id = e.teaching_period_id,
    entity_label = LEFT(e.name || ' · ' || st.first_name || ' ' || st.last_name, 255)
FROM exam_submissions s
JOIN exams x ON x.id = s.exam_id
JOIN evaluations e ON e.id = x.evaluation_id
JOIN students st ON st.id = s.student_id
WHERE al.entity_type = 'ExamSubmission' AND al.entity_id = s.id;

UPDATE audit_logs al
SET teaching_period_id = e.teaching_period_id, entity_label = e.name
FROM activities a
JOIN evaluations e ON e.id = a.evaluation_id
WHERE al.entity_type = 'Activity' AND al.entity_id = a.id;

UPDATE audit_logs al
SET teaching_period_id = e.teaching_period_id, entity_label = e.name
FROM attendance_sessions sess
JOIN evaluations e ON e.id = sess.evaluation_id
WHERE al.entity_type = 'AttendanceSession' AND al.entity_id = sess.id;

UPDATE audit_logs al
SET teaching_period_id = e.teaching_period_id, entity_label = e.name
FROM evaluations e
WHERE al.entity_type = 'Evaluation' AND al.entity_id = e.id;

UPDATE audit_logs al
SET teaching_period_id = gc.teaching_period_id, entity_label = 'Escala ' || gs.name
FROM grading_configurations gc
JOIN grading_scales gs ON gs.id = gc.grading_scale_id
WHERE al.entity_type = 'GradingConfiguration' AND al.entity_id = gc.id;

UPDATE audit_logs al
SET teaching_period_id = sch.teaching_period_id,
    entity_label = CASE sch.day_of_week
                       WHEN 'MONDAY' THEN 'Lunes'
                       WHEN 'TUESDAY' THEN 'Martes'
                       WHEN 'WEDNESDAY' THEN 'Miércoles'
                       WHEN 'THURSDAY' THEN 'Jueves'
                       WHEN 'FRIDAY' THEN 'Viernes'
                       WHEN 'SATURDAY' THEN 'Sábado'
                       ELSE 'Domingo' END
                   || ' ' || TO_CHAR(sch.start_time, 'HH24:MI') || '-' || TO_CHAR(sch.end_time, 'HH24:MI')
FROM teaching_period_schedules sch
WHERE al.entity_type = 'TeachingPeriodSchedule' AND al.entity_id = sch.id;

-- Entidades cuyo id es el propio periodo de enseñanza (la clase y sus exportaciones).
UPDATE audit_logs al
SET teaching_period_id = tp.id, entity_label = LEFT(sub.name || ' · ' || g.name, 255)
FROM teaching_periods tp
JOIN teaching_assignments ta ON ta.id = tp.teaching_assignment_id
JOIN subjects sub ON sub.id = ta.subject_id
JOIN groups g ON g.id = ta.group_id
WHERE al.entity_type IN ('TeachingPeriod', 'Students', 'Grades', 'Attendance', 'TeachingPeriodFull')
  AND al.entity_id = tp.id;
