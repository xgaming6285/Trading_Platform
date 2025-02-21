// Package declaration for model classes
package com.cryptotrading.model;

// Lombok imports for automatic code generation
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Represents a cryptocurrency entity with market data statistics.
 * Uses Lombok annotations to automatically generate:
 * - Getters/Setters
 * - toString() method
 * - equals()/hashCode() methods
 * - Constructors (all-args and no-args)
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Cryptocurrency {
    /**
     * Trading symbol abbreviation (e.g., BTC for Bitcoin)
     * Typically uppercase letters
     */
    private String symbol;

    /**
     * Full currency name (e.g., "Bitcoin")
     */
    private String name;

    /**
     * Current market price in USD
     * Represented as a floating-point number
     */
    private double currentPrice;

    /**
     * 24-hour price change percentage
     * Positive values indicate price increase
     * Negative values indicate price decrease
     */
    private double dailyChange;

    /**
     * 24-hour trading volume in USD
     * Represents total USD value traded in last 24 hours
     */
    private double volume;
}
