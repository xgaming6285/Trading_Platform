// Constants
const INITIAL_BALANCE = 10000.00;
const BACKEND_URL = 'http://localhost:8080';
const WS_URL = 'ws://localhost:8080/ws';
const RECONNECT_INTERVAL = 3000;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const NOTIFICATION_DURATION = 3000;

// State management
let accountBalance = INITIAL_BALANCE;
let totalPortfolioValue = 0;
let portfolio = new Map();
let transactions = [];
let cryptoPrices = new Map();
let ws;
let lastMessageTimestamp = Date.now();
let connectionHealthCheck;
let currentSortColumn = '';
let sortDirection = 'asc';
let subscribedPairs = new Set();

// DOM Elements
const accountBalanceElement = document.getElementById('account-balance');
const cryptoList = document.getElementById('crypto-list');
const cryptoSelect = document.getElementById('crypto-select');
const tradeAmount = document.getElementById('trade-amount');
const buyButton = document.getElementById('buy-btn');
const sellButton = document.getElementById('sell-btn');
const portfolioList = document.getElementById('portfolio-list');
const transactionHistory = document.getElementById('transaction-history');
const resetButton = document.getElementById('reset-balance');
const errorModal = new bootstrap.Modal(document.getElementById('error-modal'));
const errorMessage = document.getElementById('error-message');
const statusDot = document.getElementById('connection-status');
const statusMessage = document.getElementById('status-message');

function showStatusMessage(message) {
    statusMessage.textContent = message;
    statusMessage.classList.add('show');
    setTimeout(() => {
        statusMessage.classList.remove('show');
    }, 2000);
}

function updateConnectionStatus(status) {
    // Remove all existing status classes
    statusDot.classList.remove('connected', 'connecting', 'error');
    
    // Add appropriate class and message based on status
    switch (status) {
        case 'Connected':
            statusDot.classList.add('connected');
            showStatusMessage('Connected');
            break;
        case 'Connecting...':
            statusDot.classList.add('connecting');
            showStatusMessage('Connecting...');
            break;
        case 'Disconnected':
        case 'Error':
            statusDot.classList.add('error');
            showStatusMessage(status);
            break;
    }
}

// WebSocket Connection
function connectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        return;
    }

    ws = new WebSocket(WS_URL);
    updateConnectionStatus('Connecting...');

    ws.onopen = () => {
        console.log('WebSocket connected');
        reconnectAttempts = 0;
        lastMessageTimestamp = Date.now();
        updateConnectionStatus('Connected');
        startConnectionHealthCheck();
        subscribeToUpdates();
    };

    ws.onmessage = (event) => {
        console.log('Received WebSocket message:', event.data);
        lastMessageTimestamp = Date.now();
        updateConnectionStatus('Connected');
        const data = JSON.parse(event.data);
        handleWebSocketMessage(data);
    };

    ws.onerror = (error) => {
        console.error('WebSocket error:', error);
        updateConnectionStatus('Error');
        showError('WebSocket connection error. Please try again later.');
    };

    ws.onclose = () => {
        console.log('WebSocket disconnected.');
        updateConnectionStatus('Disconnected');
        clearInterval(connectionHealthCheck);
        
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            console.log(`Attempting to reconnect (${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS})...`);
            setTimeout(connectWebSocket, RECONNECT_INTERVAL);
        } else {
            showError('Unable to maintain connection. Please refresh the page.');
        }
    };
}

function startConnectionHealthCheck() {
    if (connectionHealthCheck) {
        clearInterval(connectionHealthCheck);
    }
    
    connectionHealthCheck = setInterval(() => {
        const now = Date.now();
        if (now - lastMessageTimestamp > 10000) { // 10 seconds without messages
            console.log('No messages received for 10 seconds, reconnecting...');
            ws.close();
            connectWebSocket();
        }
    }, 5000);
}

// Subscribe to crypto updates
function subscribeToUpdates() {
    if (ws.readyState === WebSocket.OPEN) {
        // Send initial subscription message
        const subscribeMessage = {
            type: "SUBSCRIBE",
            message: "Subscribing to price updates"
        };
        ws.send(JSON.stringify(subscribeMessage));
    } else {
        console.error('Cannot subscribe: WebSocket is not connected');
    }
}

