import { useEffect, useRef, useState } from 'react'
import { api, clearToken } from './api'

const ORDER_STEPS = ['PENDING', 'VALIDATING', 'PROVISIONING', 'COMPLETED']
const POLL_INTERVAL_MS = 2000

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
  const [msisdn, setMsisdn] = useState('')
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
      .catch((err) => setProductsError(err.message))
    return () => clearInterval(pollRef.current)
  }, [])

  async function checkEligibility(e) {
    e.preventDefault()
    setEligibilityError(null)
    setEligibility(null)
    setEligibilityLoading(true)
    try {
      setEligibility(await api.getEligibility(msisdn))
    } catch (err) {
      setEligibilityError(err.message)
    } finally {
      setEligibilityLoading(false)
    }
  }

  async function placeOrder(productCode) {
    if (!msisdn) {
      setOrderError('Önce sol taraftan bir MSISDN girin.')
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
          setOrderError(err.message)
        }
      }, POLL_INTERVAL_MS)
    } catch (err) {
      setOrderError(err.message)
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
        <h1>OMS/SLCM Simulasyonu</h1>
        <button type="button" onClick={handleLogout}>Çıkış Yap</button>
      </header>

      <div className="dashboard-body">
        <section className="panel eligibility-panel">
          <h2>Abone Sorgula</h2>
          <form onSubmit={checkEligibility}>
            <input
              placeholder="MSISDN (örn. 5551112233)"
              value={msisdn}
              onChange={(e) => setMsisdn(e.target.value)}
              required
            />
            <button type="submit" className="primary" disabled={eligibilityLoading}>
              Sorgula
            </button>
          </form>
          {eligibilityError && <p className="error">{eligibilityError}</p>}
          {eligibility && (
            <div className="eligibility-result">
              <p><strong>MSISDN:</strong> {eligibility.msisdn}</p>
              <p><strong>Tip:</strong> {eligibility.type}</p>
              <p><strong>Durum:</strong> {eligibility.status}</p>
            </div>
          )}

          {order && (
            <div className="order-progress">
              <h3>Sipariş Takibi</h3>
              <p className="order-id">{order.orderId}</p>
              <ol className="timeline">
                {buildTimeline(order.status).map((step) => (
                  <li key={step.label} className={`timeline-${step.state}`}>
                    {step.label}
                  </li>
                ))}
              </ol>
              {order.status === 'FAILED' && <p className="error">Başarısız: {order.reason}</p>}
              {order.status === 'COMPLETED' && <p className="success">Sipariş tamamlandı, abonelik aktifleşti.</p>}
            </div>
          )}
          {orderError && <p className="error">{orderError}</p>}
        </section>

        <section className="panel catalog-panel">
          <h2>Ürün Kataloğu</h2>
          {productsError && <p className="error">{productsError}</p>}
          <div className="product-grid">
            {products.map((product) => (
              <div className="product-card" key={product.productCode}>
                <h3>{product.name}</h3>
                <p className="segment">{product.segment}</p>
                <p className="code">{product.productCode}</p>
                <button type="button" className="primary" onClick={() => placeOrder(product.productCode)}>
                  Sipariş Ver
                </button>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
