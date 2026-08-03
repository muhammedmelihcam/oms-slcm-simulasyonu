import { useTheme } from './theme'

export default function ThemeSwitcher() {
  const { theme, setTheme } = useTheme()

  return (
    <div className="theme-switcher">
      <button
        type="button"
        className={theme === 'light' ? 'active' : ''}
        onClick={() => setTheme('light')}
        aria-label="Light mode"
        title="Light mode"
      >
        ☀️
      </button>
      <button
        type="button"
        className={theme === 'dark' ? 'active' : ''}
        onClick={() => setTheme('dark')}
        aria-label="Dark mode"
        title="Dark mode"
      >
        🌙
      </button>
    </div>
  )
}
