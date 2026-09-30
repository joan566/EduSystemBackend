package com.edusistem.core.auth.domain.vo;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import java.nio.charset.StandardCharsets;

/**
 * Política mínima de contraseñas: al menos 8 caracteres, con una letra y un dígito, y como mucho 72 bytes en UTF-8
 * (límite de BCrypt: lo que pasa de ahí se ignoraría; una tilde o una ñ ocupan 2 bytes).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        boolean valid = password != null
                && password.length() >= MIN_LENGTH
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!valid) {
            throw new InvalidRequestException("WEAK_PASSWORD",
                    "Password must have at least 8 characters and at most 72 bytes (accents and other special "
                            + "characters take more than one), with at least one letter and one digit");
        }
    }
}
