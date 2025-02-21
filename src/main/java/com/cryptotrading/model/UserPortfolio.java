// Package declaration for model classes
package com.cryptotrading.model;

// Import necessary collection classes
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user's cryptocurrency portfolio containing:
 * - Current cash balance
 * - Cryptocurrency holdings
 * - Transaction history
 * 
 * <p>Maintains the state of all trading activities and asset allocations</p>
 */
public class UserPortfolio {
    /**
     * Initial starting balance for new accounts (in USD)
     * Default value: $10,000
     */
    private static final double INITIAL_BALANCE = 10000.0;
    
    /**
     * Current available balance in USD
     * Used for executing new trades
     */
    private double balance;

    /**
     * Cryptocurrency holdings mapped by symbol
     * Key: Cryptocurrency symbol (e.g., "BTC")
     * Value: Quantity owned (in cryptocurrency units)
     */
    private Map<String, Double> holdings; // symbol -> quantity

    /**
     * Chronological list of all financial transactions
     * Includes trades, deposits, and withdrawals
     */
    private List<Transaction> transactions;

    /**
     * Constructs a new portfolio with:
     * - Initial balance
     * - Empty cryptocurrency holdings
     * - Empty transaction history
     */
    public UserPortfolio() {
        this.balance = INITIAL_BALANCE;
        this.holdings = new HashMap<>();
        this.transactions = new ArrayList<>();
    }

    // ACCESSORS AND MUTATORS
    
    /**
     * @return Current available balance in USD
     */
    public double getBalance() {
        return balance;
    }

    /**
     * Sets new balance value
     * @param balance New balance in USD (must be non-negative)
     */
    public void setBalance(double balance) {
        this.balance = balance;
    }

    /**
     * @return Unmodifiable view of cryptocurrency holdings
     * Format: Map<Symbol, Quantity>
     */
    public Map<String, Double> getHoldings() {
        return holdings;
    }

    /**
     * Replaces entire holdings map
     * @param holdings New holdings mapping (symbol -> quantity)
     */
    public void setHoldings(Map<String, Double> holdings) {
        this.holdings = holdings;
    }

    /**
     * @return Unmodifiable list of transaction history
     * Ordered by execution time (oldest first)
     */
    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Replaces entire transaction history
     * @param transactions New list of transactions
     */
    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    /**
     * Resets portfolio to initial state:
     * - Restores balance to initial amount
     * - Clears all cryptocurrency holdings
     * - Clears transaction history
     * Typically used for account reset functionality
     */
    public void reset() {
        this.balance = INITIAL_BALANCE;
        this.holdings.clear();
        this.transactions.clear();
    }
}
