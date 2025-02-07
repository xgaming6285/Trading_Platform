package com.cryptotrading.service;

import com.cryptotrading.websocket.CryptoWebSocketEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KrakenWebSocketServiceTest {

    private KrakenWebSocketService krakenWebSocketService;
    
    @Mock
    private WebSocketClient webSocketClient;
    
    @Mock
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        krakenWebSocketService = new KrakenWebSocketService();
        ReflectionTestUtils.setField(krakenWebSocketService, "webSocketClient", webSocketClient);
        ReflectionTestUtils.setField(krakenWebSocketService, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(krakenWebSocketService, "krakenWsUrl", "wss://ws.kraken.com");
        ReflectionTestUtils.setField(krakenWebSocketService, "subscribedPairs", new HashSet<String>());
    }

    @Test
    void subscribeToPairs_WhenNotConnected_ShouldThrowException() {
        // Given
        when(webSocketClient.isOpen()).thenReturn(false);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> krakenWebSocketService.subscribeToPairs("BTC/USD"));
        assertEquals("WebSocket is not connected", exception.getMessage());
    }

    @Test
    void subscribeToPairs_WhenAlreadySubscribed_ShouldSkipSubscription() throws Exception {
        // Given
        when(webSocketClient.isOpen()).thenReturn(true);
        @SuppressWarnings("unchecked")
        Set<String> subscribedPairs = (Set<String>) ReflectionTestUtils.getField(krakenWebSocketService, "subscribedPairs");
        assertNotNull(subscribedPairs, "subscribedPairs should not be null");
        subscribedPairs.add("BTC/USD");

        // When
        krakenWebSocketService.subscribeToPairs("BTC/USD");

        // Then
        verify(webSocketClient, never()).send(anyString());
    }

    @Test
    void subscribeToPairs_WhenValidPair_ShouldSubscribeSuccessfully() throws Exception {
        // Given
        when(webSocketClient.isOpen()).thenReturn(true);
        String expectedMessage = "{\"event\":\"subscribe\",\"pair\":[\"BTC/USD\"],\"subscription\":{\"name\":\"ticker\"}}";
        when(objectMapper.writeValueAsString(any())).thenReturn(expectedMessage);

        // When
        krakenWebSocketService.subscribeToPairs("BTC/USD");

        // Then
        verify(webSocketClient).send(expectedMessage);
        assertTrue(krakenWebSocketService.isSubscribed("BTC/USD"));
    }

    @Test
    void handleMessage_WhenValidTickerMessage_ShouldUpdatePrices() throws Exception {
        // Given
        String message = """
            [
              123,
              {
                "c": ["50000.0"],
                "o": ["48000.0"]
              },
              "ticker",
              "BTC/USD"
            ]
            """;
        JsonNode messageNode = new ObjectMapper().readTree(message);
        when(objectMapper.readTree(message)).thenReturn(messageNode);

        try (MockedStatic<CryptoWebSocketEndpoint> mockedStatic = mockStatic(CryptoWebSocketEndpoint.class)) {
            // When
            ReflectionTestUtils.invokeMethod(krakenWebSocketService, "handleMessage", message);

            // Then
            Map<String, Double> lastPrices = krakenWebSocketService.getLatestPrices();
            assertEquals(50000.0, lastPrices.get("BTC/USD"));
            mockedStatic.verify(() -> 
                CryptoWebSocketEndpoint.broadcastPriceUpdate(
                    eq("BTC/USD"), 
                    eq(50000.0), 
                    doubleThat(change -> Math.abs(change - 4.166666666666667) < 0.0001)
                )
            );
        }
    }

    @Test
    void handleMessage_WhenSubscriptionError_ShouldRemovePair() throws Exception {
        // Given
        String message = """
            {
              "event": "subscriptionStatus",
              "status": "error",
              "pair": "BTC/USD",
              "errorMessage": "Invalid pair"
            }
            """;
        JsonNode messageNode = new ObjectMapper().readTree(message);
        when(objectMapper.readTree(message)).thenReturn(messageNode);
        @SuppressWarnings("unchecked")
        Set<String> subscribedPairs = (Set<String>) ReflectionTestUtils.getField(krakenWebSocketService, "subscribedPairs");
        assertNotNull(subscribedPairs, "subscribedPairs should not be null");
        subscribedPairs.add("BTC/USD");

        try (MockedStatic<CryptoWebSocketEndpoint> mockedStatic = mockStatic(CryptoWebSocketEndpoint.class)) {
            // When
            ReflectionTestUtils.invokeMethod(krakenWebSocketService, "handleMessage", message);

            // Then
            assertFalse(krakenWebSocketService.isSubscribed("BTC/USD"));
            mockedStatic.verify(() -> 
                CryptoWebSocketEndpoint.broadcastPriceUpdate("BTC/USD", 0.0, 0.0)
            );
        }
    }

    @Test
    void reconnect_ShouldCloseAndReconnect() {
        // When
        krakenWebSocketService.reconnect();

        // Then
        verify(webSocketClient).close();
        Boolean isConnecting = (Boolean) ReflectionTestUtils.getField(krakenWebSocketService, "isConnecting");
        assertNotNull(isConnecting, "isConnecting should not be null");
        assertTrue(isConnecting, "isConnecting should be true as reconnection is initiated");
    }

    @Test
    void disconnect_ShouldCloseWebSocketClient() {
        // When
        krakenWebSocketService.disconnect();

        // Then
        verify(webSocketClient).close();
    }
} 