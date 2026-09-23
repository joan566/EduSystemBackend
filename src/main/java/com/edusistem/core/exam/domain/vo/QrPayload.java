package com.edusistem.core.exam.domain.vo;

import com.edusistem.core.shared.domain.exceptions.BusinessRuleException;
import java.util.regex.Pattern;

/**
 * Contenido versionado del QR de la hoja: {@code EDU1|examId|studentCode}. Solo identifica examen y estudiante;
 * no incluye datos personales, correo ni credenciales.
 */
public record QrPayload(String version, long examId, String studentCode) {

    public static final String CURRENT_VERSION = "EDU1";
    private static final Pattern NUMBER = Pattern.compile("\\d{1,18}");

    public static String encode(long examId, String studentCode) {
        return CURRENT_VERSION + "|" + examId + "|" + studentCode;
    }

    public static QrPayload parse(String raw) {
        if (raw == null) {
            throw invalid();
        }
        String[] parts = raw.trim().split("\\|", 3);
        if (parts.length != 3 || !CURRENT_VERSION.equals(parts[0]) || !NUMBER.matcher(parts[1]).matches()
                || parts[2].isBlank() || parts[2].length() > 50) {
            throw invalid();
        }
        return new QrPayload(parts[0], Long.parseLong(parts[1]), parts[2]);
    }

    private static BusinessRuleException invalid() {
        return new BusinessRuleException("INVALID_QR", "The QR code is not a valid EduSistem answer sheet code");
    }
}
