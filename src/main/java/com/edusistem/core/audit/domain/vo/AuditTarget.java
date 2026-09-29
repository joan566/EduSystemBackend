package com.edusistem.core.audit.domain.vo;

/**
 * Entidad sobre la que recae una acción auditada. {@code teachingPeriodId} liga el registro a una clase (para su
 * actividad reciente) y {@code label} es un nombre legible, p. ej. "Parcial 1 · Ana Pérez".
 */
public record AuditTarget(String entityType, Long entityId, Long teachingPeriodId, String label) {

    public static AuditTarget of(String entityType, Long entityId) {
        return new AuditTarget(entityType, entityId, null, null);
    }

    public static AuditTarget inTeachingPeriod(String entityType, Long entityId, Long teachingPeriodId, String label) {
        return new AuditTarget(entityType, entityId, teachingPeriodId, label);
    }

    /** "Parcial 1 · Ana Pérez"; omite las partes vacías. */
    public static String label(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" · ");
                }
                sb.append(part.trim());
            }
        }
        return sb.isEmpty() ? null : sb.toString();
    }
}