// Currency selector functionality
let availablePairs = new Map();

async function fetchAvailablePairs() {
    try {
        const response = await fetch('https://api.kraken.com/0/public/AssetPairs');
        const data = await response.json();
        
        if (data.error && data.error.length > 0) {
            throw new Error(data.error[0]);
        }

        // Process Kraken pairs into a more usable format
        for (const [key, value] of Object.entries(data.result)) {
            const wsname = value.wsname;
            if (wsname) {
                availablePairs.set(wsname, wsname);
            }
        }

        // Add some common pairs to the top of suggestions
        const commonPairs = [
            // Major Crypto/Fiat
            'XBT/USD', 'XBT/EUR', 'ETH/USD', 'ETH/EUR',
            'XRP/USD', 'XRP/EUR', 'ADA/USD', 'ADA/EUR',
            // Major Forex
            'EUR/USD', 'GBP/USD', 'USD/JPY', 'USD/CAD',
            // Stablecoins
            'USDT/USD', 'USDC/USD', 'DAI/USD',
            'USDT/EUR', 'USDC/EUR', 'DAI/EUR'
        ];

        commonPairs.forEach(pair => {
            if (!availablePairs.has(pair)) {
                availablePairs.set(pair, pair);
            }
        });

    } catch (error) {
        console.error('Error fetching available pairs:', error);
        showNotification('Error fetching currency pairs', 'error');
    }
}

// Update the subscribeToCurrency function to handle pair formatting
async function subscribeToCurrency(symbol) {
    try {
        // Format the symbol if needed
        let formattedSymbol = symbol.toUpperCase().trim();
        if (!formattedSymbol.includes('/')) {
            // Special cases for known Kraken pairs
            const specialPairs = {
                'EURT': 'EURT/EUR',
                'EUREUR': 'EUR/EUR',
                'USDEUR': 'USD/EUR',
                'GBPEUR': 'GBP/EUR',
                'EURUSD': 'EUR/USD'
            };

            if (specialPairs[formattedSymbol]) {
                formattedSymbol = specialPairs[formattedSymbol];
            } else if (formattedSymbol.length >= 6) {
                const possibleQuote = formattedSymbol.slice(-3);
                if (['USD', 'EUR', 'GBP', 'JPY', 'CHF'].includes(possibleQuote)) {
                    formattedSymbol = formattedSymbol.slice(0, -3) + '/' + possibleQuote;
                } else if (formattedSymbol.endsWith('USDT')) {
                    formattedSymbol = formattedSymbol.slice(0, -4) + '/USDT';
                } else {
                    // Let the backend handle the default case and special validations
                    formattedSymbol = formattedSymbol;
                }
            }
        }

        console.log('Attempting to subscribe to:', formattedSymbol);
        console.log('Sending request to:', `${BACKEND_URL}/api/subscribe`);

        const response = await fetch(`${BACKEND_URL}/api/subscribe`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ symbol: formattedSymbol })
        });

        console.log('Response status:', response.status);
        const responseText = await response.text();
        console.log('Response text:', responseText);

        if (!response.ok) {
            let errorMessage;
            try {
                const errorData = JSON.parse(responseText);
                errorMessage = errorData.message || 'Failed to subscribe';
            } catch (e) {
                errorMessage = responseText || 'Failed to subscribe';
            }
            throw new Error(errorMessage);
        }

        let result;
        try {
            result = JSON.parse(responseText);
        } catch (e) {
            console.error('Error parsing response:', e);
            throw new Error('Invalid response from server');
        }

        console.log('Subscription result:', result);
        showNotification(`Successfully subscribed to ${formattedSymbol}`, 'success');
        return result;
    } catch (error) {
        console.error('Error details:', {
            message: error.message,
            stack: error.stack,
            error
        });
        showNotification(`Failed to subscribe to ${symbol}: ${error.message}`, 'error');
        throw error;
    }
}

