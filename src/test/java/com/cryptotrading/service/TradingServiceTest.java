package com.cryptotrading.service;

import com.cryptotrading.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TradingServiceTest {

    private TradingService tradingService;
    private static final double INITIAL_BALANCE = 10000.00;
    private static final String SYMBOL = "BTC/USD";
    private static final double DELTA = 0.0001;

    @BeforeEach
    void setUp() {
        tradingService = new TradingService();
    }

    @Test
    void executeTrade_WhenBuyingWithSufficientFunds_ShouldSucceed() {
        // Given
        double amount = 1.0;
        double price = 5000.0;

        // When
        Map<String, Object> result = tradingService.executeTrade("BUY", SYMBOL, amount, price);

        // Then
        assertEquals(INITIAL_BALANCE - (amount * price), (Double) result.get("balance"), DELTA);
        @SuppressWarnings("unchecked")
        Map<String, Double> portfolio = (Map<String, Double>) result.get("portfolio");
        assertEquals(amount, portfolio.get(SYMBOL), DELTA);
        
        @SuppressWarnings("unchecked")
        List<Transaction> transactions = (List<Transaction>) result.get("transactions");
        assertEquals(1, transactions.size());
        
        Transaction transaction = transactions.get(0);
        assertEquals("BUY", transaction.getType());
        assertEquals(SYMBOL, transaction.getSymbol());
        assertEquals(amount, transaction.getAmount());
        assertEquals(price, transaction.getPrice());
        assertEquals(amount * price, transaction.getTotal());
        assertEquals(0.0, transaction.getProfitLoss());
    }

    @Test
    void executeTrade_WhenBuyingWithInsufficientFunds_ShouldThrowException() {
        // Given
        double amount = 1.0;
        double price = INITIAL_BALANCE + 1000.0;

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> tradingService.executeTrade("BUY", SYMBOL, amount, price));
        assertEquals("Insufficient funds", exception.getMessage());
    }

    @Test
    void executeTrade_WhenSellingWithSufficientBalance_ShouldSucceed() {
        // Given
        double buyAmount = 2.0;
        double buyPrice = 5000.0;
        tradingService.executeTrade("BUY", SYMBOL, buyAmount, buyPrice);

        double sellAmount = 1.0;
        double sellPrice = 6000.0;

        // When
        Map<String, Object> result = tradingService.executeTrade("SELL", SYMBOL, sellAmount, sellPrice);

        // Then
        double expectedBalance = INITIAL_BALANCE - (buyAmount * buyPrice) + (sellAmount * sellPrice);
        assertEquals(expectedBalance, (Double) result.get("balance"), DELTA);
        
        @SuppressWarnings("unchecked")
        Map<String, Double> portfolio = (Map<String, Double>) result.get("portfolio");
        assertEquals(buyAmount - sellAmount, portfolio.get(SYMBOL), DELTA);
        
        @SuppressWarnings("unchecked")
        List<Transaction> transactions = (List<Transaction>) result.get("transactions");
        assertEquals(2, transactions.size());
        
        Transaction sellTransaction = transactions.get(1);
        assertEquals("SELL", sellTransaction.getType());
        assertEquals(SYMBOL, sellTransaction.getSymbol());
        assertEquals(sellAmount, sellTransaction.getAmount());
        assertEquals(sellPrice, sellTransaction.getPrice());
        assertEquals(sellAmount * sellPrice, sellTransaction.getTotal());
        assertEquals(1000.0, sellTransaction.getProfitLoss(), DELTA); // (6000 - 5000) * 1.0
    }

    @Test
    void executeTrade_WhenSellingWithInsufficientBalance_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> tradingService.executeTrade("SELL", SYMBOL, 1.0, 5000.0));
        assertEquals("Insufficient cryptocurrency balance", exception.getMessage());
    }

    @Test
    void executeTrade_WithInvalidAmount_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> tradingService.executeTrade("BUY", SYMBOL, -1.0, 5000.0));
        assertEquals("Amount must be greater than 0", exception.getMessage());
    }

    @Test
    void executeTrade_WithInvalidType_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> tradingService.executeTrade("INVALID", SYMBOL, 1.0, 5000.0));
        assertEquals("Invalid trade type", exception.getMessage());
    }

    @Test
    void resetAccount_ShouldResetToInitialState() {
        // Given
        tradingService.executeTrade("BUY", SYMBOL, 1.0, 5000.0);

        // When
        Map<String, Object> result = tradingService.resetAccount();

        // Then
        assertEquals(INITIAL_BALANCE, (Double) result.get("balance"), DELTA);
        @SuppressWarnings("unchecked")
        Map<String, Double> portfolio = (Map<String, Double>) result.get("portfolio");
        assertTrue(portfolio.isEmpty());
        @SuppressWarnings("unchecked")
        List<Transaction> transactions = (List<Transaction>) result.get("transactions");
        assertTrue(transactions.isEmpty());
    }

    @Test
    void getUpdatedState_ShouldReturnCurrentState() {
        // When
        Map<String, Object> state = tradingService.getUpdatedState();

        // Then
        assertEquals(INITIAL_BALANCE, (Double) state.get("balance"), DELTA);
        @SuppressWarnings("unchecked")
        Map<String, Double> portfolio = (Map<String, Double>) state.get("portfolio");
        assertTrue(portfolio.isEmpty());
        @SuppressWarnings("unchecked")
        List<Transaction> transactions = (List<Transaction>) state.get("transactions");
        assertTrue(transactions.isEmpty());
    }

    @Test
    void executeTrade_WhenSellingEntireHolding_ShouldRemoveFromPortfolio() {
        // Given
        double amount = 1.0;
        double buyPrice = 5000.0;
        tradingService.executeTrade("BUY", SYMBOL, amount, buyPrice);

        // When
        Map<String, Object> result = tradingService.executeTrade("SELL", SYMBOL, amount, 6000.0);

        // Then
        @SuppressWarnings("unchecked")
        Map<String, Double> portfolio = (Map<String, Double>) result.get("portfolio");
        assertFalse(portfolio.containsKey(SYMBOL));
    }
} 