import React from 'react';
import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  Button,
  Box
} from '@mui/material';
import { Portfolio as PortfolioType } from '../services/api';

interface PortfolioProps {
  portfolio: PortfolioType;
  onReset: () => void;
}

export const Portfolio: React.FC<PortfolioProps> = ({ portfolio, onReset }) => {
  return (
    <Card>
      <CardContent>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          <Typography variant="h5" component="h2">
            Portfolio
          </Typography>
          <Button
            variant="outlined"
            color="warning"
            size="small"
            onClick={onReset}
          >
            Reset Portfolio
          </Button>
        </Box>
        
        <Typography color="textSecondary" gutterBottom>
          Available Balance: ${portfolio.balance.toFixed(2)}
        </Typography>
        
        <Typography color="primary" gutterBottom>
          Total Value: ${portfolio.totalValue.toFixed(2)}
        </Typography>

        <Typography variant="h6" component="h3" sx={{ mt: 2 }}>
          Holdings
        </Typography>
        
        <List>
          {Object.entries(portfolio.holdings).map(([symbol, amount]) => (
            <ListItem key={symbol}>
              <ListItemText
                primary={`${symbol}: ${amount.toFixed(8)}`}
              />
            </ListItem>
          ))}
        </List>
      </CardContent>
    </Card>
  );
}; 