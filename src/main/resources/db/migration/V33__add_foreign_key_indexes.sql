-- Índices de rendimiento sobre claves foráneas (PostgreSQL no los crea automáticamente).
CREATE INDEX idx_student_groups_group ON student_groups (group_id);
CREATE INDEX idx_teaching_assignments_group ON teaching_assignments (group_id);
CREATE INDEX idx_teaching_assignments_subject ON teaching_assignments (subject_id);
CREATE INDEX idx_teaching_periods_period ON teaching_periods (academic_period_id);
CREATE INDEX idx_evaluations_teaching_period ON evaluations (teaching_period_id);
CREATE INDEX idx_evaluations_category ON evaluations (evaluation_category_id);
CREATE INDEX idx_exam_submissions_student ON exam_submissions (student_id);
CREATE INDEX idx_exam_answers_question ON exam_answers (question_id);
CREATE INDEX idx_activity_grades_student ON activity_grades (student_id);
CREATE INDEX idx_attendance_records_student ON attendance_records (student_id);
CREATE INDEX idx_import_batches_user ON import_batches (user_id, created_at DESC);
CREATE INDEX idx_grading_configurations_scale ON grading_configurations (grading_scale_id);
