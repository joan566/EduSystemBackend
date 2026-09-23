package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

/** Refresh tokens, revocación de JWT y límite de intentos de login. */
class SessionSecurityIntegrationTest extends IntegrationTest {

    private JsonNode login(String email, String password, int status) {
        return call("POST", null, "/api/v1/auth/login", Map.of("email", email, "password", password), status);
    }

    private Teacher asTeacher(Teacher base, JsonNode session) {
        return new Teacher(base.id(), base.email(), session.get("accessToken").asText());
    }

    private JsonNode refresh(String token, int status) {
        return call("POST", null, "/api/v1/auth/refresh", Map.of("refreshToken", token), status);
    }

    @Test
    void loginAndRegisterReturnARefreshTokenThatIsOnlyStoredHashed() {
        Teacher t = newTeacher();
        JsonNode session = login(t.email(), PASSWORD, 200);
        String refreshToken = session.get("refreshToken").asText();
        assertThat(refreshToken).hasSizeGreaterThanOrEqualTo(40);
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where token_hash = ?", Integer.class, refreshToken)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ?", Integer.class, t.id())).isEqualTo(2);
    }

    @Test
    void refreshRotatesTheTokenAndTheOldOneCannotBeReusedNorItsDescendants() {
        Teacher t = newTeacher();
        String first = login(t.email(), PASSWORD, 200).get("refreshToken").asText();

        JsonNode renewed = refresh(first, 200);
        String second = renewed.get("refreshToken").asText();
        assertThat(second).isNotEqualTo(first);
        get(asTeacher(t, renewed), "/api/v1/auth/me", 200);

        // reutilizar el token ya rotado = posible robo: se rechaza y se cierran TODAS las sesiones
        assertError(refresh(first, 401), 401, "INVALID_REFRESH_TOKEN");
        assertError(refresh(second, 401), 401, "INVALID_REFRESH_TOKEN");
        assertError(get(asTeacher(t, renewed), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        login(t.email(), PASSWORD, 200); // el usuario puede volver a iniciar sesión
    }

    @Test
    void invalidAndExpiredRefreshTokensAreRejected() {
        Teacher t = newTeacher();
        assertError(refresh("not-a-real-token", 401), 401, "INVALID_REFRESH_TOKEN");
        assertError(call("POST", null, "/api/v1/auth/refresh", Map.of("refreshToken", ""), 400), 400, "INVALID_REQUEST");
        String token = login(t.email(), PASSWORD, 200).get("refreshToken").asText();
        jdbc.update("update refresh_tokens set expires_at = now() - interval '1 minute' where user_id = ?", t.id());
        assertError(refresh(token, 401), 401, "INVALID_REFRESH_TOKEN");
    }

    @Test
    void logoutRevokesAccessAndRefreshTokensEverywhere() {
        Teacher t = newTeacher();
        JsonNode deviceA = login(t.email(), PASSWORD, 200);
        JsonNode deviceB = login(t.email(), PASSWORD, 200);
        call("POST", asTeacher(t, deviceA), "/api/v1/auth/logout", null, 204);

        assertError(get(asTeacher(t, deviceA), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        assertError(get(asTeacher(t, deviceB), "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        assertError(refresh(deviceA.get("refreshToken").asText(), 401), 401, "INVALID_REFRESH_TOKEN");
        assertError(refresh(deviceB.get("refreshToken").asText(), 401), 401, "INVALID_REFRESH_TOKEN");
    }

    @Test
    void deactivatedUsersLoseAccessImmediatelyEvenWithAValidToken() {
        Teacher t = newTeacher();
        get(t, "/api/v1/auth/me", 200);
        jdbc.update("update users set active = false where id = ?", t.id());
        assertError(get(t, "/api/v1/auth/me", 401), 401, "UNAUTHORIZED");
        login(t.email(), PASSWORD, 401);
    }

    @Test
    void repeatedFailedLoginsAreBlockedWithRetryAfterEvenForTheCorrectPassword() {
        Teacher t = newTeacher();
        Teacher other = newTeacher();
        for (int i = 0; i < 5; i++) {
            login(t.email(), "Wrong" + i + "1234", 401);
        }
        MvcResult blocked = perform("POST", null, "/api/v1/auth/login", Map.of("email", t.email(), "password", PASSWORD));
        assertError(parse(blocked, 429), 429, "TOO_MANY_LOGIN_ATTEMPTS");
        assertThat(Long.parseLong(blocked.getResponse().getHeader("Retry-After"))).isBetween(1L, 15 * 60L + 1);
        // otros usuarios no se ven afectados
        login(other.email(), PASSWORD, 200);
        assertThat(get(other, "/api/v1/audit-logs?action=LOGIN", 200).get("content").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void successfulLoginResetsTheFailureCounter() {
        Teacher t = newTeacher();
        for (int round = 0; round < 3; round++) {
            for (int i = 0; i < 4; i++) {
                login(t.email(), "Wrong" + i + "1234", 401);
            }
            login(t.email(), PASSWORD, 200); // nunca se acumulan 5 fallos seguidos
        }
    }
}
