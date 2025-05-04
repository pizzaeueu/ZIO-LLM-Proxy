import React, { useState } from 'react';
import { TextField, Paper, Typography, Container } from '@mui/material';

const TextInput: React.FC = () => {
  const [text, setText] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setText(e.target.value);
  };

  return (
    <Container maxWidth={false} sx={{ height: '100%', py: 2 }}>
      <Paper 
        elevation={0} 
        sx={{ 
          height: '100%',
          display: 'flex',
          flexDirection: 'column',
          p: 2,
          backgroundColor: 'transparent'
        }}
      >
        <TextField
          fullWidth
          multiline
          value={text}
          onChange={handleChange}
          placeholder="Введите ваш текст..."
          variant="outlined"
          sx={{
            flex: 1,
            '& .MuiOutlinedInput-root': {
              height: '100%',
              '& textarea': {
                height: '100% !important'
              }
            }
          }}
        />
        <Typography 
          variant="caption" 
          color="text.secondary" 
          align="right"
          sx={{ mt: 1 }}
        >
          {text.length} символов
        </Typography>
      </Paper>
    </Container>
  );
};

export default TextInput; 