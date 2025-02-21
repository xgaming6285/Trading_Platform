// Package declaration for model classes
package com.cryptotrading.model;

// Lombok imports for code generation
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Represents a financial transaction in the cryptocurrency trading system.
 * Uses Lombok to generate boilerplate code and builder pattern support.
 * 
 * <p>Builder created with toBuilder=true allows creating modified copies of existing instances</p>
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    /**
     * Exact date and time of the transaction execution
     * Stored in UTC or local timezone based on system configuration
     */
    private LocalDateTime timestamp;

    /**
     * Transaction type (e.g., "BUY", "SELL", "DEPOSIT", "WITHDRAWAL")
     * Expected to be in uppercase format
     */
    private String type;

    /**
     * Cryptocurrency symbol (e.g., "BTC", "ETH")
     * Typically 3-5 uppercase letters
     */
    private String symbol;

    /**
     * Quantity of cryptocurrency traded
     * Always positive value
     */
    private double amount;

    /**
     * Price per unit of cryptocurrency at transaction time
     * In USD
     */
    private double price;

    /**
     * Total transaction value (calculated as amount * price)
     * In USD
     */
    private double total;

    /**
     * Realized profit/loss from the transaction (if applicable)
     * Null for non-trading transactions (deposits/withdrawals)
     * Positive values indicate profit, negative values indicate loss
     * Stored as Double wrapper type to allow null values
     */
    private Double profitLoss;
}
