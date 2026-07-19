import { useEffect, useRef, useState } from 'react'
import { api, clearToken } from './api'
import { useLanguage } from './i18n'
import LanguageSwitcher from './LanguageSwitcher'

const ORDER_STEPS = ['PENDING', 'VALIDATING', 'PROVISIONING', 'COMPLETED']
const POLL_INTERVAL_MS = 2000
const MSISDN_HISTORY_KEY = 'oms-slcm-msisdn-history'
const MAX_MSISDN_HISTORY = 8

function loadMsisdnHistory() {
  try {
    const parsed = JSON.parse(localStorage.getItem(MSISDN_HISTORY_KEY))
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function getEligibilityHint(product, eligibility) {
  if (!eligibility) return null
  if (eligibility.status === 'BARRED') return { key: 'subscriberBarred', state: 'barred' }
  if (product.segment === 'ALL' || product.segment === eligibility.type) {
    return { key: 'eligibleForSubscriber', state: 'eligible' }
  }
  return { key: 'notEligibleForSubscriber', state: 'not-eligible' }
}

function buildTimeline(status) {
  if (status === 'FAILED') {
    return [
      { label: 'PENDING', state: 'done' },
      { label: 'VALIDATING', state: 'done' },
      { label: 'FAILED', state: 'failed' },
    ]
  }
  const currentIndex = ORDER_STEPS.indexOf(status)
  return ORDER_STEPS.map((label, i) => {
    if (i < currentIndex || status === 'COMPLETED') return { label, state: 'done' }
    if (i === currentIndex) return { label, state: 'current' }
    return { label, state: 'pending' }
  })
}

export default function Dashboard({ onLogout }) {
  const { t, translateStatus, translateSegment, translateSubscriberStatus, translateReason, translateApiMessage } =
    useLanguage()

  const [msisdn, setMsisdn] = useState('')
  const [msisdnHistory, setMsisdnHistory] = useState(loadMsisdnHistory)
  const [eligibility, setEligibility] = useState(null)
  const [eligibilityError, setEligibilityError] = useState(null)
  const [eligibilityLoading, setEligibilityLoading] = useState(false)

  const [products, setProducts] = useState([])
  const [productsError, setProductsError] = useState(null)

  const [order, setOrder] = useState(null)
  const [orderError, setOrderError] = useState(null)
  const pollRef = useRef(null)

  useEffect(() => {
    api.listProducts()
      .then(setProducts)
      .catch((err) => setProductsError(translateApiMessage(err.message)))
    return () => clearInterval(pollRef.current)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  function rememberMsisdn(value) {
    setMsisdnHistory((prev) => {
      const next = [value, ...prev.filter((m) => m !== value)].slice(0, MAX_MSISDN_HISTORY)
      localStorage.setItem(MSISDN_HISTORY_KEY, JSON.stringify(next))
      return next
    })
  }

  async function checkEligibility(e) {
    e.preventDefault()
    setEligibilityError(null)
    setEligibility(null)
    setEligibilityLoading(true)
    rememberMsisdn(msisdn)
    try {
      setEligibility(await api.getEligibility(msisdn))
    } catch (err) {
      setEligibilityError(translateApiMessage(err.message))
    } finally {
      setEligibilityLoading(false)
    }
  }

  async function placeOrder(productCode) {
    if (!msisdn) {
      setOrderError(t('enterMsisdnFirst'))
      return
    }
    setOrderError(null)
    clearInterval(pollRef.current)
    try {
      const created = await api.createOrder(msisdn, productCode)
      setOrder(created)
      pollRef.current = setInterval(async () => {
        try {
          const updated = await api.getOrder(created.orderId)
          setOrder(updated)
          if (updated.status === 'COMPLETED' || updated.status === 'FAILED') {
            clearInterval(pollRef.current)
          }
        } catch (err) {
          clearInterval(pollRef.current)
          setOrderError(translateApiMessage(err.message))
        }
      }, POLL_INTERVAL_MS)
    } catch (err) {
      setOrderError(translateApiMessage(err.message))
    }
  }

  function handleLogout() {
    clearInterval(pollRef.current)
    clearToken()
    onLogout()
  }

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <h1>{t('appTitle')}</h1>
        <div className="dashboard-header-actions">
          <LanguageSwitcher />
          <button type="button" onClick={handleLogout}>
            {t('logout')}
          </button>
        </div>
      </header>

      <div className="dashboard-body">
        <section className="panel eligibility-panel">
          <h2>{t('queryOwnerHeading')}</h2>
          <form onSubmit={checkEligibility}>
            <input
              list="msisdn-history"
              placeholder={t('msisdnPlaceholder')}
              value={msisdn}
              onChange={(e) => setMsisdn(e.target.value)}
              required
            />
            <datalist id="msisdn-history">
              {msisdnHistory.map((m) => (
                <option key={m} value={m} />
              ))}
            </datalist>
            <button type="submit" className="primary" disabled={eligibilityLoading}>
              {t('query')}
            </button>
          </form>
          {eligibilityError && <p className="error">{eligibilityError}</p>}
          {eligibility && (
            <div className="eligibility-result">
              <p><strong>{t('msisdnLabel')}:</strong> {eligibility.msisdn}</p>
              <p><strong>{t('typeLabel')}:</strong> {translateSegment(eligibility.type)}</p>
              <p><strong>{t('statusLabel')}:</strong> {translateSubscriberStatus(eligibility.status)}</p>
            </div>
          )}

          {order && (
            <div className="order-progress">
              <h3>{t('orderTrackingHeading')}</h3>
              <p className="order-id">{order.orderId}</p>
              <ol className="timeline">
                {buildTimeline(order.status).map((step) => (
                  <li key={step.label} className={`timeline-${step.state}`}>
                    {translateStatus(step.label)}
                  </li>
                ))}
              </ol>
              {order.status === 'FAILED' && (
                <p className="error">
                  {t('failedPrefix')}: {translateReason(order.reason)}
                </p>
              )}
              {order.status === 'COMPLETED' && <p className="success">{t('orderCompletedMessage')}</p>}
            </div>
          )}
          {orderError && <p className="error">{orderError}</p>}
        </section>

        <section className="panel catalog-panel">
          <h2>{t('catalogHeading')}</h2>
          {productsError && <p className="error">{productsError}</p>}
          <div className="product-grid">
            {products.map((product) => {
              const hint = getEligibilityHint(product, eligibility)
              return (
                <div className="product-card" key={product.productCode}>
                  <h3>{product.name}</h3>
                  <p className="segment">{translateSegment(product.segment)}</p>
                  <p className="code">{product.productCode}</p>
                  {hint && <p className={`eligibility-hint ${hint.state}`}>{t(hint.key)}</p>}
                  <button type="button" className="primary" onClick={() => placeOrder(product.productCode)}>
                    {t('placeOrder')}
                  </button>
                </div>
              )
            })}
          </div>
        </section>
      </div>
    </div>
  )
}
