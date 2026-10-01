package com.edusistem.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edusistem.core.auth.infrastructure.adapter.ResendMailSenderAdapter;
import org.junit.jupiter.api.Test;

class ResendMailSenderAdapterTest {

    @Test
    void enabledMailRequiresApiKeyAndSender() {
        assertThatThrownBy(() -> new ResendMailSenderAdapter(true, "", "EduSistem <no-reply@example.com>"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RESEND_API_KEY");
        assertThatThrownBy(() -> new ResendMailSenderAdapter(true, "re_test", " "))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("RESEND_FROM");
    }

    @Test
    void disabledMailNeedsNoConfigurationAndSendsNothing() {
        ResendMailSenderAdapter adapter = new ResendMailSenderAdapter(false, "", "");
        assertThatCode(() -> {
            adapter.sendPasswordResetCode("a@example.com", "123456", 15);
            adapter.sendEmailVerificationCode("a@example.com", "123456", 15);
        }).doesNotThrowAnyException();
        adapter.destroy();
    }
}
