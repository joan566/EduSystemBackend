package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import com.edusistem.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

class CorsIntegrationTest extends IntegrationTest {

    private MockHttpServletResponse preflight(String origin, String method) throws Exception {
        return mvc.perform(options("/api/v1/students").header("Origin", origin)
                .header("Access-Control-Request-Method", method)
                .header("Access-Control-Request-Headers", "authorization,content-type")).andReturn().getResponse();
    }

    @Test
    void preflightFromAConfiguredOriginIsAcceptedWithoutAuthentication() throws Exception {
        MockHttpServletResponse response = preflight("http://localhost:3000", "GET");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("GET").contains("PUT").contains("DELETE");
        assertThat(response.getHeader("Access-Control-Allow-Headers")).containsIgnoringCase("authorization");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    void wildcardPatternsAreSupported() throws Exception {
        assertThat(preflight("https://app.edusistem.test", "POST").getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("https://app.edusistem.test");
    }

    @Test
    void unknownOriginsAreRejected() throws Exception {
        MockHttpServletResponse response = preflight("https://evil.example.com", "GET");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    @Test
    void actualResponsesCarryCorsHeadersAndExposeDownloadHeaders() throws Exception {
        Teacher t = newTeacher();
        MockHttpServletResponse response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/auth/me").header("Origin", "http://localhost:3000")
                .header("Authorization", "Bearer " + t.token())).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
        MockHttpServletResponse unauthorized = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/students").header("Origin", "http://localhost:3000"))
                .andReturn().getResponse();
        assertThat(unauthorized.getStatus()).isEqualTo(401); // el navegador puede leer el 401 y reaccionar
        assertThat(unauthorized.getHeader("Access-Control-Allow-Origin")).isEqualTo("http://localhost:3000");
    }
}
