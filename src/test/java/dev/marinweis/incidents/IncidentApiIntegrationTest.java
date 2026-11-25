package dev.marinweis.incidents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.marinweis.incidents.model.Role;
import dev.marinweis.incidents.model.User;
import dev.marinweis.incidents.repository.IncidentCommentRepository;
import dev.marinweis.incidents.repository.IncidentRepository;
import dev.marinweis.incidents.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentCommentRepository commentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        incidentRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedRequestsShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerLoginAndMeFlowShouldWork() throws Exception {
        String registerBody = """
                {"username":"alice","email":"alice@example.com","password":"Password123!"}
                """;
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("USER"))
                .andReturn();

        String token = extractToken(regResult);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        // Duplicate username should return 409 Conflict ProblemDetail
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void fullIncidentLifecycleWithFilteringCommentsAndRbac() throws Exception {
        String userToken = registerAndGetToken("devuser", "dev@example.com");
        String adminToken = createAdminAndGetToken("admin", "admin@example.com");

        // Validation error returns RFC 7807 ProblemDetail with field errors
        mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hi","description":"short","priority":"HIGH"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"))
                .andExpect(jsonPath("$.errors.title").exists());

        // Create two incidents
        long firstId = createIncident(userToken, "Database connection pool exhausted",
                "Production PostgreSQL pool reached 100% saturation during peak load.", "CRITICAL");
        createIncident(userToken, "Minor CSS misalignment on login",
                "Submit button is shifted by 4px on mobile Safari.", "LOW");

        // Filter by priority & keyword
        mockMvc.perform(get("/api/v1/incidents")
                        .param("priority", "CRITICAL")
                        .param("q", "postgresql")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstId));

        // Add comment & update status
        mockMvc.perform(post("/api/v1/incidents/{id}/comments", firstId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"body":"Increased HikariCP max pool size to 30 as mitigation."}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUsername").value("devuser"));

        mockMvc.perform(patch("/api/v1/incidents/{id}", firstId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"RESOLVED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.comments", hasSize(1)));

        // Stats endpoint
        mockMvc.perform(get("/api/v1/incidents/stats")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.byStatus.RESOLVED").value(1))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(1));

        // RBAC: regular user cannot delete (403), admin can delete (204)
        mockMvc.perform(delete("/api/v1/incidents/{id}", firstId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/incidents/{id}", firstId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Deleted incident returns 404
        mockMvc.perform(get("/api/v1/incidents/{id}", firstId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    private String registerAndGetToken(String username, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s","password":"Password123!"}
                                """.formatted(username, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractToken(result);
    }

    private String createAdminAndGetToken(String username, String email) throws Exception {
        userRepository.save(new User(username, email, passwordEncoder.encode("AdminPass123!"), Role.ADMIN));
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"AdminPass123!"}
                                """.formatted(username)))
                .andExpect(status().isOk())
                .andReturn();
        return extractToken(result);
    }

    private long createIncident(String token, String title, String description, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"%s","priority":"%s"}
                                """.formatted(title, description, priority)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("id").asLong();
    }

    private String extractToken(MvcResult result) throws Exception {
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }
}
