<template>
  <div class="auth-shell">
    <div class="auth-card auth-card-wide">
      <div class="auth-brand">
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>

      <div class="auth-flow-note">
        <strong>{{ t('noticeTitle') }}</strong>
        <span>{{ t('noticeText') }}</span>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleChangePassword">
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
            @keyup.enter="handleChangePassword"
          />
        </el-form-item>

        <div class="auth-password-policy">
          <span>{{ t('policyTitle') }}</span>
          <ul>
            <li>{{ t('policyLength') }}</li>
            <li>{{ t('policyComplexity') }}</li>
          </ul>
        </div>

        <el-button type="primary" size="large" class="w-full mt-3" :loading="loading" @click="handleChangePassword">
          {{ t('submit') }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { changePassword } from '../api/auth'
import { useAuthStore } from '../stores/auth'
import { useI18n } from '../composables/useI18n'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: ''
})

const { t } = useI18n({
  title: { zh: '首次登录修改密码', en: 'Change Password After First Login' },
  subtitle: { zh: '当前账号使用的是管理员下发的临时密码，必须先修改后才能继续使用系统。', en: 'This account is using a temporary password issued by an administrator and must be updated before continuing.' },
  noticeTitle: { zh: '安全要求', en: 'Security requirement' },
  noticeText: { zh: '修改成功后，系统会解除临时密码限制，你才能继续访问看板、告警、AI 专家等页面。', en: 'Once updated, the temporary-password restriction is removed and full access is restored.' },
  passwordPlaceholder: { zh: '新密码', en: 'New password' },
  confirmPlaceholder: { zh: '确认新密码', en: 'Confirm new password' },
  policyTitle: { zh: '密码策略', en: 'Password policy' },
  policyLength: { zh: '至少 10 位', en: 'At least 10 characters' },
  policyComplexity: { zh: '包含大写、小写、数字和特殊字符', en: 'Include uppercase, lowercase, digit and symbol' },
  passwordRequired: { zh: '请输入新密码', en: 'Please enter new password' },
  passwordLength: { zh: '密码至少10位', en: 'Password must be at least 10 characters' },
  confirmRequired: { zh: '请再次输入新密码', en: 'Please confirm new password' },
  mismatch: { zh: '两次输入的密码不一致', en: 'Passwords do not match' },
  submit: { zh: '确认修改密码', en: 'Change Password' },
  success: { zh: '密码已修改，正在进入系统', en: 'Password updated, entering the system' },
  failed: { zh: '修改密码失败', en: 'Failed to change password' }
})

function validateConfirm(_rule, value, callback) {
  if (!value) {
    callback(new Error(t('confirmRequired')))
    return
  }
  if (value !== form.newPassword) {
    callback(new Error(t('mismatch')))
    return
  }
  callback()
}

const rules = computed(() => ({
  newPassword: [
    { required: true, message: t('passwordRequired'), trigger: 'blur' },
    { min: 10, message: t('passwordLength'), trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirm, trigger: 'blur' }]
}))

async function handleChangePassword() {
  await formRef.value.validate()
  loading.value = true
  try {
    await changePassword({ newPassword: form.newPassword })
    auth.setPasswordChangeRequired(false)
    ElMessage.success(t('success'))
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('failed'))
  } finally {
    loading.value = false
  }
}
</script>
