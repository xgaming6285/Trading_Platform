// Package declaration for the controller classes
package com.cryptotrading.controller;

// Import necessary classes and dependencies
import com.cryptotrading.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for handling cryptocurrency trading operations
 * Maps all endpoints to the base URL path "/api"
 */
@RestController
@RequestMapping("/api")
public class TradingController {
    // Logger for tracking application events and debugging
    private static final Logger log = LoggerFactory.getLogger(TradingController.class);

    // Auto-wired service layer to handle business logic
    @Autowired
    private TradingService tradingService;

    /**
     * Handles POST requests to execute trades
     * @param request Map containing trade parameters in the request body
     * @return ResponseEntity with trade result or error message
     */
    @PostMapping("/trade")
    public ResponseEntity<?> executeTrade(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received trade request: {}", request);
            
            // Extract trade parameters from request body
            String type = (String) request.get("type");
            String symbol = (String) request.get("symbol");
            double amount = ((Number) request.get("amount")).doubleValue();
            double price = ((Number) request.get("price")).doubleValue();

            log.info("Processing trade - Type: {}, Symbol: {}, Amount: {}, Price: {}", 
                    type, symbol, amount, price);

            // Execute trade through service layer
            Map<String, Object> result = tradingService.executeTrade(type, symbol, amount, price);
            log.info("Trade executed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            // Handle client-side errors (invalid parameters)
            log.warn("Invalid trade request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            // Handle unexpected server errors
            log.error("Error executing trade", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Handles POST requests to reset user account state
     * @return ResponseEntity with reset operation result or error message
     */
    @PostMapping("/reset")
    public ResponseEntity<?> resetAccount() {
        try {
            Map<String, Object> result = tradingService.resetAccount();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error resetting account", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Internal server error"));
        }
    }

    /**
     * Handles GET requests to retrieve initial account state and market data
     * @return ResponseEntity with initial data or error message
     */
    @GetMapping("/initial-data")
    public ResponseEntity<?> getInitialData() {
        try {
            Map<String, Object> state = tradingService.getUpdatedState();
            return ResponseEntity.ok(state);
        } catch (Exception e) {
            log.error("Error fetching initial data", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Internal server error"));
        }
    }
}
