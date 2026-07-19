import { useState } from 'react'
import { api, setToken } from './api'
import { useLanguage } from './i18n'
import LanguageSwitcher from './LanguageSwitcher'

export default function AuthPage({ onAuthenticated }) {
  const { t, translateApiMessage } = useLanguage()
  const [mode, setMode] = useState('signin')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      if (mode === 'signup') {
        await api.signup(email, password)
      }
      const { token } = await api.signin(email, password)
      setToken(token)
      onAuthenticated()
    } catch (err) {
      setError(translateApiMessage(err.message))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <LanguageSwitcher />
      <form className="auth-card" onSubmit={handleSubmit}>
        <h1>{t('appTitle')}</h1>
        <div className="auth-tabs">
          <button
            type="button"
            className={mode === 'signin' ? 'active' : ''}
            onClick={() => setMode('signin')}
          >
            {t('signIn')}
          </button>
          <button
            type="button"
            className={mode === 'signup' ? 'active' : ''}
            onClick={() => setMode('signup')}
          >
            {t('signUp')}
          </button>
        </div>

        <label>
          {t('email')}
          <input
            type="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </label>
        <label>
          {t('password')}
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={6}
            required
          />
        </label>

        {error && <p className="error">{error}</p>}

        <button type="submit" className="primary" disabled={loading}>
          {loading ? t('pleaseWait') : mode === 'signin' ? t('signIn') : t('signUpAndSignIn')}
        </button>
      </form>
    </div>
  )
}
