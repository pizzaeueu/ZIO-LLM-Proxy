import {
  ThemeProvider, 
  createTheme, 
  CssBaseline,
  Box 
} from '@mui/material';
import Chat from './components/Chat';

const theme = createTheme({
  palette: {
    mode: 'light',
    background: {
      default: '#f5f5f5'
    }
  }
});

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ 
        height: '100vh',
        display: 'flex',
        flexDirection: 'column',
        backgroundColor: '#f5f5f5'
      }}>
        <Box sx={{ 
          flex: 1, 
          overflow: 'hidden',
          flexDirection: 'column'
        }}>
          <Chat />
        </Box>
      </Box>
    </ThemeProvider>
  );
}

export default App;
