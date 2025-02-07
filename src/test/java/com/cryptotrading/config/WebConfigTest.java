package com.cryptotrading.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.filter.CorsFilter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.servlet.FilterChain;
import org.mockito.Mockito;

class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    void corsFilter_ShouldConfigureCorrectly() throws Exception {
        // Given
        CorsFilter corsFilter = webConfig.corsFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = Mockito.mock(FilterChain.class);

        // When - simulate a CORS preflight request
        request.setMethod("OPTIONS");
        request.addHeader("Origin", "http://localhost:3000");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "content-type");

        corsFilter.doFilter(request, response, filterChain);

        // Then
        assertEquals("http://localhost:3000", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        
        String allowMethods = response.getHeader("Access-Control-Allow-Methods");
        assertNotNull(allowMethods, "Access-Control-Allow-Methods should not be null");
        assertTrue(allowMethods.contains("GET"));
        assertTrue(allowMethods.contains("POST"));
        assertTrue(allowMethods.contains("PUT"));
        assertTrue(allowMethods.contains("DELETE"));
        
        String allowHeaders = response.getHeader("Access-Control-Allow-Headers");
        assertNotNull(allowHeaders, "Access-Control-Allow-Headers should not be null");
        assertTrue(allowHeaders.contains("*") || allowHeaders.contains("content-type"),
                "Should allow all headers or at least content-type");
    }
} 