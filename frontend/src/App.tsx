import React, { useEffect, useState } from 'react';
import { Container, Grid, Box, Alert, Snackbar } from '@mui/material';
import { PriceTable } from './components/PriceTable';
import { Portfolio } from './components/Portfolio';
import { TradeDialog } from './components/TradeDialog';
import { api, webSocketService, CryptoPriceData, Portfolio as PortfolioType, TradeRequest } from './services/api';

function App() {
  const [prices, setPrices] = useState<CryptoPriceData[]>([]);
  const [portfolio, setPortfolio] = useState<PortfolioType>({
    balance: 0,
    holdings: {},
    totalValue: 0
  });
  const [tradeDialog, setTradeDialog] = useState<{
    open: boolean;
    symbol: string;
    type: 'buy' | 'sell';
    currentPrice?: number;
  }>({
    open: false,
    symbol: '',
    type: 'buy'
  });
  const [error, setError] = useState<string>('');

  const fetchInitialData = async () => {
    try {
      const data = await api.getInitialData();
      setPrices(data.prices || []);
      setPortfolio(data.portfolio || {
        balance: 0,
        holdings: {},
        totalValue: 0
      });
    } catch (err) {
      console.error('Error fetching initial data:', err);
      setError('Failed to load initial data. Please refresh the page.');
    }
  };

  const updatePrices = async () => {
    try {
      const prices = await api.getCryptoPrices();
      setPrices(prices);
    } catch (err) {
      console.error('Error fetching prices:', err);
    }
  };

  useEffect(() => {
    fetchInitialData();

    // Connect to WebSocket for real-time price updates
    webSocketService.connect((data) => {
      if (data.type === 'PRICE_UPDATE' && data.symbol && data.price !== undefined) {
        setPrices(prevPrices => {
          const index = prevPrices.findIndex(p => p.symbol === data.symbol);
          if (index === -1) {
            return [...prevPrices, {
              symbol: data.symbol as string,
              price: data.price as number,
              change24h: data.change24h || 0
            }];
          }
          const newPrices = [...prevPrices];
          newPrices[index] = {
            ...newPrices[index],
            price: data.price as number,
            change24h: data.change24h || newPrices[index].change24h || 0
          };
          return newPrices;
        });
      }
    });

    return () => {
      webSocketService.disconnect();
    };
  }, []);

  const handleBuy = (symbol: string) => {
    const price = prices.find(p => p.symbol === symbol);
    setTradeDialog({
      open: true,
      symbol,
      type: 'buy',
      currentPrice: price?.price
    });
  };

  const handleSell = (symbol: string) => {
    const price = prices.find(p => p.symbol === symbol);
    setTradeDialog({
      open: true,
      symbol,
      type: 'sell',
      currentPrice: price?.price
    });
  };

  const handleTrade = async (amount: number) => {
    try {
      const price = prices.find(p => p.symbol === tradeDialog.symbol)?.price;
      if (!price) {
        throw new Error('Price not available');
      }

      const tradeRequest: TradeRequest = {
        type: tradeDialog.type.toUpperCase() as 'BUY' | 'SELL',
        symbol: tradeDialog.symbol,
        amount,
        price
      };

      await api.executeTrade(tradeRequest);

      // Refresh data after trade
      await fetchInitialData();
      setTradeDialog(prev => ({ ...prev, open: false }));
    } catch (err: any) {
      setError(err.response?.data?.message || 'Trade failed. Please try again.');
    }
  };

  const handleReset = async () => {
    try {
      await api.resetAccount();
      await fetchInitialData();
    } catch (err) {
      setError('Failed to reset portfolio. Please try again.');
    }
  };

  return (
    <Container maxWidth="lg">
      <Box sx={{ flexGrow: 1, mt: 4 }}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={8}>
            <PriceTable
              prices={prices}
              onBuy={handleBuy}
              onSell={handleSell}
            />
          </Grid>
          <Grid item xs={12} md={4}>
            <Portfolio
              portfolio={portfolio}
              onReset={handleReset}
            />
          </Grid>
        </Grid>
        <TradeDialog
          open={tradeDialog.open}
          symbol={tradeDialog.symbol}
          type={tradeDialog.type}
          currentPrice={tradeDialog.currentPrice}
          onClose={() => setTradeDialog(prev => ({ ...prev, open: false }))}
          onTrade={handleTrade}
        />
        <Snackbar
          open={!!error}
          autoHideDuration={6000}
          onClose={() => setError('')}
        >
          <Alert severity="error" onClose={() => setError('')}>
            {error}
          </Alert>
        </Snackbar>
      </Box>
    </Container>
  );
}

export default App; 