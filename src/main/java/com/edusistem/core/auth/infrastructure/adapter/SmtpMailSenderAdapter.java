package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envía por SMTP si {@code edusistem.mail.enabled=true}; nunca registra el código en logs. */
@Component
public class SmtpMailSenderAdapter implements MailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSenderAdapter.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public SmtpMailSenderAdapter(JavaMailSender mailSender, @Value("${edusistem.mail.enabled}") boolean enabled,
                                 @Value("${edusistem.mail.from}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public void sendPasswordResetCode(String to, String code, int ttlMinutes) {
        if (!enabled) {
            log.warn("Mail delivery is disabled (edusistem.mail.enabled=false); password reset code was not sent");
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject("EduSistem - código de recuperación de contraseña");
            message.setText("Tu código de recuperación es: " + code + "\nExpira en " + ttlMinutes + " minutos.");
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("Could not send password reset e-mail", e);
        }
    }
}
