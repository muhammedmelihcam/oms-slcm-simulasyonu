import { useLanguage } from './i18n'

export default function LanguageSwitcher() {
  const { lang, setLang } = useLanguage()

  return (
    <div className="language-switcher">
      <button type="button" className={lang === 'tr' ? 'active' : ''} onClick={() => setLang('tr')}>
        TR
      </button>
      <button type="button" className={lang === 'en' ? 'active' : ''} onClick={() => setLang('en')}>
        EN
      </button>
    </div>
  )
}