// Handle WebSocket messages
function handleWebSocketMessage(data) {
    if (data.type === 'SUBSCRIPTION_CONFIRMED') {
        console.log('Subscription confirmed:', data.message);
        return;
    }

    if (data.type === 'PRICE_UPDATE') {
        updateCryptoPrice(data);
        return;
    }

    // Handle other message types
    switch (data.type) {
        case 'PORTFOLIO_UPDATE':
            updatePortfolio(data.portfolio);
            break;
        case 'TRANSACTION_UPDATE':
            updateTransactions(data.transactions);
            break;
        case 'ERROR':
            showError(data.message);
            break;
    }
}

// Update crypto price in the table
function updateCryptoPrice(data) {
    const { symbol, price, change24h } = data;
    const oldPrice = cryptoPrices.get(symbol);
    cryptoPrices.set(symbol, price);

    // Update account balance based on price change if we hold this crypto
    if (portfolio.has(symbol)) {
        const holdingAmount = portfolio.get(symbol);
        const priceChange = price - (oldPrice || price);
        const valueChange = holdingAmount * priceChange;
        accountBalance += valueChange;
        accountBalanceElement.textContent = `${accountBalance.toFixed(2)}`;
    }

    const existingRow = document.querySelector(`tr[data-symbol="${symbol}"]`);
    if (existingRow) {
        const priceCell = existingRow.querySelector('.crypto-price');
        const changeCell = existingRow.querySelector('.price-change');
        
        const priceChanged = priceCell.textContent !== price.toFixed(2);
        if (priceChanged) {
            priceCell.textContent = price.toFixed(2);
            priceCell.classList.add('price-update');
            setTimeout(() => priceCell.classList.remove('price-update'), 1000);
        }
        
        changeCell.textContent = `${change24h.toFixed(2)}%`;
        changeCell.className = `price-change ${change24h >= 0 ? 'price-up' : 'price-down'}`;
    } else {
        addCryptoRow(symbol, price, change24h);
    }

    calculateTotalPortfolioValue();
}

// Add a new cryptocurrency row to the table
function addCryptoRow(symbol, price, change24h, isLoading = false) {
    const existingRow = document.querySelector(`tr[data-symbol="${symbol}"]`);
    if (existingRow) {
        if (!isLoading) {
            // Update existing row with new data
            const priceCell = existingRow.querySelector('.crypto-price');
            const changeCell = existingRow.querySelector('.price-change');
            
            if (priceCell.querySelector('.fa-spinner')) {
                priceCell.textContent = price.toFixed(2);
            }
            if (changeCell.querySelector('.fa-spinner')) {
                changeCell.textContent = `${change24h.toFixed(2)}%`;
                changeCell.className = `price-change ${change24h >= 0 ? 'price-up' : 'price-down'}`;
            }
        }
        return;
    }

    // Create new row
    const row = document.createElement('tr');
    row.setAttribute('data-symbol', symbol);
    
    // Extract the base currency from the pair (e.g., "BTC" from "BTC/USD")
    const baseCurrency = symbol.split('/')[0];
    
    row.innerHTML = `
        <td class="crypto-name-cell">
            <img src="https://cdn.jsdelivr.net/gh/atomiclabs/cryptocurrency-icons@1a63530be6e374711a8554f31b17e4cb92c25fa5/32/color/${baseCurrency.toLowerCase()}.png"
                 class="crypto-icon"
                 onerror="this.src='https://cdn.jsdelivr.net/gh/atomiclabs/cryptocurrency-icons@1a63530be6e374711a8554f31b17e4cb92c25fa5/32/color/generic.png'"
                 alt="${baseCurrency}">
            ${baseCurrency}
        </td>
        <td>${symbol}</td>
        <td class="crypto-price">${isLoading ? '<i class="fas fa-spinner fa-spin"></i>' : price.toFixed(2)}</td>
        <td class="price-change">${isLoading ? '<i class="fas fa-spinner fa-spin"></i>' : `${change24h.toFixed(2)}%`}</td>
        <td class="actions text-center">
            <button class="btn btn-sm btn-danger remove-currency" onclick="removeCurrency('${symbol}')">
                <i class="fas fa-times"></i>
            </button>
        </td>
    `;

    cryptoList.appendChild(row);

    // Add to select dropdown if not exists
    if (!cryptoSelect.querySelector(`option[value="${symbol}"]`)) {
        const option = document.createElement('option');
        option.value = symbol;
        option.textContent = symbol;
        cryptoSelect.appendChild(option);
    }
}

