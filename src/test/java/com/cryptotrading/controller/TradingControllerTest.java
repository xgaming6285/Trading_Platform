package com.cryptotrading.controller;

import com.cryptotrading.service.TradingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TradingControllerTest {

    @Mock
    private TradingService tradingService;

    @InjectMocks
    private TradingController tradingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tradingController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void executeTrade_WithValidRequest_ShouldExecuteSuccessfully() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("type", "BUY");
        request.put("symbol", "BTC/USD");
        request.put("amount", 1.0);
        request.put("price", 50000.0);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("balance", 5000.0);

        when(tradingService.executeTrade(anyString(), anyString(), anyDouble(), anyDouble()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.balance").value(5000.0));

        verify(tradingService).executeTrade("BUY", "BTC/USD", 1.0, 50000.0);
    }

    @Test
    void executeTrade_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("type", "BUY");
        request.put("symbol", "BTC/USD");
        request.put("amount", -1.0);
        request.put("price", 50000.0);

        when(tradingService.executeTrade(anyString(), anyString(), anyDouble(), anyDouble()))
                .thenThrow(new IllegalArgumentException("Amount must be greater than 0"));

        // When & Then
        mockMvc.perform(post("/api/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Amount must be greater than 0"));
    }

    @Test
    void executeTrade_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Given
        Map<String, Object> request = new HashMap<>();
        request.put("type", "BUY");
        request.put("symbol", "BTC/USD");
        request.put("amount", 1.0);
        request.put("price", 50000.0);

        when(tradingService.executeTrade(anyString(), anyString(), anyDouble(), anyDouble()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/api/trade")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void resetAccount_ShouldResetSuccessfully() throws Exception {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("balance", 10000.0);
        response.put("portfolio", new HashMap<>());

        when(tradingService.resetAccount()).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.0))
                .andExpect(jsonPath("$.portfolio").exists());

        verify(tradingService).resetAccount();
    }

    @Test
    void resetAccount_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(tradingService.resetAccount()).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(post("/api/reset"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void getInitialData_ShouldReturnData() throws Exception {
        // Given
        Map<String, Object> response = new HashMap<>();
        response.put("balance", 10000.0);
        response.put("portfolio", new HashMap<>());

        when(tradingService.getUpdatedState()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/initial-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(10000.0))
                .andExpect(jsonPath("$.portfolio").exists());

        verify(tradingService).getUpdatedState();
    }

    @Test
    void getInitialData_WhenServiceThrowsException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(tradingService.getUpdatedState()).thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        mockMvc.perform(get("/api/initial-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }
} 