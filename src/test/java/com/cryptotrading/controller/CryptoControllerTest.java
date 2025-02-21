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

// Test class for CryptoController using Mockito and Spring MVC test framework
@ExtendWith(MockitoExtension.class) // Enable Mockito annotations
class CryptoControllerTest {
    // Mock dependencies that will be injected into the controller
    @Mock(lenient = true)
    private KrakenWebSocketService krakenWebSocketService; // Mock WebSocket service

    @Mock(lenient = true)
    private RestTemplate restTemplate; // Mock REST client for external API calls

    @InjectMocks
    private CryptoController cryptoController; // Controller instance with injected mocks
    // Test utilities
    private MockMvc mockMvc; // MVC test framework
    private ObjectMapper objectMapper; // JSON serialization/deserialization

    @BeforeEach
    void setUp() {
        // Initialize MVC test framework with our controller
        mockMvc = MockMvcBuilders.standaloneSetup(cryptoController).build();
        objectMapper = new ObjectMapper();
        
        // Configure default mock behaviors:
        // - WebSocket connection is active by default
        when(krakenWebSocketService.isConnected()).thenReturn(true);
        // - Default to not subscribed for any symbol
        when(krakenWebSocketService.isSubscribed(any())).thenReturn(false);
        
        // Mock Kraken API response for valid trading pairs
        String mockApiResponse = "{\"result\":{\"XXBTZUSD\":{\"wsname\":\"BTC/USD\"}}}";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockApiResponse);
    }

    // Test case: GET request to fetch crypto data should return latest prices
    @Test
    void getCryptoData_ShouldReturnLatestPrices() throws Exception {
        // Setup mock price data
        Map<String, Double> mockPrices = new HashMap<>();
        mockPrices.put("BTC/USD", 50000.0);
        mockPrices.put("ETH/USD", 3000.0);
        when(krakenWebSocketService.getLatestPrices()).thenReturn(mockPrices);

        // Perform GET request and verify response structure
        mockMvc.perform(get("/api/crypto-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prices").isArray()) // Prices should be an array
                .andExpect(jsonPath("$.prices[0].symbol").exists()) // Each item has symbol
                .andExpect(jsonPath("$.prices[0].price").exists()) // Each item has price
                .andExpect(jsonPath("$.prices[0].change24h").exists()); // Each item has 24h change

        verify(krakenWebSocketService).getLatestPrices(); // Verify service method was called
    }

    // Test case: Valid subscription request should return success
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

        verify(krakenWebSocketService).subscribeToPairs(symbol); // Verify subscription attempt
    }

    // Test case: Empty symbol should return 400 Bad Request
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

        verify(krakenWebSocketService, never()).subscribeToPairs(any()); // Ensure no subscription
    }

    // Test case: Already subscribed symbol should return success without resubscribing
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

        verify(krakenWebSocketService, never()).subscribeToPairs(any()); // No subscription call
    }

    // Test case: WebSocket disconnect should return 503 Service Unavailable
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

    // Test case: Invalid symbol format should return 400 Bad Request
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

    // Test case: Auto-formatting of valid but unformatted symbol
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

        verify(krakenWebSocketService).subscribeToPairs(formattedSymbol); // Verify formatted symbol used
    }
} 
