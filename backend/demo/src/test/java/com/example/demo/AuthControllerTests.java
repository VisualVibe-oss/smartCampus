package com.example.demo;

import com.example.demo.repository.AuthSessionRepository;
import com.example.demo.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:smartcampus-auth-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthSessionRepository authSessionRepository;

    @BeforeEach
    void resetDatabase() {
        authSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginWithValidCredentialsReturnsTokenAndGrantsAccess() throws Exception {
        createUser("etudiant@univ.test", "secret123", true);

        MvcResult result = login("etudiant@univ.test", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresAt").isString())
                .andExpect(jsonPath("$.user.email").value("etudiant@univ.test"))
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.user.enabled").value(true))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("etudiant@univ.test"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(authSessionRepository.findAll())
                .hasSize(1)
                .noneMatch(session -> session.getTokenHash().equals(token));
    }

    @Test
    void loginAcceptsEmailCaseAndSurroundingSpaces() throws Exception {
        createUser("Etudiant@Univ.Test", "secret123", true);

        login("  etudiant@univ.test  ", "secret123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("Etudiant@Univ.Test"));
    }

    @Test
    void wrongPasswordReturnsUnderstandableUnauthorizedError() throws Exception {
        createUser("etudiant@univ.test", "secret123", true);

        login("etudiant@univ.test", "mauvaisSecret")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Adresse e-mail ou mot de passe incorrect."))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.details").isEmpty());

        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    void unknownEmailReturnsTheSameUnauthorizedError() throws Exception {
        login("inconnu@univ.test", "secret123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Adresse e-mail ou mot de passe incorrect."));
    }

    @Test
    void missingEmailOrPasswordReturnsBadRequestWithFieldDetails() throws Exception {
        postLogin("""
                {"password":"secret123"}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.email").isString());

        postLogin("""
                {"email":"etudiant@univ.test"}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").isString());

        postLogin("""
                {"email":"  ","password":"   "}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").isString())
                .andExpect(jsonPath("$.details.password").isString());

        postLogin("{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").isString())
                .andExpect(jsonPath("$.details.password").isString());
    }

    @Test
    void invalidEmailFormatReturnsBadRequest() throws Exception {
        login("pas-un-email", "secret123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.email").isString());
    }

    @Test
    void disabledAccountReturnsForbidden() throws Exception {
        createUser("etudiant@univ.test", "secret123", false);

        login("etudiant@univ.test", "secret123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Ce compte est désactivé. Contactez l'administration."))
                .andExpect(jsonPath("$.details").isEmpty());

        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    void tokenIsRequiredAndLogoutInvalidatesIt() throws Exception {
        createUser("etudiant@univ.test", "secret123", true);
        String token = JsonPath.read(login("etudiant@univ.test", "secret123")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.token");

        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").isString());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer jeton-invalide"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Session invalide ou expirée. Veuillez vous reconnecter."));
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Basic abc"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    void deletingUserRemovesItsSessions() throws Exception {
        long userId = createUser("etudiant@univ.test", "secret123", true);
        MvcResult result = login("etudiant@univ.test", "secret123")
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");

        mvc.perform(delete("/api/users/{id}", userId)).andExpect(status().isNoContent());

        assertThat(userRepository.existsById(userId)).isFalse();
        assertThat(authSessionRepository.count()).isZero();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private long createUser(String email, String password, boolean enabled) throws Exception {
        MvcResult result = mvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s","fullName":"Amina Benali",
                                 "role":"STUDENT","enabled":%s}
                                """.formatted(email, password, enabled)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        return postLogin("""
                {"email":"%s","password":"%s"}
                """.formatted(email, password));
    }

    private org.springframework.test.web.servlet.ResultActions postLogin(String json) throws Exception {
        return mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
