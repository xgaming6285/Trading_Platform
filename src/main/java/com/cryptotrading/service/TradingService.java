package com.cryptotrading.service;

import com.cryptotrading.model.Transaction;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TradingService {
    private static final double INITIAL_BALANCE = 10000.00;
    private double accountBalance = INITIAL_BALANCE;
    private final Map<String, Double> portfolio = new ConcurrentHashMap<>();
    private final List<Transaction> transactions = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, List<Double>> purchasePrices = new ConcurrentHashMap<>();

    public synchronized Map<String, Object> executeTrade(String type, String symbol, double amount, double price) {
        validateTradeParameters(type, amount);

        double total = amount * price;

        if ("BUY".equals(type)) {
            validateSufficientFunds(total);
            executeBuy(symbol, amount, price);
        } else {
            validateSufficientCryptoBalance(symbol, amount);
            executeSell(symbol, amount, price);
        }

        return getUpdatedState();
    }

    private void validateTradeParameters(String type, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (!"BUY".equals(type) && !"SELL".equals(type)) {
            throw new IllegalArgumentException("Invalid trade type");
        }
    }

    private void validateSufficientFunds(double total) {
        if (total > accountBalance) {
            throw new IllegalArgumentException("Insufficient funds");
        }
    }

    private void validateSufficientCryptoBalance(String symbol, double amount) {
        if (amount > portfolio.getOrDefault(symbol, 0.0)) {
            throw new IllegalArgumentException("Insufficient cryptocurrency balance");
        }
    }

    private void executeBuy(String symbol, double amount, double price) {
        double total = amount * price;
        accountBalance -= total;

        portfolio.merge(symbol, amount, Double::sum);
        purchasePrices.computeIfAbsent(symbol, k -> new ArrayList<>()).add(price);

        recordTransaction("BUY", symbol, amount, price, total);
    }

    private void executeSell(String symbol, double amount, double price) {
        double total = amount * price;  // Total from selling
        accountBalance += total;

        double avgPurchasePrice = calculateAveragePurchasePrice(symbol, amount);
        double profitLoss = (price - avgPurchasePrice) * amount;  // Calculate profit/loss per unit * amount

        updatePortfolio(symbol, amount);
        recordTransaction("SELL", symbol, amount, price, total, profitLoss);
        updatePurchasePrices(symbol, amount);
    }

    private void updatePortfolio(String symbol, double amount) {
        double newAmount = portfolio.merge(symbol, -amount, Double::sum);
        if (newAmount <= 0) {
            portfolio.remove(symbol);
            purchasePrices.remove(symbol);
        }
    }

    private double calculateAveragePurchasePrice(String symbol, double amount) {
        List<Double> prices = purchasePrices.get(symbol);
        if (prices == null || prices.isEmpty()) {
            return 0.0;
        }
        
        // For FIFO, we only need the first price(s) that cover our amount
        double totalPrice = 0.0;
        int numPrices = 0;
        
        for (Double price : prices) {
            if (numPrices < amount) {
                double units = Math.min(1.0, amount - numPrices);
                totalPrice += price * units;
                numPrices += units;
            }
        }
        
        return totalPrice / amount;
    }

    private void updatePurchasePrices(String symbol, double amount) {
        List<Double> prices = purchasePrices.get(symbol);
        if (prices != null) {
            double remainingToRemove = amount;
            
            while (remainingToRemove > 0 && !prices.isEmpty()) {
                if (remainingToRemove >= 1.0) {
                    prices.remove(0);  // Remove whole unit
                    remainingToRemove -= 1.0;
                } else {
                    // Handle fractional unit
                    double currentPrice = prices.get(0);
                    double remainingFraction = 1.0 - remainingToRemove;
                    if (remainingFraction > 0) {
                        prices.set(0, currentPrice);  // Keep the price for remaining fraction
                    } else {
                        prices.remove(0);
                    }
                    remainingToRemove = 0;
                }
            }
        }
    }

    private void recordTransaction(String type, String symbol, double amount, double price, double total) {
        recordTransaction(type, symbol, amount, price, total, 0.0);
    }

    private void recordTransaction(String type, String symbol, double amount, double price, double total, double profitLoss) {
        transactions.add(Transaction.builder()
                .timestamp(LocalDateTime.now())
                .type(type)
                .symbol(symbol)
                .amount(amount)
                .price(price)
                .total(total)
                .profitLoss(profitLoss)
                .build());
    }

    public Map<String, Object> resetAccount() {
        accountBalance = INITIAL_BALANCE;
        portfolio.clear();
        transactions.clear();
        purchasePrices.clear();
        return getUpdatedState();
    }

    public Map<String, Object> getUpdatedState() {
        Map<String, Object> state = new HashMap<>();
        state.put("balance", accountBalance);
        state.put("portfolio", new HashMap<>(portfolio));
        state.put("transactions", new ArrayList<>(transactions));
        return state;
    }
}