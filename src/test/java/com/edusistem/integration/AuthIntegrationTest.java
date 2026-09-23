package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import com.edusistem.core.auth.domain.outputports.MailSenderPort;
import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AuthIntegrationTest extends IntegrationTest {

    @MockitoBean
    MailSenderPort mailSender;

    private Map<String, String> registration(String email) {
        return Map.of("firstName", "Ana", "lastName", "Prof", "email", email, "password", PASSWORD);
    }

    @Test
    void registerCreatesTeacherReturnsSessionAndStoresOnlyAHash() {
        String email = unique("reg") + "@example.com";
        JsonNode body = call("POST", null, "/api/v1/auth/register", registration(email), 201);

        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("user").get("email").asText()).isEqualTo(email);
        assertThat(body.get("user").get("roles").get(0).asText()).isEqualTo("TEACHER");
        assertThat(body.toString()).doesNotContain(PASSWORD).doesNotContain("password");

        String stored = jdbc.queryForObject("select password_hash from users where email = ?", String.class, email);
        assertThat(stored).isNotEqualTo(PASSWORD).startsWith("$2");
    }

    @Test
    void registerRejectsDuplicateEmailEvenWithDifferentCase() {
        String email = unique("dup") + "@example.com";
        call("POST", null, "/api/v1/auth/register", registration(email), 201);
        JsonNode error = call("POST", null, "/api/v1/auth/register", registration(email.toUpperCase()), 409);
        assertError(error, 409, "EMAIL_ALREADY_REGISTERED");
    }

    @Test
    void registerValidatesInput() {
        JsonNode invalid = call("POST", null, "/api/v1/auth/register",
                Map.of("firstName", "", "lastName", "X", "email", "not-an-email", "password", "short"), 400);
        assertError(invalid, 400, "INVALID_REQUEST");
        assertThat(invalid.get("errors").size()).isGreaterThanOrEqualTo(3);
        assertThat(invalid.toString()).doesNotContain("Exception").doesNotContain("at com.");

        JsonNode weak = call("POST", null, "/api/v1/auth/register", Map.of("firstName", "A", "lastName", "B",
                "email", unique("weak") + "@example.com", "password", "onlyletters"), 400);
        assertError(weak, 400, "WEAK_PASSWORD");
    }

    @Test
    void loginReturnsJwtAndRejectsBadCredentialsUniformly() {
        Teacher t = newTeacher();
        JsonNode ok = call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD), 200);
        assertThat(ok.get("accessToken").asText()).isNotBlank();
        assertThat(ok.get("user").get("id").asLong()).isEqualTo(t.id());

        JsonNode wrongPassword = call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", "Wrong123"), 401);
        assertError(wrongPassword, 401, "INVALID_CREDENTIALS");
        JsonNode unknownUser = call("POST", null, "/api/v1/auth/login",
                Map.of("email", "nobody" + unique("") + "@example.com", "password", PASSWORD), 401);
        assertError(unknownUser, 401, "INVALID_CREDENTIALS");
        assertThat(unknownUser.get("message").asText()).isEqualTo(wrongPassword.get("message").asText());
    }

    @Test
    void meRequiresAValidJwt() {
        Teacher t = newTeacher();
        JsonNode me = get(t, "/api/v1/auth/me", 200);
        assertThat(me.get("email").asText()).isEqualTo(t.email());

        assertError(get(null, "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        assertError(get(new Teacher(0, "x", "garbage.token.value"), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
    }

    @Test
    void profileUpdateOnlyChangesNamesAndIgnoresSensitiveFields() {
        Teacher t = newTeacher();
        JsonNode updated = put(t, "/api/v1/users/me", Map.of("firstName", "Nuevo", "lastName", "Nombre",
                "email", "hacker@example.com", "roles", List.of("ADMIN"), "id", 999), 200);
        assertThat(updated.get("firstName").asText()).isEqualTo("Nuevo");
        assertThat(updated.get("email").asText()).isEqualTo(t.email());
        assertThat(updated.get("roles").toString()).contains("TEACHER").doesNotContain("ADMIN");
        assertThat(updated.get("id").asLong()).isEqualTo(t.id());
    }

    @Test
    void changePasswordRequiresCurrentPasswordAndInvalidatesTheOldOne() {
        Teacher t = newTeacher();
        assertError(post(t, "/api/v1/auth/change-password", Map.of("currentPassword", "Nope12345", "newPassword", "Another456"), 400),
                400, "INVALID_CURRENT_PASSWORD");
        JsonNode renewed = call("POST", t, "/api/v1/auth/change-password",
                Map.of("currentPassword", PASSWORD, "newPassword", "Another456"), 200);
        // las sesiones anteriores se cierran, pero se entrega una sesión nueva para este dispositivo
        assertError(get(t, "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        get(new Teacher(t.id(), t.email(), renewed.get("accessToken").asText()), "/api/v1/auth/me", 200);
        call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", "Another456"), 200);
        call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD), 401);
    }

    @Test
    void passwordRecoveryFlowWithVerifyAndReset() {
        reset(mailSender);
        Teacher t = newTeacher();
        JsonNode openSession = call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD), 200);
        call("POST", null, "/api/v1/auth/forgot-password", Map.of("email", t.email()), 202);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendPasswordResetCode(eq(t.email()), code.capture(), anyInt());
        assertThat(code.getValue()).matches("\\d{6}");
        String wrong = code.getValue().equals("000000") ? "111111" : "000000";

        assertError(call("POST", null, "/api/v1/auth/verify-code", Map.of("email", t.email(), "code", wrong), 400),
                400, "INVALID_RESET_CODE");
        call("POST", null, "/api/v1/auth/verify-code", Map.of("email", t.email(), "code", code.getValue()), 200);
        call("POST", null, "/api/v1/auth/reset-password",
                Map.of("email", t.email(), "code", code.getValue(), "newPassword", "Brandnew789"), 200);

        call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", "Brandnew789"), 200);
        call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD), 401);
        // el restablecimiento cierra las sesiones abiertas (access y refresh tokens)
        assertError(get(new Teacher(t.id(), t.email(), openSession.get("accessToken").asText()), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        assertError(call("POST", null, "/api/v1/auth/refresh", Map.of("refreshToken", openSession.get("refreshToken").asText()), 401),
                401, "INVALID_REFRESH_TOKEN");
        // el código ya fue usado
        assertError(call("POST", null, "/api/v1/auth/reset-password",
                Map.of("email", t.email(), "code", code.getValue(), "newPassword", "Again12345"), 400), 400, "INVALID_RESET_CODE");
        String stored = jdbc.queryForObject("select code_hash from password_reset_tokens where user_id = ? order by id desc limit 1",
                String.class, t.id());
        assertThat(stored).isNotEqualTo(code.getValue()).startsWith("$2");
    }

    @Test
    void forgotPasswordDoesNotRevealWhetherTheEmailExists() {
        reset(mailSender);
        JsonNode unknown = call("POST", null, "/api/v1/auth/forgot-password",
                Map.of("email", "ghost" + unique("") + "@example.com"), 202);
        assertThat(unknown.get("message").asText()).contains("If the email is registered");
        verify(mailSender, never()).sendPasswordResetCode(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), anyInt());
    }

    @Test
    void resetCodeIsLockedAfterTooManyWrongAttempts() {
        reset(mailSender);
        Teacher t = newTeacher();
        call("POST", null, "/api/v1/auth/forgot-password", Map.of("email", t.email()), 202);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendPasswordResetCode(eq(t.email()), code.capture(), anyInt());
        String wrong = code.getValue().equals("000000") ? "111111" : "000000";
        for (int i = 0; i < 5; i++) {
            call("POST", null, "/api/v1/auth/verify-code", Map.of("email", t.email(), "code", wrong), 400);
        }
        // ni siquiera el código correcto sirve después del bloqueo
        call("POST", null, "/api/v1/auth/verify-code", Map.of("email", t.email(), "code", code.getValue()), 400);
    }

    @Test
    void loginAndLogoutAreAudited() {
        Teacher t = newTeacher();
        call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD), 200);
        call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", "Wrong1234"), 401);
        call("POST", t, "/api/v1/auth/logout", null, 204);
        JsonNode again = call("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD), 200);
        Teacher fresh = new Teacher(t.id(), t.email(), again.get("accessToken").asText());
        JsonNode logins = get(fresh, "/api/v1/audit-logs?action=LOGIN", 200);
        assertThat(logins.get("content").toString()).contains("SUCCESS").contains("FAILURE");
        assertThat(get(fresh, "/api/v1/audit-logs?action=LOGOUT", 200).get("content").size()).isEqualTo(1);
    }

    @Test
    void swaggerAndOpenApiAreExposedAndDescribeBearerAuth() throws Exception {
        String docs = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
                .andReturn().getResponse().getContentAsString();
        assertThat(docs).contains("bearerAuth").contains("/api/v1/auth/login").contains("/api/v1/exams/{examId}/submissions");
        assertThat(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/swagger-ui/index.html"))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}
