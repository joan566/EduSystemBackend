#!/usr/bin/env bash
# Backup cifrado de la base (pg_dump) y de la carpeta de archivos (STORAGE_PATH).
#
# Variables:
#   PGHOST, PGPORT, PGDATABASE, PGUSER, PGPASSWORD  conexión a PostgreSQL (variables estándar de libpq)
#   STORAGE_PATH             carpeta de archivos de la app (por defecto ./storage)
#   BACKUP_DIR               dónde dejar los backups (por defecto ./backups)
#   BACKUP_PASSPHRASE_FILE   archivo con la frase de cifrado (obligatorio; guárdalo fuera del servidor también)
#   BACKUP_RETENTION_DAYS    días que se conservan los backups locales (por defecto 14)
#
# Cron diario de ejemplo (03:00):
#   0 3 * * * cd /opt/edusistem && ./scripts/backup.sh >> /var/log/edusistem-backup.log 2>&1
# Copia BACKUP_DIR fuera del servidor (otro proveedor o región) con rclone, aws s3 sync, restic...
set -euo pipefail
umask 077

: "${PGDATABASE:?PGDATABASE is required}"
: "${BACKUP_PASSPHRASE_FILE:?BACKUP_PASSPHRASE_FILE is required}"
STORAGE_PATH="${STORAGE_PATH:-./storage}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"

[[ -r "$BACKUP_PASSPHRASE_FILE" ]] || { echo "Cannot read $BACKUP_PASSPHRASE_FILE" >&2; exit 1; }
mkdir -p "$BACKUP_DIR"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
encrypt() {
  gpg --batch --yes --quiet --symmetric --cipher-algo AES256 --passphrase-file "$BACKUP_PASSPHRASE_FILE" -o "$1"
}

# La base primero: un archivo subido durante el backup queda huérfano en el tar, nunca una fila sin su archivo.
db_file="$BACKUP_DIR/edusistem-db-$stamp.dump.gpg"
pg_dump --format=custom --no-owner --no-privileges | encrypt "$db_file.part"
mv "$db_file.part" "$db_file"

files_file="$BACKUP_DIR/edusistem-storage-$stamp.tar.gz.gpg"
if [[ -d "$STORAGE_PATH" ]]; then
  tar -C "$STORAGE_PATH" -czf - . | encrypt "$files_file.part"
  mv "$files_file.part" "$files_file"
fi

(cd "$BACKUP_DIR" && sha256sum "edusistem-"*"-$stamp."*.gpg > "edusistem-$stamp.sha256")
find "$BACKUP_DIR" -maxdepth 1 -name 'edusistem-*' -type f -mtime +"$BACKUP_RETENTION_DAYS" -delete
echo "$(date -u +%FT%TZ) backup $stamp done"
