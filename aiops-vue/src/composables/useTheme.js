import { ref } from 'vue'

const STORAGE_KEY = 'ui-theme'
const FALLBACK_THEME = 'dark'

function resolveInitialTheme() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'dark' || saved === 'light') {
    return saved
  }
  const prefersLight = window.matchMedia?.('(prefers-color-scheme: light)').matches
  return prefersLight ? 'light' : FALLBACK_THEME
}

export function applyTheme(theme) {
  const normalized = theme === 'light' ? 'light' : 'dark'
  document.documentElement.setAttribute('data-theme', normalized)
  localStorage.setItem(STORAGE_KEY, normalized)
  return normalized
}

export function initTheme() {
  return applyTheme(resolveInitialTheme())
}

export function useTheme() {
  const theme = ref(resolveInitialTheme())

  function setTheme(nextTheme) {
    theme.value = applyTheme(nextTheme)
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  applyTheme(theme.value)

  return {
    theme,
    setTheme,
    toggleTheme
  }
}
