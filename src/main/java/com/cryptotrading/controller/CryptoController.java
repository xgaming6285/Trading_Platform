package com.cryptotrading.controller;

import com.cryptotrading.service.KrakenWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowedHeaders = "*", methods = {
    RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
    RequestMethod.DELETE, RequestMethod.OPTIONS
}, allowCredentials = "true")
public class CryptoController {
    private static final Logger log = LoggerFactory.getLogger(CryptoController.class);
    private static final String KRAKEN_API_URL = "https://api.kraken.com/0/public/AssetPairs";
    private static Set<String> validPairs = new HashSet<>();
    private static long lastValidPairsUpdate = 0;
    private static final long PAIRS_CACHE_DURATION = 3600000; // 1 hour in milliseconds

    @Autowired
    private KrakenWebSocketService krakenWebSocketService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private void updateValidPairs() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastValidPairsUpdate > PAIRS_CACHE_DURATION || validPairs.isEmpty()) {
            try {
                String response = restTemplate.getForObject(KRAKEN_API_URL, String.class);
                JsonNode root = objectMapper.readTree(response);
                JsonNode result = root.get("result");
                
                Set<String> newPairs = new HashSet<>();
                result.fields().forEachRemaining(entry -> {
                    JsonNode pair = entry.getValue();
                    if (pair.has("wsname")) {
                        newPairs.add(pair.get("wsname").asText());
                    }
                });
                
                validPairs = newPairs;
                lastValidPairsUpdate = currentTime;
                log.info("Updated valid pairs from Kraken API. Total pairs: {}", validPairs.size());
            } catch (Exception e) {
                log.error("Error updating valid pairs from Kraken API: {}", e.getMessage());
            }
        }
    }

    private boolean isValidKrakenPair(String symbol) {
        try {
            String response = restTemplate.getForObject(KRAKEN_API_URL, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.get("result");
            
            if (result != null && result.isObject()) {
                Iterator<JsonNode> elements = result.elements();
                while (elements.hasNext()) {
                    JsonNode pair = elements.next();
                    if (pair.has("wsname") && pair.get("wsname").asText().equals(symbol)) {
                        return true;
                    }
                }
            }
            
            // If we can't find the pair but it has valid format, consider it valid
            return isValidSymbolFormat(symbol);
        } catch (Exception e) {
            log.error("Error validating pair with Kraken API: {}", e.getMessage());
            // If we can't validate with Kraken, assume it's valid if it passes our format check
            return isValidSymbolFormat(symbol);
        }
    }

    @GetMapping("/crypto-data")
    public ResponseEntity<Map<String, Object>> getCryptoData() {
        Map<String, Object> response = new HashMap<>();
        
        // Get latest prices and 24h changes from the WebSocket service
        Map<String, Double> latestPrices = krakenWebSocketService.getLatestPrices();
        Map<String, Double> dailyChanges = krakenWebSocketService.get24hChanges();
        
        // Convert prices to the format expected by frontend
        List<Map<String, Object>> pricesList = new ArrayList<>();
        latestPrices.forEach((symbol, price) -> {
            Map<String, Object> priceData = new HashMap<>();
            priceData.put("symbol", symbol);
            priceData.put("price", price);
            priceData.put("change24h", dailyChanges.getOrDefault(symbol, 0.0));
            pricesList.add(priceData);
        });

        response.put("prices", pricesList);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribeToPair(@RequestBody Map<String, String> request) {
        try {
            String symbol = request.get("symbol");
            log.info("Received subscription request for symbol: {}", symbol);
            
            // 1. Basic validation
            if (symbol == null || symbol.trim().isEmpty()) {
                log.warn("Subscription request rejected: Symbol is required");
                return ResponseEntity.badRequest().body(Map.of("message", "Symbol is required"));
            }

            // 2. Format the symbol
            String formattedSymbol = formatSymbolForKraken(symbol);
            log.info("Formatted symbol for subscription: {}", formattedSymbol);

            // 3. Basic format validation
            if (!isValidSymbolFormat(formattedSymbol)) {
                log.warn("Invalid symbol format: {}", formattedSymbol);
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid symbol format. Expected format: XXX/YYY"
                ));
            }

            // 4. Check WebSocket connection
            if (!krakenWebSocketService.isConnected()) {
                log.error("WebSocket is not connected");
                return ResponseEntity.status(503).body(Map.of(
                    "message", "WebSocket service is not available. Please try again later."
                ));
            }

            // 5. Check if already subscribed
            if (krakenWebSocketService.isSubscribed(formattedSymbol)) {
                log.info("Already subscribed to symbol: {}", formattedSymbol);
                return ResponseEntity.ok(Map.of(
                    "message", "Already subscribed to " + formattedSymbol,
                    "symbol", formattedSymbol
                ));
            }

            // 6. Check if the pair exists in Kraken's API
            if (!isValidKrakenPair(formattedSymbol)) {
                log.warn("Invalid currency pair: {}", formattedSymbol);
                return ResponseEntity.badRequest().body(Map.of(
                    "message", "Currency pair '" + formattedSymbol + "' is not available on Kraken"
                ));
            }

            // 7. All validations passed, subscribe to the pair
            log.info("Attempting to subscribe to symbol: {}", formattedSymbol);
            krakenWebSocketService.subscribeToPairs(formattedSymbol);
            
            log.info("Successfully subscribed to symbol: {}", formattedSymbol);
            return ResponseEntity.ok(Map.of(
                "message", "Successfully subscribed to " + formattedSymbol,
                "symbol", formattedSymbol
            ));
        } catch (IllegalStateException e) {
            log.error("WebSocket connection error: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                "message", "WebSocket service is not available: " + e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Error subscribing to pair: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                "message", "Error subscribing to pair: " + e.getMessage(),
                "error", e.getClass().getSimpleName()
            ));
        }
    }

    private boolean isValidSymbolFormat(String symbol) {
        // Basic validation: should contain exactly one forward slash
        // and both parts should be non-empty
        String[] parts = symbol.split("/");
        if (parts.length != 2) {
            return false;
        }
        
        String baseCurrency = parts[0].trim();
        String quoteCurrency = parts[1].trim();
        
        if (baseCurrency.isEmpty() || quoteCurrency.isEmpty()) {
            return false;
        }
        
        // Basic format validation - currencies should be 2-6 characters
        if (baseCurrency.length() < 2 || baseCurrency.length() > 6 ||
            quoteCurrency.length() < 2 || quoteCurrency.length() > 6) {
            return false;
        }
        
        // Check if currencies only contain letters
        return baseCurrency.matches("[A-Z]+") && quoteCurrency.matches("[A-Z]+");
    }

    private String formatSymbolForKraken(String symbol) {
        // Convert to uppercase and trim
        symbol = symbol.toUpperCase().trim();
        
        // If symbol already has correct format with a slash, return as is
        if (symbol.contains("/")) {
            return symbol;
        }
        
        // Remove any non-letter characters
        symbol = symbol.replaceAll("[^A-Z]", "");
        
        // Special cases for known Kraken pairs
        Map<String, String> specialPairs = Map.of(
            "EURT", "EUR/T",
            "EUREUR", "EUR/EUR",
            "USDEUR", "USD/EUR",
            "GBPEUR", "GBP/EUR",
            "EURUSD", "EUR/USD"
        );
        
        if (specialPairs.containsKey(symbol)) {
            return specialPairs.get(symbol);
        }
        
        // For standard pairs, split into base and quote currencies
        if (symbol.length() >= 6) {
            // Try common quote currencies
            String[] quoteCurrencies = {"USD", "EUR", "GBP", "JPY", "CHF", "USDT"};
            for (String quote : quoteCurrencies) {
                if (symbol.endsWith(quote)) {
                    return symbol.substring(0, symbol.length() - quote.length()) + "/" + quote;
                }
            }
        }
        
        // Default to USD if no other pattern matches
        return symbol + "/USD";
    }
} 