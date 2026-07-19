import { useState } from 'react'
import { getToken } from './api'
import AuthPage from './AuthPage'
import Dashboard from './Dashboard'
import './App.css'

function App() {
  const [authenticated, setAuthenticated] = useState(() => Boolean(getToken()))

  if (!authenticated) {
    return <AuthPage onAuthenticated={() => setAuthenticated(true)} />
  }
  return <Dashboard onLogout={() => setAuthenticated(false)} />
}

export default App
