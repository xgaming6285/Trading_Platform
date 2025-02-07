package com.cryptotrading.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UserPortfolioTest {

    @Test
    void testConstructor() {
        // When
        UserPortfolio portfolio = new UserPortfolio();

        // Then
        assertEquals(10000.0, portfolio.getBalance());
        assertNotNull(portfolio.getHoldings());
        assertTrue(portfolio.getHoldings().isEmpty());
        assertNotNull(portfolio.getTransactions());
        assertTrue(portfolio.getTransactions().isEmpty());
    }

    @Test
    void testSetAndGetBalance() {
        // Given
        UserPortfolio portfolio = new UserPortfolio();
        double newBalance = 5000.0;

        // When
        portfolio.setBalance(newBalance);

        // Then
        assertEquals(newBalance, portfolio.getBalance());
    }

    @Test
    void testSetAndGetHoldings() {
        // Given
        UserPortfolio portfolio = new UserPortfolio();
        Map<String, Double> holdings = new HashMap<>();
        holdings.put("BTC/USD", 1.5);
        holdings.put("ETH/USD", 10.0);

        // When
        portfolio.setHoldings(holdings);

        // Then
        assertEquals(holdings, portfolio.getHoldings());
        assertEquals(1.5, portfolio.getHoldings().get("BTC/USD"));
        assertEquals(10.0, portfolio.getHoldings().get("ETH/USD"));
    }

    @Test
    void testSetAndGetTransactions() {
        // Given
        UserPortfolio portfolio = new UserPortfolio();
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                .symbol("BTC/USD")
                .amount(1.0)
                .price(50000.0)
                .type("BUY")
                .build());

        // When
        portfolio.setTransactions(transactions);

        // Then
        assertEquals(transactions, portfolio.getTransactions());
        assertEquals(1, portfolio.getTransactions().size());
        assertEquals("BTC/USD", portfolio.getTransactions().get(0).getSymbol());
    }

    @Test
    void testReset() {
        // Given
        UserPortfolio portfolio = new UserPortfolio();
        portfolio.setBalance(5000.0);
        
        Map<String, Double> holdings = new HashMap<>();
        holdings.put("BTC/USD", 1.5);
        portfolio.setHoldings(holdings);
        
        List<Transaction> transactions = new ArrayList<>();
        transactions.add(Transaction.builder()
                .symbol("BTC/USD")
                .amount(1.0)
                .price(50000.0)
                .type("BUY")
                .build());
        portfolio.setTransactions(transactions);

        // When
        portfolio.reset();

        // Then
        assertEquals(10000.0, portfolio.getBalance());
        assertTrue(portfolio.getHoldings().isEmpty());
        assertTrue(portfolio.getTransactions().isEmpty());
    }
} 