// Update portfolio display
function updatePortfolio(portfolioData) {
    portfolio = new Map(Object.entries(portfolioData));
    renderPortfolio();
    calculateTotalPortfolioValue();
}

// Render portfolio
function renderPortfolio() {
    portfolioList.innerHTML = '';
    portfolio.forEach((amount, symbol) => {
        if (amount > 0) {
            const price = cryptoPrices.get(symbol) || 0;
            const value = amount * price;
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${symbol}</td>
                <td>${amount.toFixed(6)}</td>
                <td>${value.toFixed(2)}</td>
            `;
            portfolioList.appendChild(row);
        }
    });
}

// Update transaction history
function updateTransactions(transactionData) {
    transactions = transactionData;
    renderTransactions();
}

// Render transactions
function renderTransactions() {
    transactionHistory.innerHTML = '';
    transactions.forEach(transaction => {
        const row = document.createElement('tr');
        const profitLoss = transaction.type === 'SELL' ? transaction.profitLoss : '-';
        const profitLossClass = profitLoss > 0 ? 'profit' : profitLoss < 0 ? 'loss' : '';
        
        row.innerHTML = `
            <td>${new Date(transaction.timestamp).toLocaleString()}</td>
            <td>${transaction.type}</td>
            <td>${transaction.symbol}</td>
            <td>${transaction.amount.toFixed(6)}</td>
            <td>${transaction.price.toFixed(2)}</td>
            <td>${transaction.total.toFixed(2)}</td>
            <td class="${profitLossClass}">${profitLoss === '-' ? '-' : `${profitLoss.toFixed(2)}`}</td>
        `;
        transactionHistory.appendChild(row);
    });
}

// Add new function to calculate total portfolio value
function calculateTotalPortfolioValue() {
    totalPortfolioValue = 0;
    portfolio.forEach((amount, symbol) => {
        const price = cryptoPrices.get(symbol) || 0;
        totalPortfolioValue += amount * price;
    });
    document.getElementById('total-value').textContent = `${totalPortfolioValue.toFixed(2)}`;
}

// Execute trade
async function executeTrade(type) {
    const symbol = cryptoSelect.value;
    const amount = parseFloat(tradeAmount.value);

    if (!symbol || !amount || amount <= 0) {
        showError('Please select a cryptocurrency and enter a valid amount.');
        return;
    }

    const price = cryptoPrices.get(symbol);
    if (!price) {
        showError('Price information not available. Please try again.');
        return;
    }

    const tradeValue = amount * price;

    try {
        if (type === 'BUY' && tradeValue > accountBalance) {
            showError('Insufficient funds for this purchase.');
            return;
        }

        const response = await fetch(`${BACKEND_URL}/api/trade`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                type,
                symbol,
                amount,
                price
            })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message);
        }

        const result = await response.json();
        
        if (type === 'BUY') {
            accountBalance -= tradeValue;
        } else if (type === 'SELL') {
            const latestTransaction = result.transactions[result.transactions.length - 1];
            accountBalance += tradeValue;
        }
        
        // Update displays
        accountBalanceElement.textContent = `${accountBalance.toFixed(2)}`;
        
        updatePortfolio(result.portfolio);
        updateTransactions(result.transactions);
        
        // Clear form
        cryptoSelect.value = '';
        tradeAmount.value = '';
    } catch (error) {
        showError(error.message);
    }
}

// Reset account
async function resetAccount() {
    try {
        const response = await fetch(`${BACKEND_URL}/api/reset`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error('Failed to reset account');
        }

        const result = await response.json();
        accountBalance = result.balance;
        totalPortfolioValue = 0;
        
        // Keep track of subscribed pairs before clearing the table
        const currentPairs = new Set(Array.from(document.querySelectorAll('#crypto-list tr'))
            .map(row => row.getAttribute('data-symbol')));
        
        updatePortfolio(result.portfolio);
        updateTransactions(result.transactions);
        accountBalanceElement.textContent = `${accountBalance.toFixed(2)}`;
        document.getElementById('total-value').textContent = `${totalPortfolioValue.toFixed(2)}`;
        
        // Resubscribe to all pairs
        currentPairs.forEach(pair => {
            if (ws.readyState === WebSocket.OPEN) {
                const subscribeMessage = JSON.stringify({
                    event: "subscribe",
                    pair: [pair],
                    subscription: { name: "ticker" }
                });
                ws.send(subscribeMessage);
            }
        });
    } catch (error) {
        showError('Failed to reset account. Please try again.');
    }
}

// Show error message
function showError(message) {
    errorMessage.textContent = message;
    errorModal.show();
}

// Event Listeners
buyButton.addEventListener('click', () => executeTrade('BUY'));
sellButton.addEventListener('click', () => executeTrade('SELL'));
resetButton.addEventListener('click', resetAccount);

// Initialize WebSocket connection
connectWebSocket();

// Initial data load with retry
function loadInitialData(retryCount = 0, maxRetries = 3) {
    fetch(`${BACKEND_URL}/api/initial-data`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to load initial data');
            }
            return response.json();
        })
        .then(data => {
            accountBalance = data.balance;
            totalPortfolioValue = 0; // Start with 0 total portfolio value
            accountBalanceElement.textContent = `${accountBalance.toFixed(2)}`;
            document.getElementById('total-value').textContent = `${totalPortfolioValue.toFixed(2)}`;
            updatePortfolio(data.portfolio || {});
            updateTransactions(data.transactions || []);
            if (data.prices && data.prices.length > 0) {
                data.prices.forEach(price => updateCryptoPrice(price));
            }
        })
        .catch(error => {
            console.error('Error loading initial data:', error);
            if (retryCount < maxRetries) {
                console.log(`Retrying initial data load (${retryCount + 1}/${maxRetries})...`);
                setTimeout(() => loadInitialData(retryCount + 1, maxRetries), 2000);
            } else {
                // Don't show error if we're receiving WebSocket updates
                if (!lastMessageTimestamp || Date.now() - lastMessageTimestamp > 10000) {
                    showError('Failed to load initial data. Please refresh the page.');
                }
            }
        });
}

// Start loading initial data
loadInitialData();

// Add this new function to handle sorting
function sortCryptoTable(column) {
    const tbody = document.getElementById('crypto-list');
    const rows = Array.from(tbody.getElementsByTagName('tr'));
    
    // Toggle sort direction if clicking the same column
    if (currentSortColumn === column) {
        sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
        currentSortColumn = column;
        sortDirection = 'asc';
    }

    // Update sort icons
    document.querySelectorAll('.fa-sort').forEach(icon => {
        icon.className = 'fas fa-sort ms-1';
    });
    const currentIcon = document.querySelector(`[data-sort="${column}"]`);
    currentIcon.className = `fas fa-sort-${sortDirection === 'asc' ? 'up' : 'down'} ms-1`;

    // Sort the rows
    rows.sort((a, b) => {
        let aValue, bValue;
        
        switch(column) {
            case 'name':
                aValue = a.querySelector('.crypto-name-cell').textContent.trim();
                bValue = b.querySelector('.crypto-name-cell').textContent.trim();
                break;
            case 'symbol':
                aValue = a.getAttribute('data-symbol');
                bValue = b.getAttribute('data-symbol');
                break;
            case 'price':
                aValue = parseFloat(a.querySelector('.crypto-price').textContent);
                bValue = parseFloat(b.querySelector('.crypto-price').textContent);
                break;
            case 'change24h':
                aValue = parseFloat(a.querySelector('.price-change').textContent);
                bValue = parseFloat(b.querySelector('.price-change').textContent);
                break;
        }

        if (sortDirection === 'asc') {
            return aValue > bValue ? 1 : -1;
        } else {
            return aValue < bValue ? 1 : -1;
        }
    });

    // Reorder the table
    rows.forEach(row => tbody.appendChild(row));
}

// Update the event listeners for sorting
document.addEventListener('DOMContentLoaded', () => {
    // Listen for clicks on the entire header cell
    document.querySelectorAll('#crypto-table th').forEach(header => {
        header.addEventListener('click', (e) => {
            const column = header.querySelector('i').getAttribute('data-sort');
            sortCryptoTable(column);
        });
    });
});

// Update the currency selector click handler
function initializeCurrencySelector() {
    const addButton = document.getElementById('add-currency-btn');
    const searchContainer = document.getElementById('currency-search-container');
    const searchInput = document.getElementById('currency-search-input');
    const suggestionsContainer = document.getElementById('currency-suggestions');

    // Fetch available pairs when initializing
    fetchAvailablePairs();

    addButton.addEventListener('click', () => {
        searchContainer.classList.toggle('active');
        if (searchContainer.classList.contains('active')) {
            searchInput.focus();
            // Refresh available pairs when opening the search
            fetchAvailablePairs();
        }
    });

    searchInput.addEventListener('input', debounce(async (e) => {
        const searchTerm = e.target.value.toLowerCase();
        suggestionsContainer.innerHTML = '';

        if (searchTerm.length < 2) return;

        // Get all available pairs
        const allPairs = Array.from(availablePairs.entries());
        
        // Create a function to score matches
        const getMatchScore = (pair) => {
            const [key] = pair;
            const [base, quote] = key.split('/');
            let score = 0;
            
            // Exact matches get highest score
            if (base.toLowerCase() === searchTerm) score += 10;
            if (quote.toLowerCase() === searchTerm) score += 8;
            
            // Starts with search term
            if (base.toLowerCase().startsWith(searchTerm)) score += 6;
            if (quote.toLowerCase().startsWith(searchTerm)) score += 5;
            
            // Contains search term
            if (base.toLowerCase().includes(searchTerm)) score += 3;
            if (quote.toLowerCase().includes(searchTerm)) score += 2;
            
            // Match in full pair string
            if (key.toLowerCase().includes(searchTerm)) score += 1;
            
            return score;
        };

        // Filter and sort matches
        const matches = allPairs
            .map(pair => ({ pair, score: getMatchScore(pair) }))
            .filter(({ score }) => score > 0)
            .sort((a, b) => b.score - a.score)
            .slice(0, 10)
            .map(({ pair }) => pair);

        // Create suggestion elements
        matches.forEach(([key, value]) => {
            const div = document.createElement('div');
            div.className = 'currency-suggestion-item';
            
            // Split the pair and handle special cases (like XXBT → BTC)
            const [base, quote] = value.split('/');
            const displayBase = formatCurrencyCode(base);
            const displayQuote = formatCurrencyCode(quote);
            
            const searchTermUpper = searchTerm.toUpperCase();
            
            // Highlight matching parts
            const baseHtml = highlightMatch(displayBase, searchTermUpper);
            const quoteHtml = highlightMatch(displayQuote, searchTermUpper);
            
            div.innerHTML = `
                <span class="pair-name">${baseHtml}/<span class="quote-currency">${quoteHtml}</span></span>
                ${subscribedPairs.has(value) ? '<span class="subscribed-tag">Subscribed</span>' : ''}
            `;
            
            // Add click handler
            div.addEventListener('click', () => {
                if (!subscribedPairs.has(value)) {
                    searchInput.value = value;
                    subscribedPairs.add(value);
                    suggestionsContainer.innerHTML = '';
                    
                    // Add to price table
                    addCurrencyToTable(value);
                    
                    // Subscribe to websocket updates
                    if (ws && ws.readyState === WebSocket.OPEN) {
                        subscribeToCurrency(value);
                    }
                }
            });

            // Add subscribed state
            if (subscribedPairs.has(value)) {
                div.classList.add('subscribed');
            }
            
            suggestionsContainer.appendChild(div);
        });
    }, 300));

    // Close suggestions when clicking outside
    document.addEventListener('click', (e) => {
        if (!searchContainer.contains(e.target) && !addButton.contains(e.target)) {
            searchContainer.classList.remove('active');
        }
    });
}

// Debounce helper function
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Add this to your initialization code
document.addEventListener('DOMContentLoaded', () => {
    // ... existing initialization code ...
    initializeCurrencySelector();
});

// Add this new function for notifications
function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    
    document.body.appendChild(notification);
    
    // Trigger animation
    setTimeout(() => notification.classList.add('show'), 100);
    
    // Remove notification after duration
    setTimeout(() => {
        notification.classList.remove('show');
        setTimeout(() => notification.remove(), 300);
    }, NOTIFICATION_DURATION);
}

// Add this function to check if a currency is actually displayed
function isCurrencyDisplayed(symbol) {
    return !!document.querySelector(`tr[data-symbol="${symbol}"]`);
}

// Helper function to format currency codes
function formatCurrencyCode(code) {
    // Handle special Kraken currency codes
    const replacements = {
        'XXBT': 'BTC',
        'XETH': 'ETH',
        'ZEUR': 'EUR',
        'ZUSD': 'USD',
        'ZGBP': 'GBP',
        'ZJPY': 'JPY'
    };
    return replacements[code] || code;
}

// Helper function to highlight matching text
function highlightMatch(text, searchTerm) {
    if (!searchTerm) return text;
    const regex = new RegExp(`(${searchTerm})`, 'gi');
    return text.replace(regex, '<strong>$1</strong>');
}

// Add this function to handle adding new currencies to the table
function addCurrencyToTable(symbol) {
    try {
        // Check if currency is already in table
        if (document.querySelector(`tr[data-symbol="${symbol}"]`)) {
            showNotification('Currency already added', 'warning');
            return;
        }

        // Add loading state row
        const row = document.createElement('tr');
        row.setAttribute('data-symbol', symbol);
        
        // Extract the base currency from the pair (e.g., "BTC" from "BTC/USD")
        const baseCurrency = formatCurrencyCode(symbol.split('/')[0]);
        
        row.innerHTML = `
            <td class="crypto-name-cell">
                <img src="https://cdn.jsdelivr.net/gh/atomiclabs/cryptocurrency-icons@1a63530be6e374711a8554f31b17e4cb92c25fa5/32/color/${baseCurrency.toLowerCase()}.png"
                     class="crypto-icon"
                     onerror="this.src='https://cdn.jsdelivr.net/gh/atomiclabs/cryptocurrency-icons@1a63530be6e374711a8554f31b17e4cb92c25fa5/32/color/generic.png'"
                     alt="${baseCurrency}">
                ${baseCurrency}
            </td>
            <td>${symbol}</td>
            <td class="crypto-price"><i class="fas fa-spinner fa-spin"></i></td>
            <td class="price-change"><i class="fas fa-spinner fa-spin"></i></td>
            <td class="actions">
                <button class="btn btn-sm btn-danger remove-currency" onclick="removeCurrency('${symbol}')">
                    <i class="fas fa-times"></i>
                </button>
            </td>
        `;
        
        // Add to crypto list
        const cryptoList = document.getElementById('crypto-list');
        cryptoList.appendChild(row);

        // Add to select dropdown if not exists
        const cryptoSelect = document.getElementById('crypto-select');
        if (!cryptoSelect.querySelector(`option[value="${symbol}"]`)) {
            const option = document.createElement('option');
            option.value = symbol;
            option.textContent = symbol;
            cryptoSelect.appendChild(option);
        }

        // Subscribe to updates
        subscribeToCurrency(symbol);
        
        showNotification(`Added ${symbol} to watchlist`, 'success');
    } catch (error) {
        console.error('Error adding currency to table:', error);
        showNotification('Failed to add currency', 'error');
    }
}

// Add this function to handle currency removal
function removeCurrency(symbol) {
    try {
        // Remove from table
        const row = document.querySelector(`tr[data-symbol="${symbol}"]`);
        if (row) {
            row.remove();
        }

        // Remove from select dropdown
        const option = document.querySelector(`option[value="${symbol}"]`);
        if (option) {
            option.remove();
        }

        // Remove from subscribed pairs
        subscribedPairs.delete(symbol);

        // Unsubscribe from updates if needed
        if (ws && ws.readyState === WebSocket.OPEN) {
            const unsubscribeMessage = {
                event: "unsubscribe",
                pair: [symbol]
            };
            ws.send(JSON.stringify(unsubscribeMessage));
        }

        showNotification(`Removed ${symbol} from watchlist`, 'success');
    } catch (error) {
        console.error('Error removing currency:', error);
        showNotification('Failed to remove currency', 'error');
    }
} 