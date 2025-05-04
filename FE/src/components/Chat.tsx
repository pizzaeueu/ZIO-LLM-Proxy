import React, { useState, useRef, useEffect } from 'react';
import { 
  Box, 
  TextField, 
  Button, 
  CircularProgress,
  Alert,
  Snackbar,
  ButtonGroup
} from '@mui/material';
import SendIcon from '@mui/icons-material/Send';
import CheckIcon from '@mui/icons-material/Check';
import CloseIcon from '@mui/icons-material/Close';
import Message from './Message';
import { useChat } from '../hooks/useChat';
import { ReadyState } from 'react-use-websocket';

const Chat: React.FC = () => {
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const {
    messages,
    error,
    readyState,
    connectionStatus,
    sendChatMessage,
    isSensitiveInfo,
    sendApproval
  } = useChat();

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const handleSend = () => {
    if (input.trim()) {
      sendChatMessage(input);
      setInput('');
    }
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  return (
    <Box sx={{ 
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      maxWidth: '800px',
      margin: '0 auto',
      padding: '0 16px'
    }}>
      <Box sx={{ 
        flex: 1, 
        overflow: 'auto',
        mb: 2
      }}>
        {messages.map((message) => (
          <Message
            key={message.id}
            text={message.text}
            isUser={message.isUser}
            type={message.type}
          />
        ))}
        <div ref={messagesEndRef} />
      </Box>
      
      <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', mb: 3 }}>
        <TextField
          fullWidth
          multiline
          maxRows={4}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyPress}
          placeholder="Type your message here..."
          variant="outlined"
          size="small"
          disabled={readyState !== ReadyState.OPEN || isSensitiveInfo}
        />
        {isSensitiveInfo ? (
          <ButtonGroup variant="contained">
            <Button
              color="success"
              onClick={() => sendApproval(true)}
              startIcon={<CheckIcon />}
            >
              Approve
            </Button>
            <Button
              color="error"
              onClick={() => sendApproval(false)}
              startIcon={<CloseIcon />}
            >
              Reject
            </Button>
          </ButtonGroup>
        ) : (
          <Button 
            variant="contained" 
            onClick={handleSend}
            disabled={!input.trim() || readyState !== ReadyState.OPEN}
            endIcon={readyState === ReadyState.CONNECTING ? <CircularProgress size={20} /> : <SendIcon />}
          >
            {connectionStatus}
          </Button>
        )}
      </Box>

      <Snackbar 
        open={!!error} 
        autoHideDuration={6000}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="error" sx={{ width: '100%' }}>
          {error}
        </Alert>
      </Snackbar>
    </Box>
  );
};

export default Chat; 