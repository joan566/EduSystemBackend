#!/usr/bin/env bash
# Restaura un backup de scripts/backup.sh. SOBRESCRIBE la base PGDATABASE y el contenido de STORAGE_PATH.
#
#   ./scripts/restore.sh backups/edusistem-db-20260101T030000Z.dump.gpg [backups/edusistem-storage-...tar.gz.gpg]
#
# Mismas variables que backup.sh (PG*, STORAGE_PATH, BACKUP_PASSPHRASE_FILE). Los archivos cifrados por la app
# (STORAGE_ENCRYPTION_KEY) siguen cifrados en el backup: hace falta la misma clave para que la app los lea.
# Ensáyalo periódicamente contra una base de prueba: un backup que nunca se restauró no es un backup.
set -euo pipefail

db_backup="${1:?usage: restore.sh <db-backup.dump.gpg> [storage-backup.tar.gz.gpg]}"
files_backup="${2:-}"
: "${PGDATABASE:?PGDATABASE is required}"
: "${BACKUP_PASSPHRASE_FILE:?BACKUP_PASSPHRASE_FILE is required}"
STORAGE_PATH="${STORAGE_PATH:-./storage}"

sums="$(dirname "$db_backup")/$(basename "$db_backup" | sed -E 's/^edusistem-db-(.*)\.dump\.gpg$/edusistem-\1.sha256/')"
if [[ -f "$sums" ]]; then
  (cd "$(dirname "$db_backup")" && sha256sum --check --ignore-missing "$(basename "$sums")")
fi

read -r -p "This will overwrite database '$PGDATABASE' and '$STORAGE_PATH'. Type the database name to continue: " answer
[[ "$answer" == "$PGDATABASE" ]] || { echo "Aborted" >&2; exit 1; }

decrypt() {
  gpg --batch --quiet --decrypt --passphrase-file "$BACKUP_PASSPHRASE_FILE" "$1"
}

decrypt "$db_backup" | pg_restore --clean --if-exists --no-owner --no-privileges --single-transaction -d "$PGDATABASE"
if [[ -n "$files_backup" ]]; then
  mkdir -p "$STORAGE_PATH"
  find "$STORAGE_PATH" -mindepth 1 -delete
  decrypt "$files_backup" | tar -C "$STORAGE_PATH" -xzf -
fi
echo "Restore done"
