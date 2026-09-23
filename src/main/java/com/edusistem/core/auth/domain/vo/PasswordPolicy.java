package com.edusistem.core.auth.domain.vo;

import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;

/** Política mínima de contraseñas: 8-72 caracteres (límite de BCrypt), con al menos una letra y un dígito. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static void validate(String password) {
        boolean valid = password != null
                && password.length() >= MIN_LENGTH
                && password.length() <= MAX_LENGTH
                && password.chars().anyMatch(Character::isLetter)
                && password.chars().anyMatch(Character::isDigit);
        if (!valid) {
            throw new InvalidRequestException("WEAK_PASSWORD",
                    "Password must have between 8 and 72 characters, with at least one letter and one digit");
        }
    }
}
