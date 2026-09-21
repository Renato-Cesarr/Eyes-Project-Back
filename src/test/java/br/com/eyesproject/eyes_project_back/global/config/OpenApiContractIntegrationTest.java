package br.com.eyesproject.eyes_project_back.global.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class OpenApiContractIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("OpenAPI distinguishes protected invitations from public activation")
    void invitationAndActivationSecurityIsDocumented() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['201']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/users/{id}'].get.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{id}/status'].patch.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/{id}/resend-invitation'].post.responses['204']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/setup-password'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/users/setup-password'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/users/setup-password'].post.responses['422']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/forgot-password'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/reset-password'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/access-requests'].post.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/access-requests'].post.responses['202']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/access-requests'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/access-requests/{id}/approve'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/access-requests/{id}/reject'].post.security[0].bearerAuth").isArray());
    }
}
