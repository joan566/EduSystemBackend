package com.edusistem.core.shared.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /** Zona horaria del colegio (el reloj sigue en UTC; esta zona sólo se usa para calcular fechas y horas locales). */
    @Bean
    ZoneId schoolZoneId(SchoolProperties school) {
        return ZoneId.of(school.timezone());
    }
}
