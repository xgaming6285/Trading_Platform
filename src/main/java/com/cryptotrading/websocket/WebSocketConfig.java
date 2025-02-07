package com.cryptotrading.websocket;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class WebSocketConfig extends ServerEndpointConfig.Configurator {
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        return super.getEndpointInstance(endpointClass);
    }

    @Override
    public boolean checkOrigin(String originHeaderValue) {
        // Allow connections from localhost:3000
        return originHeaderValue == null || 
               originHeaderValue.equals("http://localhost:3000");
    }
} 