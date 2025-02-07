package com.cryptotrading.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private LocalDateTime timestamp;
    private String type;
    private String symbol;
    private double amount;
    private double price;
    private double total;
    private Double profitLoss;
} 