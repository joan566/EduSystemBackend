package com.edusistem.core.auth.domain.outputports;

public interface MailSenderPort {

    void sendPasswordResetCode(String to, String code, int ttlMinutes);
}
