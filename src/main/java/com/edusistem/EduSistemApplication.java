package com.edusistem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// Sin UserDetailsServiceAutoConfiguration: la autenticación es por JWT (JwtAuthenticationFilter), y ese usuario en
// memoria no se usaría y dejaría su contraseña generada en los logs.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class EduSistemApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduSistemApplication.class, args);
    }
}
