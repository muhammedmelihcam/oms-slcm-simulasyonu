import { createContext, useContext, useEffect, useMemo, useState } from 'react'

const LANG_KEY = 'oms-slcm-lang'

const translations = {
  tr: {
    appTitle: 'OMS/SLCM Simülasyonu',
    signIn: 'Giriş Yap',
    signUp: 'Kayıt Ol',
    signUpAndSignIn: 'Kayıt Ol ve Giriş Yap',
    pleaseWait: 'Bekleyin...',
    email: 'E-posta',
    password: 'Şifre',
    logout: 'Çıkış Yap',
    queryOwnerHeading: 'Abone Sorgula',
    msisdnPlaceholder: 'MSISDN (örn. 5551112233)',
    query: 'Sorgula',
    msisdnLabel: 'MSISDN',
    typeLabel: 'Tip',
    statusLabel: 'Durum',
    orderTrackingHeading: 'Sipariş Takibi',
    failedPrefix: 'Başarısız',
    orderCompletedMessage: 'Sipariş tamamlandı, abonelik aktifleşti.',
    catalogHeading: 'Ürün Kataloğu',
    placeOrder: 'Sipariş Ver',
    enterMsisdnFirst: 'Önce sol taraftan bir MSISDN girin.',
    eligibleForSubscriber: '✓ Bu abone için uygun',
    notEligibleForSubscriber: '✕ Bu abone için uygun değil',
    subscriberBarred: '✕ MSISDN engelli, sipariş verilemez',
    activeSubscriptionsHeading: 'Aktif Abonelikler',
    noActiveSubscriptions: 'Aktif abonelik yok.',
  },
  en: {
    appTitle: 'OMS/SLCM Simulation',
    signIn: 'Sign In',
    signUp: 'Sign Up',
    signUpAndSignIn: 'Sign Up and Sign In',
    pleaseWait: 'Please wait...',
    email: 'Email',
    password: 'Password',
    logout: 'Log Out',
    queryOwnerHeading: 'Look Up Subscriber',
    msisdnPlaceholder: 'MSISDN (e.g. 5551112233)',
    query: 'Look Up',
    msisdnLabel: 'MSISDN',
    typeLabel: 'Type',
    statusLabel: 'Status',
    orderTrackingHeading: 'Order Tracking',
    failedPrefix: 'Failed',
    orderCompletedMessage: 'Order completed, subscription activated.',
    catalogHeading: 'Product Catalog',
    placeOrder: 'Place Order',
    enterMsisdnFirst: 'Enter an MSISDN on the left first.',
    eligibleForSubscriber: '✓ Eligible for this subscriber',
    notEligibleForSubscriber: '✕ Not eligible for this subscriber',
    subscriberBarred: '✕ MSISDN barred, cannot order',
    activeSubscriptionsHeading: 'Active Subscriptions',
    noActiveSubscriptions: 'No active subscriptions.',
  },
}

// Backend enum values are fixed English strings (OrderStatus, TargetSegment,
// SubscriberStatus) - translated for display here rather than touched
// server-side, since the set is small and finite.
const STATUS_LABELS = {
  PENDING: { tr: 'Beklemede', en: 'Pending' },
  VALIDATING: { tr: 'Doğrulanıyor', en: 'Validating' },
  PROVISIONING: { tr: 'Kuruluyor', en: 'Provisioning' },
  COMPLETED: { tr: 'Tamamlandı', en: 'Completed' },
  FAILED: { tr: 'Başarısız', en: 'Failed' },
}

// Also used for SubscriberType (B2C/B2B) - same code space as segment, ALL
// just never appears there.
const SEGMENT_LABELS = {
  B2C: { tr: 'Bireysel', en: 'B2C' },
  B2B: { tr: 'Kurumsal', en: 'B2B' },
  ALL: { tr: 'Tümü', en: 'All' },
}

const SUBSCRIBER_STATUS_LABELS = {
  ACTIVE: { tr: 'Aktif', en: 'Active' },
  BARRED: { tr: 'Engelli', en: 'Barred' },
}

// The state machine's fixed failure reasons (see OrderProcessingService).
// Anything not in this map (e.g. a dynamic "Internal error: ..." message)
// falls back to the raw backend string.
const FAILURE_REASON_LABELS = {
  'MSISDN is barred': { tr: 'MSISDN engellenmiş', en: 'MSISDN is barred' },
  'Product not eligible for subscriber segment': {
    tr: 'Ürün, abone segmentine uygun değil',
    en: 'Product not eligible for subscriber segment',
  },
  'Subscriber already has an active subscription to this product': {
    tr: 'Abonenin bu ürüne zaten aktif bir aboneliği var',
    en: 'Subscriber already has an active subscription to this product',
  },
  'Subscriber not found': { tr: 'Abone bulunamadı', en: 'Subscriber not found' },
  'Product not found': { tr: 'Ürün bulunamadı', en: 'Product not found' },
}

// Backend API error messages carry dynamic data (msisdn, orderId, email), so
// these are matched by pattern rather than exact string. Only translates
// into Turkish - the backend already produces English, so there's nothing to
// do when the UI language is English.
const API_MESSAGE_PATTERNS = [
  [/^Subscriber not found: (.+)$/, (m) => `Abone bulunamadı: ${m[1]}`],
  [/^Order not found: (.+)$/, (m) => `Sipariş bulunamadı: ${m[1]}`],
  [/^Invalid email or password$/, () => 'Geçersiz e-posta veya şifre'],
  [/^Email already registered: (.+)$/, (m) => `Bu e-posta zaten kayıtlı: ${m[1]}`],
]

const LanguageContext = createContext(null)

export function LanguageProvider({ children }) {
  const [lang, setLang] = useState(() => localStorage.getItem(LANG_KEY) || 'tr')

  useEffect(() => {
    localStorage.setItem(LANG_KEY, lang)
  }, [lang])

  const value = useMemo(() => {
    const dict = translations[lang]
    const t = (key) => dict[key] ?? key
    const translateStatus = (status) => STATUS_LABELS[status]?.[lang] ?? status
    const translateSegment = (segment) => SEGMENT_LABELS[segment]?.[lang] ?? segment
    const translateSubscriberStatus = (status) => SUBSCRIBER_STATUS_LABELS[status]?.[lang] ?? status
    const translateReason = (reason) => FAILURE_REASON_LABELS[reason]?.[lang] ?? reason
    const translateApiMessage = (message) => {
      if (!message || lang !== 'tr') return message
      for (const [pattern, toTurkish] of API_MESSAGE_PATTERNS) {
        const match = message.match(pattern)
        if (match) return toTurkish(match)
      }
      return message
    }

    return {
      lang,
      setLang,
      t,
      translateStatus,
      translateSegment,
      translateSubscriberStatus,
      translateReason,
      translateApiMessage,
    }
  }, [lang])

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

export function useLanguage() {
  const ctx = useContext(LanguageContext)
  if (!ctx) throw new Error('useLanguage must be used within LanguageProvider')
  return ctx
}
