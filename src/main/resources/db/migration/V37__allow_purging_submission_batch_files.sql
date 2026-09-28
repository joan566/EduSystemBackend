-- El PDF original de un lote terminado se borra tras el periodo de retención; el lote y sus resultados se conservan.
ALTER TABLE exam_submission_batches ALTER COLUMN file_path DROP NOT NULL;
ALTER TABLE exam_submission_batches ADD COLUMN file_purged_at TIMESTAMP;
