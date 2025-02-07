package com.cryptotrading.service;

import com.cryptotrading.websocket.CryptoWebSocketEndpoint;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KrakenWebSocketService {
    private static final Logger log = LoggerFactory.getLogger(KrakenWebSocketService.class);

    @Value("${kraken.ws.url:wss://ws.kraken.com}")
    private String krakenWsUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebSocketClient webSocketClient;
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private final Map<String, Double> previousDayPrices = new ConcurrentHashMap<>();
    private boolean isConnecting = false;
    private final Set<String> subscribedPairs = ConcurrentHashMap.newKeySet();
    private final Map<String, Double> dailyChanges = new ConcurrentHashMap<>();

    private static final String[] DEFAULT_PAIRS = new String[]{
        "XBT/USD", "ETH/USD", "XPR/USD", "ADA/USD", "DOT/USD",
        "DOGE/USD", "UNI/USD", "LINK/USD", "SOL/USD", "MATIC/USD",
        "AAVE/USD", "ALGO/USD", "ATOM/USD", "LTC/USD", "BCH/USD",
        "EOS/USD", "TRX/USD", "XLM/USD", "XTZ/USD", "EURR/USDT",
        "MANA/USD", 
    };

    @PostConstruct
    public void connect() {
        if (isConnecting) return;
        isConnecting = true;

        try {
            webSocketClient = new WebSocketClient(new URI(krakenWsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    log.info("Connected to Kraken WebSocket");
                    subscribe();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        handleMessage(message);
                    } catch (Exception e) {
                        log.error("Error handling message: {}", e.getMessage());
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Kraken WebSocket connection closed: {} (code: {})", reason, code);
                    isConnecting = false;
                    if (remote) {
                        scheduleReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Kraken WebSocket error: {}", ex.getMessage());
                    isConnecting = false;
                }
            };

            webSocketClient.setConnectionLostTimeout(30);
            webSocketClient.connect();
        } catch (Exception e) {
            log.error("Error connecting to Kraken WebSocket: {}", e.getMessage());
            isConnecting = false;
        }
    }

    @Scheduled(fixedDelay = 5000)
    private void scheduleReconnect() {
        if (!isConnected()) {
            connect();
        }
    }

    private void subscribe() {
        try {
            // Subscribe to default pairs first
            subscribeToPairs(DEFAULT_PAIRS);
        } catch (Exception e) {
            log.error("Error subscribing to Kraken WebSocket: {}", e.getMessage());
        }
    }

    public void subscribeToPairs(String... pairs) {
        if (!isConnected()) {
            log.error("Cannot subscribe to pairs: WebSocket not connected");
            throw new IllegalStateException("WebSocket is not connected");
        }

        try {
            for (String pair : pairs) {
                log.info("Processing subscription request for pair: {}", pair);
                
                if (subscribedPairs.contains(pair)) {
                    log.info("Pair already subscribed: {}", pair);
                    continue;
                }

                String subscribeMessage = objectMapper.writeValueAsString(Map.of(
                    "event", "subscribe",
                    "pair", new String[]{pair},
                    "subscription", Map.of("name", "ticker")
                ));
                
                log.info("Sending subscription message for pair {}: {}", pair, subscribeMessage);
                webSocketClient.send(subscribeMessage);
                subscribedPairs.add(pair);
                log.info("Successfully sent subscription request for pair: {}", pair);
            }
        } catch (Exception e) {
            log.error("Error subscribing to pairs: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to subscribe to pairs: " + e.getMessage(), e);
        }
    }

    public boolean isSubscribed(String pair) {
        return subscribedPairs.contains(pair);
    }

    public Set<String> getSubscribedPairs() {
        return new HashSet<>(subscribedPairs);
    }

    private void handleMessage(String message) {
        try {
            JsonNode data = objectMapper.readTree(message);
            log.debug("Received message from Kraken: {}", message);
            
            if (data.has("event")) {
                String event = data.get("event").asText();
                if ("subscriptionStatus".equals(event)) {
                    String status = data.get("status").asText();
                    String pair = data.get("pair").asText();
                    if ("error".equals(status)) {
                        String errorMessage = data.has("errorMessage") ? data.get("errorMessage").asText() : "Unknown error";
                        log.error("Subscription error for {}: {}", pair, errorMessage);
                        // Remove from subscribed pairs if there was an error
                        subscribedPairs.remove(pair);
                        // Broadcast error to clients
                        CryptoWebSocketEndpoint.broadcastPriceUpdate(pair, 0.0, 0.0);
                    } else {
                        log.info("Subscription status for {}: {}", pair, status);
                    }
                    return;
                }
            }
            
            if (data.isArray() && data.size() > 2 && "ticker".equals(data.get(2).asText())) {
                String pair = data.get(3).asText();
                JsonNode tickerData = data.get(1);
                
                if (tickerData.has("c") && tickerData.get("c").size() > 0) {
                    try {
                        double currentPrice = Double.parseDouble(tickerData.get("c").get(0).asText());
                        
                        // Get the 24h opening price from the "o" field if available
                        double previousPrice;
                        if (tickerData.has("o") && tickerData.get("o").size() > 0) {
                            previousPrice = Double.parseDouble(tickerData.get("o").get(0).asText());
                        } else {
                            // Fallback to stored previous price or current price
                            previousPrice = previousDayPrices.getOrDefault(pair, currentPrice);
                        }
                        
                        double change24h = ((currentPrice - previousPrice) / previousPrice) * 100;
                        
                        dailyChanges.put(pair, change24h);
                        
                        log.info("Price update for {}: Current Price = {}, 24h Change = {}%", 
                            pair, currentPrice, String.format("%.2f", change24h));
                        
                        lastPrices.put(pair, currentPrice);
                        previousDayPrices.put(pair, previousPrice); // Update the previous day price
                        CryptoWebSocketEndpoint.broadcastPriceUpdate(pair, currentPrice, change24h);
                    } catch (NumberFormatException e) {
                        log.error("Error parsing price data for {}: {}", pair, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing message: {} - Raw message: {}", e.getMessage(), message);
        }
    }

    @PreDestroy
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
            log.info("Kraken WebSocket connection closed");
        }
    }

    public boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public void reconnect() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
        isConnecting = false;
        connect();
    }

    public Map<String, Double> getLatestPrices() {
        return new ConcurrentHashMap<>(lastPrices);
    }

    public Map<String, Double> get24hChanges() {
        return new ConcurrentHashMap<>(dailyChanges);
    }
}