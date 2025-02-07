import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Typography
} from '@mui/material';

interface TradeDialogProps {
  open: boolean;
  onClose: () => void;
  onTrade: (quantity: number) => void;
  symbol: string;
  type: 'buy' | 'sell';
  currentPrice?: number;
}

export const TradeDialog: React.FC<TradeDialogProps> = ({
  open,
  onClose,
  onTrade,
  symbol,
  type,
  currentPrice
}) => {
  const [quantity, setQuantity] = useState<string>('');
  const [error, setError] = useState<string>('');

  const handleQuantityChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    if (value === '' || /^\d*\.?\d*$/.test(value)) {
      setQuantity(value);
      setError('');
    }
  };

  const handleTrade = () => {
    const parsedQuantity = parseFloat(quantity);
    if (isNaN(parsedQuantity) || parsedQuantity <= 0) {
      setError('Please enter a valid quantity');
      return;
    }
    onTrade(parsedQuantity);
    setQuantity('');
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose}>
      <DialogTitle>
        {type === 'buy' ? 'Buy' : 'Sell'} {symbol}
      </DialogTitle>
      <DialogContent>
        {currentPrice && (
          <Typography variant="body1" gutterBottom>
            Current Price: ${currentPrice.toFixed(2)}
          </Typography>
        )}
        <TextField
          autoFocus
          margin="dense"
          label="Quantity"
          type="text"
          fullWidth
          value={quantity}
          onChange={handleQuantityChange}
          error={!!error}
          helperText={error}
        />
        {currentPrice && quantity && !error && (
          <Typography variant="body2" color="textSecondary" sx={{ mt: 1 }}>
            Total: ${(parseFloat(quantity) * currentPrice).toFixed(2)}
          </Typography>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          Cancel
        </Button>
        <Button onClick={handleTrade} color="primary" variant="contained">
          {type === 'buy' ? 'Buy' : 'Sell'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}; 