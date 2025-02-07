import axios from 'axios';
export { webSocketService } from './websocket';

const API_BASE_URL = 'http://localhost:8080/api';

export interface CryptoPriceData {
  symbol: string;
  price: number;
  change24h: number;
}

export interface Portfolio {
  balance: number;
  holdings: {
    [key: string]: number;
  };
  totalValue: number;
}

export interface TradeRequest {
  type: 'BUY' | 'SELL';
  symbol: string;
  amount: number;
  price: number;
}

export const api = {
  getInitialData: async () => {
    const response = await axios.get(`${API_BASE_URL}/initial-data`);
    return response.data;
  },

  getCryptoPrices: async () => {
    const response = await axios.get(`${API_BASE_URL}/crypto-data`);
    return response.data.prices;
  },

  executeTrade: async (tradeRequest: TradeRequest) => {
    const response = await axios.post(`${API_BASE_URL}/trade`, tradeRequest);
    return response.data;
  },

  resetAccount: async () => {
    const response = await axios.post(`${API_BASE_URL}/reset`);
    return response.data;
  }
}; 