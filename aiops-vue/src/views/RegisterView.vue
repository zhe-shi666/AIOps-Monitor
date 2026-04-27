<template>
  <div class="auth-shell">
    <div class="auth-card">
      <div class="auth-brand">
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input v-model="form.username" :placeholder="t('usernamePlaceholder')" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="form.email" :placeholder="t('emailPlaceholder')" size="large" prefix-icon="Message" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" :placeholder="t('passwordPlaceholder')" size="large"
            prefix-icon="Lock" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full mt-2" :loading="loading" @click="handleRegister">
          {{ t('register') }}
        </el-button>
      </el-form>

      <p class="text-center text-slate-400 text-sm mt-5">
        {{ t('hasAccount') }}
        <router-link to="/login" class="text-sky-300 hover:text-sky-200 font-semibold">{{ t('loginNow') }}</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '../api/auth'
import { useI18n } from '../composables/useI18n'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', email: '', password: '' })

const { t } = useI18n({
  title: { zh: '创建工作区账号', en: 'Create Workspace Account' },
  subtitle: { zh: '创建账号后即可使用监控、告警、通知与升级策略能力', en: 'Create an account to use monitoring, incidents, channels and escalation features' },
  usernamePlaceholder: { zh: '用户名（3-50字符）', en: 'Username (3-50 chars)' },
  emailPlaceholder: { zh: '邮箱', en: 'Email' },
  passwordPlaceholder: { zh: '密码（至少6位）', en: 'Password (min 6 chars)' },
  register: { zh: '注册', en: 'Register' },
  hasAccount: { zh: '已有账号？', en: 'Already have an account?' },
  loginNow: { zh: '立即登录', en: 'Sign in now' },
  usernameRequired: { zh: '请输入用户名', en: 'Please enter username' },
  usernameLength: { zh: '用户名长度 3-50 字符', en: 'Username must be 3-50 characters' },
  emailRequired: { zh: '请输入邮箱', en: 'Please enter email' },
  emailInvalid: { zh: '邮箱格式不正确', en: 'Invalid email format' },
  passwordRequired: { zh: '请输入密码', en: 'Please enter password' },
  passwordLength: { zh: '密码至少6位', en: 'Password must be at least 6 characters' },
  registerSuccess: { zh: '注册成功，请登录', en: 'Registration successful, please sign in' },
  registerFailed: { zh: '注册失败', en: 'Registration failed' }
})

const rules = computed(() => ({
  username: [
    { required: true, message: t('usernameRequired'), trigger: 'blur' },
    { min: 3, max: 50, message: t('usernameLength'), trigger: 'blur' }
  ],
  email: [
    { required: true, message: t('emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('emailInvalid'), trigger: 'blur' }
  ],
  password: [
    { required: true, message: t('passwordRequired'), trigger: 'blur' },
    { min: 6, message: t('passwordLength'), trigger: 'blur' }
  ]
}))

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register(form)
    ElMessage.success(t('registerSuccess'))
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('registerFailed'))
  } finally {
    loading.value = false
  }
}
</script>
