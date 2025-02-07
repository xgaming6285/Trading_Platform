package com.cryptotrading.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@ServerEndpoint(value = "/ws", configurator = WebSocketConfig.class)
public class CryptoWebSocketEndpoint {
    private static final Logger log = LoggerFactory.getLogger(CryptoWebSocketEndpoint.class);
    private static final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, Double> lastPrices = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        log.info("New WebSocket connection established. Session ID: {}", session.getId());
        session.setMaxIdleTimeout(0); // Disable idle timeout
        sendLastPrices(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            Map<String, Object> data = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            if ("SUBSCRIBE".equals(data.get("type"))) {
                handleSubscription(session);
            }
        } catch (IOException e) {
            log.error("Error processing WebSocket message", e);
        }
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        log.info("WebSocket connection closed. Session ID: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error for session {}: {}", session.getId(), error.getMessage());
    }

    private void handleSubscription(Session session) {
        try {
            log.info("Subscription request received from session: {}", session.getId());
            // Send subscription confirmation
            sendMessage(session, Map.of(
                "type", "SUBSCRIPTION_CONFIRMED",
                "message", "Successfully subscribed to price updates"
            ));
        } catch (IOException e) {
            log.error("Error handling subscription", e);
            try {
                sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Failed to subscribe: " + e.getMessage()
                ));
            } catch (IOException ex) {
                log.error("Error sending error message", ex);
            }
        }
    }

    private void sendLastPrices(Session session) {
        lastPrices.forEach((symbol, price) -> {
            try {
                sendMessage(session, Map.of(
                    "type", "PRICE_UPDATE",
                    "symbol", symbol,
                    "price", price,
                    "change24h", 0.0
                ));
            } catch (IOException e) {
                log.error("Error sending last prices to session {}: {}", session.getId(), e.getMessage());
            }
        });
    }

    public static void broadcastPriceUpdate(String symbol, double price, double change24h) {
        String message = createPriceUpdateMessage(symbol, price, change24h);
        sessions.forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(message);
                } catch (IOException e) {
                    log.error("Error sending message to session {}: {}", session.getId(), e.getMessage());
                }
            }
        });
    }

    private static String createPriceUpdateMessage(String symbol, double price, double change24h) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                "type", "PRICE_UPDATE",
                "symbol", symbol,
                "price", price,
                "change24h", change24h
            ));
        } catch (IOException e) {
            log.error("Error creating price update message", e);
            return "";
        }
    }

    private void sendMessage(Session session, Map<String, Object> message) throws IOException {
        session.getBasicRemote().sendText(objectMapper.writeValueAsString(message));
    }
}