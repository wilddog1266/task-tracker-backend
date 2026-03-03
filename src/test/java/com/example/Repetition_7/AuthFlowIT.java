package com.example.Repetition_7;

import com.example.Repetition_7.entity.UserEntity;
import com.example.Repetition_7.entity.roles.UserRole;
import com.example.Repetition_7.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
public class AuthFlowIT extends BaseIntegrationTest {
    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private final static String USERNAME = "testuser";
    private final static String PASSWORD = "password123";

    private final static String ADMIN_USERNAME = "admin";
    private final static String ADMIN_PASSWORD = "admin123";


    @BeforeEach
    public void setUpUser() {
        userRepository.findByUsername(USERNAME).ifPresent(entity -> userRepository.delete(entity));

        UserEntity user = new UserEntity();
        user.setUsername(USERNAME);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.USER);
        userRepository.save(user);
        userRepository.flush();
    }

    @BeforeEach
    public void setUpAdmin() {
        userRepository.findByUsername(ADMIN_USERNAME).ifPresent(entity -> userRepository.delete(entity));

        UserEntity user = new UserEntity();
        user.setUsername(ADMIN_USERNAME);
        user.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
        userRepository.flush();
    }

    @Test
    public void login_ShouldReturnAccessToken_andRefreshCookie() throws Exception {
       AuthTokens tokens = doLogin(USERNAME, PASSWORD);

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshTokenValue()).isNotNull();
        assertThat(tokens.setCookie().contains("HttpOnly"));
    }

    @Test
    public void refresh_ShouldRotateRefreshCookie_andReturnNewAccessToken() throws Exception {
        AuthTokens first = doLogin(USERNAME, PASSWORD);
        AuthTokens second = doRefresh(first.refreshTokenValue());

        assertThat(second.accessToken()).isNotBlank();
        assertThat(second.refreshTokenValue()).isNotNull();

        assertThat(second.refreshTokenValue()).isNotEqualTo(first.refreshTokenValue());
    }

    @Test
    public void tasks_WithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/tasks")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void tasks_WithUserToken_ShouldReturn200() throws Exception {
        AuthTokens login = doLogin(USERNAME, PASSWORD);

        mockMvc.perform(get("/api/tasks")
                .header("Authorization", "Bearer " + login.accessToken())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void adminUser_WithUserToken_ShouldReturn403() throws Exception {
        AuthTokens login = doLogin(USERNAME, PASSWORD);

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + login.accessToken())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    public void users_WithAdminToken_ShouldReturn200() throws Exception {
        AuthTokens login = doLogin(ADMIN_USERNAME, ADMIN_PASSWORD);

        mockMvc.perform(get("/api/admin/users")
                .header("Authorization", "Bearer " + login.accessToken())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private AuthTokens doLogin(String username, String password) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if(setCookie == null) {
            throw new AssertionError("Set-Cookie header is missing");
        }
        if(!setCookie.contains("HttpOnly")) {
            throw new AssertionError("refreshToken cookie must be HttpOnly: " + setCookie);
        }
        if(!setCookie.contains("Path=/api/auth")) {
            throw new AssertionError("refreshToken cookie path must be /api/auth: " + setCookie);
        }

        String refreshValue = extractCookieValue(setCookie, "refreshToken");
        String access = extractAccessToken(responseBody);

        return new AuthTokens(access, refreshValue, setCookie);
    }

    private AuthTokens doRefresh(String refreshTokenValue) throws Exception {
        Cookie cookie = new Cookie("refreshToken", refreshTokenValue);
        cookie.setPath("/api/auth");

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("refreshToken=")))
                .andReturn();

        String setCookie = result.getResponse().getHeader("Set-Cookie");
        if(setCookie == null) {
            throw new AssertionError("Set-Cookie header is missing on refresh");
        }

        String newRefreshValue = extractCookieValue(setCookie, "refreshToken");
        String newAccess = extractAccessToken(result.getResponse().getContentAsString());

        return new AuthTokens(newAccess, newRefreshValue, setCookie);
    }

    private String extractAccessToken(String responseBody) {
        String key = "\"accessToken\":\"";
        int start = responseBody.indexOf(key);
        if(start == -1) {
            throw new AssertionError("accessToken not found in response");
        }
        start += key.length();

        int end = responseBody.indexOf('"', start);
        if(end == -1) {
            throw new AssertionError("malformed JSON: " + responseBody);
        }

        return responseBody.substring(start, end);
    }

    private String extractCookieValue(String setCookie, String cookieName) {
        String prefix = cookieName + "=";
        int start = setCookie.indexOf(prefix);
        if (start == -1) {
            throw new AssertionError(cookieName + " not found in Set-Cookie: " + setCookie);
        }
        start += prefix.length();

        int end = setCookie.indexOf(';', start);
        if (end == -1) end = setCookie.length();

        String value = setCookie.substring(start, end);
        if (value.isBlank()) {
            throw new AssertionError(cookieName + " value is blank in Set-Cookie: " + setCookie);
        }
        return value;
    }

    private record AuthTokens(String accessToken, String refreshTokenValue, String setCookie) {}

}
