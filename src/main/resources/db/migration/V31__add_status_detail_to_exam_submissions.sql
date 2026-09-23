-- Fuera del DBML: motivo legible del estado (FAILED / REVIEW_REQUIRED).
ALTER TABLE exam_submissions ADD COLUMN status_detail VARCHAR(500);
