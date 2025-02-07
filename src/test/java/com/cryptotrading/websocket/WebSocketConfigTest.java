package com.cryptotrading.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketConfigTest {

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig();
    }

    @Test
    void getEndpointInstance_ShouldCreateNewInstance() throws InstantiationException {
        // Given
        Class<TestEndpoint> endpointClass = TestEndpoint.class;

        // When
        TestEndpoint instance = webSocketConfig.getEndpointInstance(endpointClass);

        // Then
        assertNotNull(instance, "Endpoint instance should not be null");
        assertTrue(instance instanceof TestEndpoint, "Instance should be of TestEndpoint type");
    }

    @Test
    void checkOrigin_WithAllowedOrigin_ShouldReturnTrue() {
        // When & Then
        assertTrue(webSocketConfig.checkOrigin("http://localhost:3000"),
                "Should allow connection from localhost:3000");
    }

    @Test
    void checkOrigin_WithNullOrigin_ShouldReturnTrue() {
        // When & Then
        assertTrue(webSocketConfig.checkOrigin(null),
                "Should allow connection with null origin");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://localhost:8080",
        "http://other-domain.com",
        "https://localhost:3000",
        "http://localhost:3001"
    })
    void checkOrigin_WithDisallowedOrigins_ShouldReturnFalse(String origin) {
        // When & Then
        assertFalse(webSocketConfig.checkOrigin(origin),
                "Should not allow connection from " + origin);
    }

    // Test endpoint class for getEndpointInstance test
    public static class TestEndpoint {
        // Add public no-args constructor
        public TestEndpoint() {
            // Empty constructor
        }
    }
} 