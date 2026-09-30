package com.edusistem.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.edusistem.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

/** Con API_DOCS_ENABLED=true (desarrollo) la documentación OpenAPI se publica y describe la autenticación Bearer. */
@TestPropertySource(properties = {"springdoc.api-docs.enabled=true", "springdoc.swagger-ui.enabled=true"})
class ApiDocsIntegrationTest extends IntegrationTest {

    @Test
    void swaggerAndOpenApiAreExposedAndDescribeBearerAuth() throws Exception {
        String docs = perform("GET", null, "/v3/api-docs", null).getResponse().getContentAsString();
        assertThat(docs).contains("bearerAuth").contains("/api/v1/auth/login").contains("/api/v1/exams/{examId}/submissions");
        assertThat(perform("GET", null, "/swagger-ui/index.html", null).getResponse().getStatus()).isEqualTo(200);
    }
}
