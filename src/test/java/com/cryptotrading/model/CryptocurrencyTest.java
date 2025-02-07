package com.cryptotrading.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CryptocurrencyTest {

    @Test
    void testNoArgsConstructor() {
        // When
        Cryptocurrency crypto = new Cryptocurrency();

        // Then
        assertNotNull(crypto);
        assertNull(crypto.getSymbol());
        assertNull(crypto.getName());
        assertEquals(0.0, crypto.getCurrentPrice());
        assertEquals(0.0, crypto.getDailyChange());
        assertEquals(0.0, crypto.getVolume());
    }

    @Test
    void testAllArgsConstructor() {
        // Given
        String symbol = "BTC/USD";
        String name = "Bitcoin";
        double currentPrice = 50000.0;
        double dailyChange = 2.5;
        double volume = 1000000.0;

        // When
        Cryptocurrency crypto = new Cryptocurrency(symbol, name, currentPrice, dailyChange, volume);

        // Then
        assertNotNull(crypto);
        assertEquals(symbol, crypto.getSymbol());
        assertEquals(name, crypto.getName());
        assertEquals(currentPrice, crypto.getCurrentPrice());
        assertEquals(dailyChange, crypto.getDailyChange());
        assertEquals(volume, crypto.getVolume());
    }

    @Test
    void testSettersAndGetters() {
        // Given
        Cryptocurrency crypto = new Cryptocurrency();
        String symbol = "ETH/USD";
        String name = "Ethereum";
        double currentPrice = 3000.0;
        double dailyChange = -1.5;
        double volume = 500000.0;

        // When
        crypto.setSymbol(symbol);
        crypto.setName(name);
        crypto.setCurrentPrice(currentPrice);
        crypto.setDailyChange(dailyChange);
        crypto.setVolume(volume);

        // Then
        assertEquals(symbol, crypto.getSymbol());
        assertEquals(name, crypto.getName());
        assertEquals(currentPrice, crypto.getCurrentPrice());
        assertEquals(dailyChange, crypto.getDailyChange());
        assertEquals(volume, crypto.getVolume());
    }

    @Test
    void testEqualsAndHashCode() {
        // Given
        Cryptocurrency crypto1 = new Cryptocurrency("BTC/USD", "Bitcoin", 50000.0, 2.5, 1000000.0);
        Cryptocurrency crypto2 = new Cryptocurrency("BTC/USD", "Bitcoin", 50000.0, 2.5, 1000000.0);
        Cryptocurrency crypto3 = new Cryptocurrency("ETH/USD", "Ethereum", 3000.0, -1.5, 500000.0);

        // Then
        assertEquals(crypto1, crypto2);
        assertNotEquals(crypto1, crypto3);
        assertEquals(crypto1.hashCode(), crypto2.hashCode());
        assertNotEquals(crypto1.hashCode(), crypto3.hashCode());
    }

    @Test
    void testToString() {
        // Given
        Cryptocurrency crypto = new Cryptocurrency("BTC/USD", "Bitcoin", 50000.0, 2.5, 1000000.0);

        // When
        String toString = crypto.toString();

        // Then
        assertTrue(toString.contains("BTC/USD"));
        assertTrue(toString.contains("Bitcoin"));
        assertTrue(toString.contains("50000.0"));
        assertTrue(toString.contains("2.5"));
        assertTrue(toString.contains("1000000.0"));
    }
} 