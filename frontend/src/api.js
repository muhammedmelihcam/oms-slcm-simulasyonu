const BASE_URL = '/api/v1'
const TOKEN_KEY = 'oms-slcm-token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

class ApiError extends Error {
  constructor(status, body) {
    super(body?.message || body?.errors?.join(', ') || `Request failed (${status})`)
    this.status = status
    this.body = body
  }
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (auth) {
    const token = getToken()
    if (token) headers.Authorization = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (response.status === 401 && auth) {
    clearToken()
  }

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null)
    throw new ApiError(response.status, errorBody)
  }

  if (response.status === 204 || response.status === 201) {
    return null
  }
  return response.json()
}

export const api = {
  signup: (email, password) => request('/auth/signup', { method: 'POST', body: { email, password }, auth: false }),
  signin: (email, password) => request('/auth/signin', { method: 'POST', body: { email, password }, auth: false }),
  listProducts: () => request('/catalog/products'),
  getEligibility: (msisdn) => request(`/eligibility/${encodeURIComponent(msisdn)}`),
  createOrder: (msisdn, productCode) => request('/orders', { method: 'POST', body: { msisdn, productCode } }),
  getOrder: (orderId) => request(`/orders/${encodeURIComponent(orderId)}`),
}

export { ApiError }
