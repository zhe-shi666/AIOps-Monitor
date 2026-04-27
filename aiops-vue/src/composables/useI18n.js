import { computed } from 'vue'
import { useLocaleMode } from './useLocaleMode'

function interpolate(text, params = {}) {
  return String(text).replace(/\{(\w+)\}/g, (_, key) => {
    const value = params[key]
    return value == null ? '' : String(value)
  })
}

export function useI18n(messages = {}) {
  const { locale } = useLocaleMode()
  const isZh = computed(() => locale.value === 'zh')

  function t(key, params) {
    const item = messages[key]
    if (!item) return key
    const raw = isZh.value ? item.zh : item.en
    return interpolate(raw ?? key, params)
  }

  return {
    locale,
    isZh,
    t
  }
}
