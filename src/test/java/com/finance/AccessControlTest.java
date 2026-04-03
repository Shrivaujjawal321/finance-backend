package com.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.FinancialRecordRequest;
import com.finance.dto.request.LoginRequest;
import com.finance.enums.RecordType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String viewerToken;
    private String analystToken;

    @BeforeEach
    void setUp() throws Exception {
        // Use seeded users from DataSeeder (admin, analyst, viewer)
        adminToken = loginAndGetToken("admin", "admin123");
        viewerToken = loginAndGetToken("viewer", "viewer123");
        analystToken = loginAndGetToken("analyst", "analyst123");
    }

    @Test
    @Order(1)
    void admin_canCreateRecord() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRecord())))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(2)
    void viewer_canReadRecords() throws Exception {
        mockMvc.perform(get("/api/records")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void viewer_cannotCreateRecord() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRecord())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(4)
    void viewer_cannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(5)
    void analyst_canAccessDashboard() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary")
                        .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());
    }

    @Test
    @Order(6)
    void analyst_cannotCreateRecord() throws Exception {
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRecord())))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(7)
    void viewer_cannotManageUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(8)
    void unauthenticated_cannotAccessRecords() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Order(9)
    void publicRegistration_alwaysAssignsViewer() throws Exception {
        // Even if someone tries to pass a role, registration should assign VIEWER
        String json = "{\"username\":\"hacker\",\"email\":\"h@test.com\",\"password\":\"pass123\",\"fullName\":\"Hacker\",\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assert body.contains("\"role\":\"VIEWER\"") : "Registration must always assign VIEWER role";
                });
    }

    private FinancialRecordRequest sampleRecord() {
        FinancialRecordRequest req = new FinancialRecordRequest();
        req.setAmount(new BigDecimal("1500.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("Salary");
        req.setDate(LocalDate.now());
        req.setDescription("Monthly salary");
        return req;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
}
