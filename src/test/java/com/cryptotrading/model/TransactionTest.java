package com.cryptotrading.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testNoArgsConstructor() {
        // When
        Transaction transaction = new Transaction();

        // Then
        assertNotNull(transaction);
        assertNull(transaction.getTimestamp());
        assertNull(transaction.getType());
        assertNull(transaction.getSymbol());
        assertEquals(0.0, transaction.getAmount());
        assertEquals(0.0, transaction.getPrice());
        assertEquals(0.0, transaction.getTotal());
        assertNull(transaction.getProfitLoss());
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        String type = "BUY";
        String symbol = "BTC/USD";
        double amount = 1.5;
        double price = 50000.0;
        double total = 75000.0;
        Double profitLoss = 1000.0;

        // When
        Transaction transaction = new Transaction(timestamp, type, symbol, amount, price, total, profitLoss);

        // Then
        assertNotNull(transaction);
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(type, transaction.getType());
        assertEquals(symbol, transaction.getSymbol());
        assertEquals(amount, transaction.getAmount());
        assertEquals(price, transaction.getPrice());
        assertEquals(total, transaction.getTotal());
        assertEquals(profitLoss, transaction.getProfitLoss());
    }

    @Test
    void testBuilder() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        String type = "SELL";
        String symbol = "ETH/USD";
        double amount = 2.0;
        double price = 3000.0;
        double total = 6000.0;
        Double profitLoss = 500.0;

        // When
        Transaction transaction = Transaction.builder()
                .timestamp(timestamp)
                .type(type)
                .symbol(symbol)
                .amount(amount)
                .price(price)
                .total(total)
                .profitLoss(profitLoss)
                .build();

        // Then
        assertNotNull(transaction);
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(type, transaction.getType());
        assertEquals(symbol, transaction.getSymbol());
        assertEquals(amount, transaction.getAmount());
        assertEquals(price, transaction.getPrice());
        assertEquals(total, transaction.getTotal());
        assertEquals(profitLoss, transaction.getProfitLoss());
    }

    @Test
    void testSettersAndGetters() {
        // Given
        Transaction transaction = new Transaction();
        LocalDateTime timestamp = LocalDateTime.now();
        String type = "BUY";
        String symbol = "BTC/USD";
        double amount = 1.0;
        double price = 45000.0;
        double total = 45000.0;
        Double profitLoss = 0.0;

        // When
        transaction.setTimestamp(timestamp);
        transaction.setType(type);
        transaction.setSymbol(symbol);
        transaction.setAmount(amount);
        transaction.setPrice(price);
        transaction.setTotal(total);
        transaction.setProfitLoss(profitLoss);

        // Then
        assertEquals(timestamp, transaction.getTimestamp());
        assertEquals(type, transaction.getType());
        assertEquals(symbol, transaction.getSymbol());
        assertEquals(amount, transaction.getAmount());
        assertEquals(price, transaction.getPrice());
        assertEquals(total, transaction.getTotal());
        assertEquals(profitLoss, transaction.getProfitLoss());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Transaction transaction1 = new Transaction(timestamp, "BUY", "BTC/USD", 1.0, 50000.0, 50000.0, 0.0);
        Transaction transaction2 = new Transaction(timestamp, "BUY", "BTC/USD", 1.0, 50000.0, 50000.0, 0.0);
        Transaction transaction3 = new Transaction(timestamp, "SELL", "ETH/USD", 2.0, 3000.0, 6000.0, 500.0);

        // Then
        assertEquals(transaction1, transaction2);
        assertNotEquals(transaction1, transaction3);
        assertEquals(transaction1.hashCode(), transaction2.hashCode());
        assertNotEquals(transaction1.hashCode(), transaction3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Transaction transaction = new Transaction(timestamp, "BUY", "BTC/USD", 1.0, 50000.0, 50000.0, 0.0);

        // When
        String toString = transaction.toString();

        // Then
        assertTrue(toString.contains("timestamp=" + timestamp));
        assertTrue(toString.contains("type=BUY"));
        assertTrue(toString.contains("symbol=BTC/USD"));
        assertTrue(toString.contains("amount=1.0"));
        assertTrue(toString.contains("price=50000.0"));
        assertTrue(toString.contains("total=50000.0"));
        assertTrue(toString.contains("profitLoss=0.0"));
    }

    @Test
    void testToBuilder() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        Transaction originalTransaction = Transaction.builder()
                .timestamp(timestamp)
                .type("BUY")
                .symbol("BTC/USD")
                .amount(1.0)
                .price(50000.0)
                .total(50000.0)
                .profitLoss(0.0)
                .build();

        // When
        Transaction modifiedTransaction = originalTransaction.toBuilder()
                .type("SELL")
                .price(55000.0)
                .total(55000.0)
                .profitLoss(5000.0)
                .build();

        // Then
        assertEquals(timestamp, modifiedTransaction.getTimestamp());
        assertEquals("SELL", modifiedTransaction.getType());
        assertEquals("BTC/USD", modifiedTransaction.getSymbol());
        assertEquals(1.0, modifiedTransaction.getAmount());
        assertEquals(55000.0, modifiedTransaction.getPrice());
        assertEquals(55000.0, modifiedTransaction.getTotal());
        assertEquals(5000.0, modifiedTransaction.getProfitLoss());
    }
} 