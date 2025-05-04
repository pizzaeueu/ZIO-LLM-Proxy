import React from 'react';
import { Paper, ListItemText, Box } from '@mui/material';

interface MessageProps {
  text: string;
  isUser: boolean;
  type?: string;
}

const Message: React.FC<MessageProps> = ({ text, isUser, type }) => {
  return (
    <Box sx={{
      display: 'flex',
      justifyContent: isUser ? 'flex-end' : 'flex-start',
      mb: 1
    }}>
      <Paper 
        elevation={1}
        sx={{
          p: 2,
          maxWidth: '70%',
          backgroundColor: isUser ? 'primary.main' : type === 'sensitive-info' ? 'warning.light' : 'grey.100',
          color: isUser ? 'white' : 'text.primary',
          borderRadius: 2,
        }}
      >
        <ListItemText 
          primary={text}
        />
      </Paper>
    </Box>
  );
};

export default Message; 