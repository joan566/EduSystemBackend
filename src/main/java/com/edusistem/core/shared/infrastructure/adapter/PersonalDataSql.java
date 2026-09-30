package com.edusistem.core.shared.infrastructure.adapter;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Objects;

/** SQL nativo compartido por los borrados de datos personales (estudiante o cuenta completa). */
public final class PersonalDataSql {

    public static final String ERASED_STUDENT = "[estudiante eliminado]";

    private PersonalDataSql() {
    }

    public static int update(EntityManager em, String sql, Object... namedParamPairs) {
        var query = em.createNativeQuery(sql);
        for (int i = 0; i < namedParamPairs.length; i += 2) {
            query.setParameter((String) namedParamPairs[i], namedParamPairs[i + 1]);
        }
        return query.executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public static List<String> strings(EntityManager em, String sql, Object... namedParamPairs) {
        var query = em.createNativeQuery(sql);
        for (int i = 0; i < namedParamPairs.length; i += 2) {
            query.setParameter((String) namedParamPairs[i], namedParamPairs[i + 1]);
        }
        return ((List<Object>) query.getResultList()).stream().filter(Objects::nonNull).map(Object::toString).toList();
    }

    /**
     * Sustituye {@code value} (como palabra completa) por {@code replacement} en la etiqueta y el detalle de la
     * auditoría de un usuario; la auditoría se conserva pero sin datos del estudiante borrado.
     */
    public static void scrubAudit(EntityManager em, Long userId, String value, String replacement) {
        if (value == null || value.isBlank()) {
            return;
        }
        String pattern = "(^|[^[:alnum:]])" + escapeRegex(value) + "(?=[^[:alnum:]]|$)";
        update(em, """
                update audit_logs
                set entity_label = regexp_replace(entity_label, :pattern, '\\1' || :replacement, 'g'),
                    details = regexp_replace(details, :pattern, '\\1' || :replacement, 'g')
                where user_id = :user and (entity_label ~ :pattern or details ~ :pattern)""",
                "pattern", pattern, "replacement", replacement, "user", userId);
    }

    /** En las expresiones regulares de PostgreSQL, "\" seguido de un carácter no alfanumérico lo vuelve literal. */
    static String escapeRegex(String value) {
        StringBuilder escaped = new StringBuilder();
        value.codePoints().forEach(c -> {
            if (!Character.isLetterOrDigit(c)) {
                escaped.append('\\');
            }
            escaped.appendCodePoint(c);
        });
        return escaped.toString();
    }
}
