package team.projectpulse.user;

import jakarta.persistence.EntityManager;
import team.projectpulse.AbstractIntegrationTest;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team.projectpulse.system.StatusCode;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Integration tests for User API endpoints")
class UserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JsonMapper jsonMapper;

    @Autowired
    EntityManager entityManager;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    @Test
    void testCheckUserExists1() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/users/exists/j.smith@abc.edu").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void testCheckUserExists2() throws Exception {
        this.mockMvc.perform(get(this.baseUrl + "/users/exists/m.long@abc.edu").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS))
                .andExpect(jsonPath("$.message").value(""))
                .andExpect(jsonPath("$.data").value(false));
    }

    /*
     * Oracle for the two login tests below: the login must not succeed or crash for an account with no
     * usable role. Such an account is refused with the app's standard 403 (flag false, code FORBIDDEN),
     * not a 500 from a NullPointerException and not a 200 that carries no token.
     */

    @Test
    @DisplayName("Login is refused with 403 for an account whose roles are null")
    void loginIsForbiddenForAnAccountWithNullRoles() throws Exception {
        setRoles("b.gates@abc.edu", null); // Seeded instructor; authentication itself does not need roles.

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
    }

    @Test
    @DisplayName("Login is refused with 403 for an account that is neither student nor instructor")
    void loginIsForbiddenForAnAccountThatIsNeitherStudentNorInstructor() throws Exception {
        setRoles("b.gates@abc.edu", "admin");

        this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic("b.gates@abc.edu", "123456")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.FORBIDDEN));
    }

    /**
     * Changes a seeded user's roles and flushes, so the login request in the same (rolled-back) test
     * transaction loads the changed row.
     */
    private void setRoles(String username, String roles) {
        PeerEvaluationUser user = this.entityManager
                .createQuery("select u from PeerEvaluationUser u where u.username = :username", PeerEvaluationUser.class)
                .setParameter("username", username)
                .getSingleResult();
        user.setRoles(roles);
        this.entityManager.flush();
    }

}
