# API Documentation

## Overview
This document describes the REST API endpoints and WebSocket interface for the Cryptocurrency Trading Simulator platform.

## Base URL 
http://localhost:8080/api

## REST Endpoints

### Get Initial Data
Retrieves initial application data including account balance, portfolio, and current prices.
GET /api/initial-data

#### Response
json
{
"balance": 10000.00,
"portfolio": {
"holdings": {
"BTC/USD": 0.5,
"ETH/USD": 2.0
},
"totalValue": 25000.00
},
"prices": [
{
"symbol": "BTC/USD",
"price": 35000.00,
"change24h": 2.5
}
]
}

### Get Cryptocurrency Prices
Fetches current prices for all tracked cryptocurrencies.
GET /api/crypto-data

#### Response
json
{
"prices": [
{
"symbol": "BTC/USD",
"price": 35000.00,
"change24h": 2.5
}
]
}

### Execute Trade
Executes a buy or sell trade.
POST /api/trade

#### Request Body
json
{
"type": "BUY", // or "SELL"
"symbol": "BTC/USD",
"amount": 0.5,
"price": 35000.00
}

#### Response
json
{
"success": true,
"balance": 8250.00,
"holdings": {
"BTC/USD": 0.5
},
"transaction": {
"id": "tx123",
"timestamp": "2024-03-21T15:30:00Z",
"type": "BUY",
"symbol": "BTC/USD",
"amount": 0.5,
"price": 35000.00,
"total": 17500.00
}
}

### Reset Account
Resets the account balance and holdings to initial values.
POST /api/reset

#### Response
json
{
"balance": 10000.00,
"holdings": {},
"totalValue": 10000.00
}

## WebSocket Interface

### Connection URL
ws://localhost:8080/ws

### Message Types

#### Subscribe to Updates
json
{
"type": "SUBSCRIBE",
"message": "Subscribing to price updates"
}

#### Price Update Message
json
{
"type": "PRICE_UPDATE",
"symbol": "BTC/USD",
"price": 35000.00,
"change24h": 2.5
}

#### Error Message
json
{
"type": "ERROR",
"message": "Error description"
}

## Error Handling

All endpoints return standard HTTP status codes:

- 200: Success
- 400: Bad Request (invalid input)
- 401: Unauthorized
- 403: Forbidden
- 404: Not Found
- 500: Internal Server Error

Error responses include a message:
json
{
"error": "Error description",
"timestamp": "2024-03-21T15:30:00Z"
}