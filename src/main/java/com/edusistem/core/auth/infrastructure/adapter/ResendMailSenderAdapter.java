package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Envía por la API de Resend si {@code edusistem.mail.enabled=true}; nunca registra el código ni la API key en logs. El
 * envío va en segundo plano para que la latencia de Resend no delate (por el tiempo de respuesta) qué correos están
 * registrados.
 */
@Component
public class ResendMailSenderAdapter implements MailSenderPort, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ResendMailSenderAdapter.class);

    private final Resend resend;
    private final String from;
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    public ResendMailSenderAdapter(@Value("${edusistem.mail.enabled}") boolean enabled,
                                   @Value("${edusistem.mail.resend.api-key:}") String apiKey,
                                   @Value("${edusistem.mail.resend.from:}") String from) {
        if (enabled && (apiKey.isBlank() || from.isBlank())) {
            throw new IllegalStateException("MAIL_ENABLED=true requires RESEND_API_KEY and RESEND_FROM");
        }
        this.resend = enabled ? new Resend(apiKey) : null;
        this.from = from;
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
    }

    @Override
    public void sendPasswordResetCode(String to, String code, int ttlMinutes) {
        send(to, "EduSistem - código de recuperación de contraseña", "Tu código de recuperación es", code, ttlMinutes,
             "password reset");
    }

    @Override
    public void sendEmailVerificationCode(String to, String code, int ttlMinutes) {
        send(to, "EduSistem - código de verificación de correo", "Tu código de verificación es", code, ttlMinutes,
             "email verification");
    }

    private void send(String to, String subject, String intro, String code, int ttlMinutes, String kind) {
        if (resend == null) {
            log.warn("Mail delivery is disabled (edusistem.mail.enabled=false); {} code was not sent", kind);
            return;
        }
        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(from)
                .to(to)
                .subject(subject)
                .text(intro + ": " + code + "\nExpira en " + ttlMinutes + " minutos.")
                .html("<p>" + intro + ": <strong>" + code + "</strong></p><p>Expira en " + ttlMinutes
                      + " minutos.</p>")
                .build();
        try {
            executor.execute(() -> deliver(email, kind));
        } catch (RuntimeException e) { // cola llena: se descarta; el usuario puede pedir otro código
            log.error("Could not queue {} e-mail", kind, e);
        }
    }

    private void deliver(CreateEmailOptions email, String kind) {
        try {
            resend.emails().send(email);
        } catch (ResendException e) {
            log.error("Resend rejected the {} e-mail (status {}): {}", kind, e.getStatusCode(), e.getMessage());
        } catch (RuntimeException e) {
            log.error("Could not send {} e-mail", kind, e);
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
