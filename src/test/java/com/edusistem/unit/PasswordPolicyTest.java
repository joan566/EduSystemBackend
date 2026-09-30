package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.auth.domain.vo.PasswordPolicy;
import com.edusistem.core.shared.domain.exceptions.InvalidRequestException;
import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsReasonablePasswords() {
        assertThatCode(() -> PasswordPolicy.validate("Secret123")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortNumericOnlyAlphabeticOnlyAndTooLong() {
        for (String bad : new String[]{"abc12", "12345678", "onlyletters", "a1".repeat(40), null}) {
            assertThatThrownBy(() -> PasswordPolicy.validate(bad)).isInstanceOf(InvalidRequestException.class);
        }
    }

    @Test
    void limitsTheLengthInUtf8BytesBecauseBcryptIgnoresEverythingAfter72() {
        assertThatCode(() -> PasswordPolicy.validate("ñ1".repeat(24))).doesNotThrowAnyException(); // 72 bytes
        assertThatThrownBy(() -> PasswordPolicy.validate("ñ1".repeat(25))) // 50 caracteres, 75 bytes
                .isInstanceOf(InvalidRequestException.class);
    }
}
