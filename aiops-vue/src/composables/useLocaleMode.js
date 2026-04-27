import { ref } from 'vue'

const STORAGE_KEY = 'ui-locale'

function resolveInitialLocale() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'zh' || saved === 'en') return saved
  const lang = (navigator.language || '').toLowerCase()
  return lang.startsWith('zh') ? 'zh' : 'en'
}

const locale = ref(resolveInitialLocale())

function applyLocale(next) {
  const normalized = next === 'en' ? 'en' : 'zh'
  locale.value = normalized
  document.documentElement.setAttribute('lang', normalized === 'zh' ? 'zh-CN' : 'en')
  localStorage.setItem(STORAGE_KEY, normalized)
  return normalized
}

export function initLocaleMode() {
  return applyLocale(locale.value)
}

export function useLocaleMode() {
  function setLocale(next) {
    applyLocale(next)
  }

  function toggleLocale() {
    setLocale(locale.value === 'zh' ? 'en' : 'zh')
  }

  return {
    locale,
    setLocale,
    toggleLocale
  }
}
