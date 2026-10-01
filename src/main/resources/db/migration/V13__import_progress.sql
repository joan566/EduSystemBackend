-- Avance de una importación en segundo plano: filas ya recorridas (de total_rows, que se fija al leer el archivo) y
-- la hoja que se está procesando. Se actualizan mientras corre; al terminar processed_rows = total_rows.
ALTER TABLE import_batches ADD COLUMN processed_rows INT;
ALTER TABLE import_batches ADD COLUMN current_step VARCHAR(100);
