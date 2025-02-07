package com.cryptotrading.model;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class UserPortfolio {
    private static final double INITIAL_BALANCE = 10000.0;
    
    private double balance;
    private Map<String, Double> holdings; // symbol -> quantity
    private List<Transaction> transactions;

    public UserPortfolio() {
        this.balance = INITIAL_BALANCE;
        this.holdings = new HashMap<>();
        this.transactions = new ArrayList<>();
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public Map<String, Double> getHoldings() {
        return holdings;
    }

    public void setHoldings(Map<String, Double> holdings) {
        this.holdings = holdings;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void reset() {
        this.balance = INITIAL_BALANCE;
        this.holdings.clear();
        this.transactions.clear();
    }
} 