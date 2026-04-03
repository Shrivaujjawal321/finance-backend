package com.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dto.request.FinancialRecordRequest;
import com.finance.dto.request.LoginRequest;
import com.finance.dto.request.RegisterRequest;
import com.finance.enums.RecordType;
import com.finance.enums.Role;
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
        adminToken = getOrCreateToken("acl_admin", "acl_admin@test.com", "pass123", Role.ADMIN);
        viewerToken = getOrCreateToken("acl_viewer", "acl_viewer@test.com", "pass123", Role.VIEWER);
        analystToken = getOrCreateToken("acl_analyst", "acl_analyst@test.com", "pass123", Role.ANALYST);
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

    private FinancialRecordRequest sampleRecord() {
        FinancialRecordRequest req = new FinancialRecordRequest();
        req.setAmount(new BigDecimal("1500.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("Salary");
        req.setDate(LocalDate.now());
        req.setDescription("Monthly salary");
        return req;
    }

    private String getOrCreateToken(String username, String email, String password, Role role) throws Exception {
        // Try to register; if already exists, just login
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setFullName(username);
        reg.setRole(role);

        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        if (regResult.getResponse().getStatus() == 201) {
            return objectMapper.readTree(regResult.getResponse().getContentAsString()).get("token").asText();
        }

        // Login if registration failed (already exists)
        LoginRequest login = new LoginRequest();
        login.setUsername(username);
        login.setPassword(password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();
    }
}
