import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button
} from '@mui/material';
import { CryptoPriceData } from '../services/api';

interface PriceTableProps {
  prices: CryptoPriceData[];
  onBuy: (symbol: string) => void;
  onSell: (symbol: string) => void;
}

export const PriceTable: React.FC<PriceTableProps> = ({ prices, onBuy, onSell }) => {
  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Symbol</TableCell>
            <TableCell align="right">Price (USD)</TableCell>
            <TableCell align="right">Last Updated</TableCell>
            <TableCell align="center">Actions</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {prices.map((price) => (
            <TableRow key={price.symbol}>
              <TableCell component="th" scope="row">
                {price.symbol}
              </TableCell>
              <TableCell align="right">${price.price.toFixed(2)}</TableCell>
              <TableCell align="center">
                <Button
                  variant="contained"
                  color="primary"
                  size="small"
                  onClick={() => onBuy(price.symbol)}
                  sx={{ mr: 1 }}
                >
                  Buy
                </Button>
                <Button
                  variant="contained"
                  color="secondary"
                  size="small"
                  onClick={() => onSell(price.symbol)}
                >
                  Sell
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
}; 