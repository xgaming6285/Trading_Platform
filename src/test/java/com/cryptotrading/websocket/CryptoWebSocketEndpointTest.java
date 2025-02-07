package com.cryptotrading.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoWebSocketEndpointTest {

    private CryptoWebSocketEndpoint endpoint;
    private ObjectMapper objectMapper;

    @Mock
    private Session session;

    @Mock
    private RemoteEndpoint.Basic basicRemote;

    @BeforeEach
    void setUp() {
        endpoint = new CryptoWebSocketEndpoint();
        objectMapper = new ObjectMapper();
    }

    @Test
    void onOpen_ShouldAddSessionAndDisableTimeout() throws IOException {
        // Given
        when(session.getId()).thenReturn("test-session-id");

        // When
        endpoint.onOpen(session);

        // Then
        verify(session).setMaxIdleTimeout(0);
        verify(session).getId();
    }

    @Test
    void onMessage_WithValidSubscribeMessage_ShouldHandleSubscription() throws IOException {
        // Given
        when(session.getBasicRemote()).thenReturn(basicRemote);
        String subscribeMessage = objectMapper.writeValueAsString(Map.of("type", "SUBSCRIBE"));

        // When
        endpoint.onMessage(subscribeMessage, session);

        // Then
        verify(basicRemote).sendText(argThat(message -> 
            message.contains("SUBSCRIPTION_CONFIRMED") && 
            message.contains("Successfully subscribed to price updates")
        ));
    }

    @Test
    void onMessage_WithInvalidJson_ShouldHandleError() throws IOException {
        // Given
        String invalidMessage = "invalid json";

        // When
        endpoint.onMessage(invalidMessage, session);

        // Then
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    void onClose_ShouldRemoveSession() {
        // Given
        when(session.getId()).thenReturn("test-session-id");

        // When
        endpoint.onClose(session);

        // Then
        verify(session).getId();
    }

    @Test
    void onError_ShouldLogError() {
        // Given
        when(session.getId()).thenReturn("test-session-id");
        Throwable error = new RuntimeException("Test error");

        // When
        endpoint.onError(session, error);

        // Then
        verify(session).getId();
    }

    @Test
    void broadcastPriceUpdate_ShouldSendToAllSessions() throws IOException {
        // Given
        when(session.getBasicRemote()).thenReturn(basicRemote);
        when(session.isOpen()).thenReturn(true);
        String symbol = "BTC/USD";
        double price = 50000.0;
        double change24h = 2.5;

        // When
        endpoint.onOpen(session); // Add a session first
        CryptoWebSocketEndpoint.broadcastPriceUpdate(symbol, price, change24h);

        // Then
        verify(basicRemote).sendText(argThat(message ->
            message.contains("PRICE_UPDATE") &&
            message.contains(symbol) &&
            message.contains(String.valueOf(price)) &&
            message.contains(String.valueOf(change24h))
        ));
    }

    @Test
    void broadcastPriceUpdate_WhenSessionClosed_ShouldSkip() throws IOException {
        // Given
        when(session.isOpen()).thenReturn(false);
        String symbol = "BTC/USD";
        double price = 50000.0;
        double change24h = 2.5;

        // When
        endpoint.onOpen(session);
        CryptoWebSocketEndpoint.broadcastPriceUpdate(symbol, price, change24h);

        // Then
        verify(basicRemote, never()).sendText(anyString());
    }

    @Test
    void broadcastPriceUpdate_WhenSendFails_ShouldHandleError() throws IOException {
        // Given
        when(session.getBasicRemote()).thenReturn(basicRemote);
        when(session.isOpen()).thenReturn(true);
        when(session.getId()).thenReturn("test-session-id");
        doThrow(new IOException("Send failed")).when(basicRemote).sendText(anyString());
        
        String symbol = "BTC/USD";
        double price = 50000.0;
        double change24h = 2.5;

        // When
        endpoint.onOpen(session);
        CryptoWebSocketEndpoint.broadcastPriceUpdate(symbol, price, change24h);

        // Then
        verify(basicRemote).sendText(anyString());
        verify(session, atLeastOnce()).getId();
    }

    @Test
    void onMessage_WithNonSubscribeMessage_ShouldNotSendConfirmation() throws IOException {
        // Given
        String nonSubscribeMessage = objectMapper.writeValueAsString(Map.of("type", "OTHER"));

        // When
        endpoint.onMessage(nonSubscribeMessage, session);

        // Then
        verify(basicRemote, never()).sendText(anyString());
    }
} 