import { useState } from 'react'
import { getToken } from './api'
import { LanguageProvider } from './i18n'
import { ThemeProvider } from './theme'
import AuthPage from './AuthPage'
import Dashboard from './Dashboard'
import './App.css'

function App() {
  const [authenticated, setAuthenticated] = useState(() => Boolean(getToken()))

  return (
    <ThemeProvider>
      <LanguageProvider>
        {authenticated ? (
          <Dashboard onLogout={() => setAuthenticated(false)} />
        ) : (
          <AuthPage onAuthenticated={() => setAuthenticated(true)} />
        )}
      </LanguageProvider>
    </ThemeProvider>
  )
}

export default App
