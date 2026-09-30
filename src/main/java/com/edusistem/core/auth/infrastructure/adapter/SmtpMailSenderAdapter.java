package com.edusistem.core.auth.infrastructure.adapter;

import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Envía por SMTP si {@code edusistem.mail.enabled=true}; nunca registra el código en logs. El envío va en segundo plano
 * para que la latencia del SMTP no delate (por el tiempo de respuesta) qué correos están registrados.
 */
@Component
public class SmtpMailSenderAdapter implements MailSenderPort, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSenderAdapter.class);

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;
    private final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    public SmtpMailSenderAdapter(JavaMailSender mailSender, @Value("${edusistem.mail.enabled}") boolean enabled,
                                 @Value("${edusistem.mail.from}") String from) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1_000);
        executor.setThreadNamePrefix("mail-");
        executor.initialize();
    }

    @Override
    public void sendPasswordResetCode(String to, String code, int ttlMinutes) {
        if (!enabled) {
            log.warn("Mail delivery is disabled (edusistem.mail.enabled=false); password reset code was not sent");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("EduSistem - código de recuperación de contraseña");
        message.setText("Tu código de recuperación es: " + code + "\nExpira en " + ttlMinutes + " minutos.");
        try {
            executor.execute(() -> send(message));
        } catch (RuntimeException e) { // cola llena: se descarta; el usuario puede pedir otro código
            log.error("Could not queue password reset e-mail", e);
        }
    }

    private void send(SimpleMailMessage message) {
        try {
            mailSender.send(message);
        } catch (RuntimeException e) {
            log.error("Could not send password reset e-mail", e);
        }
    }

    @Override
    public void destroy() {
        executor.shutdown();
    }
}
