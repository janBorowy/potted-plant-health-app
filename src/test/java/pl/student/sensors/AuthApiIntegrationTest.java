package pl.student.sensors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.student.sensors.generated.model.LoginRequest;
import pl.student.sensors.generated.model.LoginResponse;
import pl.student.sensors.generated.model.PasswordChange;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pl.student.sensors.TestUtil.OPEN_API_VALIDATOR;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserDetailsManager userDetailsManager;

    @Test
    void loginReturnsSignedBearerTokenForValidCredentials() throws Exception {
        when(userDetailsManager.loadUserByUsername("admin"))
                .thenReturn(User.withUsername("admin")
                        .password(passwordEncoder.encode("secretPass"))
                        .roles("USER")
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new LoginRequest("admin", "secretPass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        when(userDetailsManager.loadUserByUsername("admin"))
                .thenReturn(User.withUsername("admin")
                        .password(passwordEncoder.encode("secretPass"))
                        .roles("USER")
                        .build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new LoginRequest("admin", "wrongPass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void loginReturnsBadRequestWhenUsernameIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"secretPass\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBadRequestWhenPasswordIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsBadRequestWhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin")
    void changePasswordUpdatesAuthenticatedUsersPassword() throws Exception {
        when(userDetailsManager.loadUserByUsername("admin"))
                .thenReturn(User.withUsername("admin")
                        .password(passwordEncoder.encode("secretPass"))
                        .build());

        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new PasswordChange("secretPass", "newSecretPass"))))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        verify(userDetailsManager).updateUser(argThat(user ->
                user.getUsername().equals("admin")
                        && passwordEncoder.matches("newSecretPass", user.getPassword())));
    }

    @Test
    @WithMockUser(username = "admin")
    void changePasswordReturnsForbiddenWhenCurrentPasswordIsIncorrect() throws Exception {
        when(userDetailsManager.loadUserByUsername("admin"))
                .thenReturn(User.withUsername("admin")
                        .password(passwordEncoder.encode("secretPass"))
                        .build());

        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new PasswordChange("wrongPass", "newSecretPass"))))
                .andExpect(status().isForbidden())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        verify(userDetailsManager, never()).updateUser(ArgumentMatchers.any());
    }

    @Test
    void changePasswordReturnsUnauthorizedWhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new PasswordChange("secretPass", "newSecretPass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));
    }

    @Test
    void logoutRevokesTokenAndRejectsSubsequentAuthenticatedRequests() throws Exception {
        when(userDetailsManager.loadUserByUsername("admin"))
                .thenReturn(User.withUsername("admin")
                        .password(passwordEncoder.encode("secretPass"))
                        .roles("USER")
                        .build());

        var login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new LoginRequest("admin", "secretPass"))))
                .andExpect(status().isOk())
                .andReturn();
        var token = TestUtil.toObject(login.getResponse().getContentAsString(), LoginResponse.class)
                .getAccessToken();
        var bearer = "Bearer " + token;

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer))
                .andExpect(status().isNoContent())
                .andExpect(openApi().isValid(OPEN_API_VALIDATOR));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", bearer))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/auth/password")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestUtil.toJson(new PasswordChange("secretPass", "newSecretPass"))))
                .andExpect(status().isUnauthorized());
    }
}
