import { useState, useCallback, useEffect } from 'react';
import useWebSocket, { ReadyState } from 'react-use-websocket';
import { WS_URL } from '../config';

export interface Message {
  id: number;
  text: string;
  isUser: boolean;
  type?: string;
}

export const useChat = () => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isSensitiveInfo, setIsSensitiveInfo] = useState(false);

  const { sendMessage, lastMessage, readyState } = useWebSocket(WS_URL, {
    onOpen: () => {
      console.log('Connected to:', WS_URL);
      setError(null);
    },
    onClose: () => {
      console.log('Disconnected from:', WS_URL);
      setError('Trying to reconnect...');
    },
    onError: (event) => {
      console.error('Error connecting to:', WS_URL, event);
      setError('Connection error');
    },
    shouldReconnect: () => true,
    reconnectInterval: 5000,
  });

  useEffect(() => {
    if (lastMessage !== null) {
      try {
        const data = JSON.parse(lastMessage.data);
        
        if (data.type === 'sensitive-info') {
          setIsSensitiveInfo(true);
          addMessage(data.text, false, 'sensitive-info');
        } else if (data && typeof data.text === 'string') {
          addMessage(data.text, false);
        } else {
          console.warn('Received message of incorrect format:', data);
        }
      } catch (error) {
        console.error('Error processing message:', error);
        console.error('Raw data:', lastMessage.data);
        addMessage('Error processing server response', false);
      }
    }
  }, [lastMessage]);

  const addMessage = useCallback((text: string, isUser: boolean, type?: string) => {
    const newMessage: Message = {
      id: Date.now(),
      text,
      isUser,
      type
    };
    setMessages(prev => [...prev, newMessage]);
  }, []);

  const sendChatMessage = useCallback((text: string) => {
    if (text.trim()) {
      addMessage(text.trim(), true);
      sendMessage(JSON.stringify({ text: text.trim() }));
    }
  }, [sendMessage, addMessage]);

  const sendApproval = useCallback((allow: boolean) => {
    sendMessage(JSON.stringify({ allow }));
    setIsSensitiveInfo(false);
  }, [sendMessage]);

  const connectionStatus = (() => {
    switch (readyState) {
      case ReadyState.CONNECTING: return 'Connecting...';
      case ReadyState.OPEN: return 'Send';
      case ReadyState.CLOSING: return 'Closing...';
      case ReadyState.CLOSED: return 'Closed';
      default: return 'Closed';
    }
  })();

  return {
    messages,
    error,
    readyState,
    connectionStatus,
    sendChatMessage,
    isSensitiveInfo,
    sendApproval,
  };
}; 