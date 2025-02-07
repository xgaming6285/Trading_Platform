package com.cryptotrading.controller;

import com.cryptotrading.service.TradingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class TradingController {
    private static final Logger log = LoggerFactory.getLogger(TradingController.class);

    @Autowired
    private TradingService tradingService;

    @PostMapping("/trade")
    public ResponseEntity<?> executeTrade(@RequestBody Map<String, Object> request) {
        try {
            log.info("Received trade request: {}", request);
            
            String type = (String) request.get("type");
            String symbol = (String) request.get("symbol");
            double amount = ((Number) request.get("amount")).doubleValue();
            double price = ((Number) request.get("price")).doubleValue();

            log.info("Processing trade - Type: {}, Symbol: {}, Amount: {}, Price: {}", 
                    type, symbol, amount, price);

            Map<String, Object> result = tradingService.executeTrade(type, symbol, amount, price);
            log.info("Trade executed successfully: {}", result);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid trade request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error executing trade", e);
            return ResponseEntity.internalServerError()
                .body(Map.of("message", "Internal server error"));
        }
    }

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