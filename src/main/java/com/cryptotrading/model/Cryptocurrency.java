package com.cryptotrading.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cryptocurrency {
    private String symbol;
    private String name;
    private double currentPrice;
    private double dailyChange;
    private double volume;
} 