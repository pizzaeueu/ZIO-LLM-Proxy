const config = {
  api: {
    host: import.meta.env.VITE_API_HOST || 'localhost',
    port: import.meta.env.VITE_API_PORT || '8081',
    path: import.meta.env.VITE_API_PATH || '/api/v1/model/ask'
  }
};

export const getWebSocketUrl = () => {
  return `ws://${config.api.host}:${config.api.port}${config.api.path}`;
};

export const WS_URL = getWebSocketUrl();

export default config; 