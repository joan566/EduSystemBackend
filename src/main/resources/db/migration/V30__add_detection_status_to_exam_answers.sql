-- Fuera del DBML: permite distinguir MARKED / EMPTY / MULTIPLE_MARK / REVIEW_REQUIRED / MANUAL por respuesta.
ALTER TABLE exam_answers ADD COLUMN detection_status VARCHAR(20) NOT NULL DEFAULT 'MARKED';
ALTER TABLE exam_answers ALTER COLUMN detection_status DROP DEFAULT;
