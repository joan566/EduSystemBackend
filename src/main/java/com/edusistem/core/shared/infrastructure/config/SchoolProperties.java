package com.edusistem.core.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Datos del colegio; {@code timezone} define qué es "hoy" para horarios y agenda (ID IANA, p. ej. America/Bogota). */
@ConfigurationProperties(prefix = "edusistem.school")
public record SchoolProperties(String timezone) {
}
