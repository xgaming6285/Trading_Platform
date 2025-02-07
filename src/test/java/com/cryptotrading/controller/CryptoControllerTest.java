package com.cryptotrading.controller;

import com.cryptotrading.service.KrakenWebSocketService;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@ExtendWith(MockitoExtension.class)
class CryptoControllerTest {

    @Mock(lenient = true)
    private KrakenWebSocketService krakenWebSocketService;

    @Mock(lenient = true)
    private RestTemplate restTemplate;

    @InjectMocks
    private CryptoController cryptoController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cryptoController).build();
        objectMapper = new ObjectMapper();
        
        // Default mock responses
        when(krakenWebSocketService.isConnected()).thenReturn(true);
        when(krakenWebSocketService.isSubscribed(any())).thenReturn(false);
        
        // Default mock for Kraken API to return valid pairs
        String mockApiResponse = "{\"result\":{\"XXBTZUSD\":{\"wsname\":\"BTC/USD\"}}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);
    }

    @Test
    void getCryptoData_ShouldReturnLatestPrices() throws Exception {
        // Given
        Map<String, Double> mockPrices = new HashMap<>();
        mockPrices.put("BTC/USD", 50000.0);
        mockPrices.put("ETH/USD", 3000.0);
        when(krakenWebSocketService.getLatestPrices()).thenReturn(mockPrices);

        // When & Then
        mockMvc.perform(get("/api/crypto-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prices").isArray())
                .andExpect(jsonPath("$.prices[0].symbol").exists())
                .andExpect(jsonPath("$.prices[0].price").exists())
                .andExpect(jsonPath("$.prices[0].change24h").exists());

        verify(krakenWebSocketService).getLatestPrices();
    }

    @Test
    void subscribeToPair_WithValidSymbol_ShouldSubscribeSuccessfully() throws Exception {
        // Given
        String symbol = "BTC/USD";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("symbol", symbol);

        // When & Then
        mockMvc.perform(post("/api/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully subscribed to " + symbol))
                .andExpect(jsonPath("$.symbol").value(symbol));

        verify(krakenWebSocketService).subscribeToPairs(symbol);
    }

    @Test
    void subscribeToPair_WithEmptySymbol_ShouldReturnBadRequest() throws Exception {
        // Given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("symbol", "");

        // When & Then
        mockMvc.perform(post("/api/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Symbol is required"));

        verify(krakenWebSocketService, never()).subscribeToPairs(any());
    }

    @Test
    void subscribeToPair_WhenAlreadySubscribed_ShouldReturnSuccess() throws Exception {
        // Given
        String symbol = "BTC/USD";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("symbol", symbol);

        when(krakenWebSocketService.isSubscribed(symbol)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Already subscribed to " + symbol))
                .andExpect(jsonPath("$.symbol").value(symbol));

        verify(krakenWebSocketService, never()).subscribeToPairs(any());
    }

    @Test
    void subscribeToPair_WhenWebSocketNotConnected_ShouldReturnServiceUnavailable() throws Exception {
        // Given
        String symbol = "BTC/USD";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("symbol", symbol);

        when(krakenWebSocketService.isConnected()).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/api/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("WebSocket service is not available. Please try again later."));

        verify(krakenWebSocketService, never()).subscribeToPairs(any());
    }

    @Test
    void subscribeToPair_WithInvalidSymbolFormat_ShouldReturnBadRequest() throws Exception {
        // Given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("symbol", "invalid-symbol");

        // When & Then
        mockMvc.perform(post("/api/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid symbol format. Expected format: XXX/YYY"));

        verify(krakenWebSocketService, never()).subscribeToPairs(any());
    }

    @Test
    void subscribeToPair_WithUnformattedSymbol_ShouldFormatAndSubscribeSuccessfully() throws Exception {
        // Given
        String unformattedSymbol = "BTCUSD";
        String formattedSymbol = "BTC/USD";
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("symbol", unformattedSymbol);

        // When & Then
        mockMvc.perform(post("/api/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully subscribed to " + formattedSymbol))
                .andExpect(jsonPath("$.symbol").value(formattedSymbol));

        verify(krakenWebSocketService).subscribeToPairs(formattedSymbol);
    }
} 