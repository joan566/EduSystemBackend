-- Datos de referencia: roles, categorías de evaluación y escalas de calificación comunes.
INSERT INTO roles (name, description, created_at) VALUES
    ('TEACHER', 'Profesor: gestiona sus grupos, evaluaciones y calificaciones', now()),
    ('ADMIN', 'Administrador (reservado para futuras instituciones)', now());

INSERT INTO evaluation_categories (name, description, created_at) VALUES
    ('EXAMS', 'Exámenes de selección múltiple', now()),
    ('ACTIVITIES', 'Actividades calificadas manualmente', now()),
    ('ATTENDANCE', 'Asistencia', now());

INSERT INTO grading_scales (name, minimum_value, maximum_value, created_at) VALUES
    ('Colombian 0-5', 0, 5, now()),
    ('Scale 0-10', 0, 10, now()),
    ('Percentage 0-100', 0, 100, now());
