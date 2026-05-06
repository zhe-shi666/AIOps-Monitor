<template>
  <div class="auth-shell">
    <div class="auth-card auth-card-wide">
      <div class="auth-brand">
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleReset">
        <el-form-item prop="token">
          <el-input
            v-model="form.token"
            :placeholder="t('tokenPlaceholder')"
            size="large"
            prefix-icon="Key"
            clearable
          />
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            :placeholder="t('passwordPlaceholder')"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            :placeholder="t('confirmPlaceholder')"
            size="large"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleReset"
          />
        </el-form-item>

        <div class="auth-password-policy">
          <span>{{ t('policyTitle') }}</span>
          <ul>
            <li>{{ t('policyLength') }}</li>
            <li>{{ t('policyComplexity') }}</li>
          </ul>
        </div>

        <el-button type="primary" size="large" class="w-full mt-3" :loading="loading" @click="handleReset">
          {{ t('resetPassword') }}
        </el-button>
      </el-form>

      <p class="text-center text-slate-400 text-sm mt-5">
        <router-link to="/forgot-password" class="text-sky-300 hover:text-sky-200 font-semibold">{{ t('needToken') }}</router-link>
        <span class="mx-2">·</span>
        <router-link to="/login" class="text-sky-300 hover:text-sky-200 font-semibold">{{ t('backLogin') }}</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { confirmPasswordReset } from '../api/auth'
import { useI18n } from '../composables/useI18n'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  token: String(route.query.token || ''),
  newPassword: '',
  confirmPassword: ''
})

const { t } = useI18n({
  title: { zh: '重置密码', en: 'Reset Password' },
  subtitle: { zh: '输入重置凭证并设置新的企业账号密码', en: 'Enter reset token and set a new enterprise password' },
  tokenPlaceholder: { zh: '重置凭证 resetToken', en: 'Reset token' },
  passwordPlaceholder: { zh: '新密码', en: 'New password' },
  confirmPlaceholder: { zh: '确认新密码', en: 'Confirm new password' },
  policyTitle: { zh: '密码策略', en: 'Password policy' },
  policyLength: { zh: '至少 10 位', en: 'At least 10 characters' },
  policyComplexity: { zh: '包含大写、小写、数字和特殊字符', en: 'Include uppercase, lowercase, digit and symbol' },
  tokenRequired: { zh: '请输入重置凭证', en: 'Please enter reset token' },
  passwordRequired: { zh: '请输入新密码', en: 'Please enter new password' },
  passwordLength: { zh: '密码至少10位', en: 'Password must be at least 10 characters' },
  confirmRequired: { zh: '请再次输入新密码', en: 'Please confirm new password' },
  passwordMismatch: { zh: '两次输入的密码不一致', en: 'Passwords do not match' },
  resetPassword: { zh: '确认重置密码', en: 'Reset Password' },
  resetSuccess: { zh: '密码已重置，请重新登录', en: 'Password reset. Please sign in again' },
  resetFailed: { zh: '重置密码失败', en: 'Failed to reset password' },
  needToken: { zh: '没有凭证？重新生成', en: 'Need a token?' },
  backLogin: { zh: '返回登录', en: 'Back to sign in' }
})

function validateConfirm(_rule, value, callback) {
  if (!value) {
    callback(new Error(t('confirmRequired')))
    return
  }
  if (value !== form.newPassword) {
    callback(new Error(t('passwordMismatch')))
    return
  }
  callback()
}

const rules = computed(() => ({
  token: [{ required: true, message: t('tokenRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('passwordRequired'), trigger: 'blur' },
    { min: 10, message: t('passwordLength'), trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}))

async function handleReset() {
  await formRef.value.validate()
  loading.value = true
  try {
    await confirmPasswordReset({ token: form.token.trim(), newPassword: form.newPassword })
    ElMessage.success(t('resetSuccess'))
    router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('resetFailed'))
  } finally {
    loading.value = false
  }
}
</script>
