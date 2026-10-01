package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** GET /api/v1/app/version es público y devuelve las versiones configuradas; el resto de la API sigue protegida. */
@TestPropertySource(properties = {"edusistem.app-version.latest=1.2.0", "edusistem.app-version.minimum=1.1.0"})
class AppVersionIntegrationTest extends IntegrationTest {

    private static final String URL = "/api/v1/app/version";

    @Test
    void returnsConfiguredVersionsWithoutAuthentication() {
        JsonNode body = get(null, URL, 200);
        assertThat(body.get("latestVersion").asText()).isEqualTo("1.2.0");
        assertThat(body.get("minimumVersion").asText()).isEqualTo("1.1.0");
        assertThat(body.size()).isEqualTo(2);
    }

    @Test
    void stillAnswersWithInvalidOrValidToken() throws Exception {
        MvcResult result = mvc.perform(MockMvcRequestBuilders.get(URL).header("Authorization", "Bearer not-a-valid-token")).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(get(newTeacher(), URL, 200).get("latestVersion").asText()).isEqualTo("1.2.0");
    }

    @Test
    void onlyGetIsPublicAndOtherEndpointsStayProtected() {
        assertThat(perform("POST", null, URL, null).getResponse().getStatus()).isEqualTo(401);
        get(null, "/api/v1/users/me", 401);
        get(null, "/api/v1/schedule/today", 401);
    }
}
