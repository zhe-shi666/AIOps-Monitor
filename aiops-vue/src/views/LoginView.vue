<template>
  <div class="auth-shell">
    <div class="auth-card">
      <div class="auth-brand">
        <h1>AIOps Monitor</h1>
        <p>{{ t('subtitle') }}</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('usernamePlaceholder')" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" :placeholder="t('passwordPlaceholder')" size="large"
            prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full mt-1" :loading="loading" @click="handleLogin">
          {{ t('login') }}
        </el-button>
      </el-form>

      <p class="text-center text-slate-400 text-sm mt-5">
        {{ t('noAccount') }}
        <router-link to="/register" class="text-sky-300 hover:text-sky-200 font-semibold">{{ t('registerNow') }}</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })

const { t } = useI18n({
  subtitle: { zh: '企业级智能运维平台 · 登录后进入控制台', en: 'Enterprise AIOps platform · Sign in to enter console' },
  usernamePlaceholder: { zh: '用户名', en: 'Username' },
  passwordPlaceholder: { zh: '密码', en: 'Password' },
  login: { zh: '登录', en: 'Sign In' },
  noAccount: { zh: '还没有账号？', en: 'No account yet?' },
  registerNow: { zh: '立即注册', en: 'Register now' },
  usernameRequired: { zh: '请输入用户名', en: 'Please enter username' },
  passwordRequired: { zh: '请输入密码', en: 'Please enter password' },
  invalidCredential: { zh: '用户名或密码错误', en: 'Invalid username or password' }
})

const rules = computed(() => ({
  username: [{ required: true, message: t('usernameRequired'), trigger: 'blur' }],
  password: [{ required: true, message: t('passwordRequired'), trigger: 'blur' }]
}))

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(form)
    auth.setAuth(data)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('invalidCredential'))
  } finally {
    loading.value = false
  }
}
</script>
