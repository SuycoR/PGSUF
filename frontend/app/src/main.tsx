import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

// Global theme first, so component stylesheets can override it.
import './styles/theme.css';
import App from './App';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
