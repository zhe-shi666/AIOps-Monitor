<template>
  <div class="auth-shell">
    <div class="auth-card auth-card-wide">
      <div class="auth-brand">
        <h1>{{ t('title') }}</h1>
        <p>{{ t('subtitle') }}</p>
      </div>

      <div class="auth-flow-note">
        <strong>{{ t('flowTitle') }}</strong>
        <span>{{ t('flowText') }}</span>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleStartReset">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            :placeholder="t('usernamePlaceholder')"
            size="large"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            :placeholder="t('emailPlaceholder')"
            size="large"
            prefix-icon="Message"
            clearable
            @keyup.enter="handleStartReset"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full mt-1" :loading="loading" @click="handleStartReset">
          {{ t('sendReset') }}
        </el-button>
      </el-form>

      <div v-if="resultMessage" class="auth-result-box" :class="{ 'auth-result-warning': sent === false }">
        <div class="auth-result-title">{{ sent ? t('mailSent') : t('notMatched') }}</div>
        <p>{{ resultMessage }}</p>
        <el-button v-if="sent" type="success" plain class="w-full mt-2" @click="goLogin">
          {{ t('backLogin') }}
        </el-button>
      </div>

      <p class="text-center text-slate-400 text-sm mt-5">
        <router-link to="/login" class="text-sky-300 hover:text-sky-200 font-semibold">{{ t('backLogin') }}</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { startPasswordReset } from '../api/auth'
import { useI18n } from '../composables/useI18n'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const resultMessage = ref('')
const sent = ref(null)

const form = reactive({ username: '', email: '' })

const { t } = useI18n({
  title: { zh: '找回密码', en: 'Recover Password' },
  subtitle: { zh: '输入用户名和绑定邮箱，匹配成功后系统会发送重置链接', en: 'Enter username and bound email. A reset link will be sent after verification.' },
  flowTitle: { zh: '邮箱验证流程', en: 'Email verification flow' },
  flowText: { zh: '系统会校验用户名与邮箱是否属于同一账号，匹配后发送 30 分钟有效的密码重置链接。', en: 'The system verifies that username and email belong to the same account, then sends a reset link valid for 30 minutes.' },
  usernamePlaceholder: { zh: '用户名', en: 'Username' },
  emailPlaceholder: { zh: '绑定邮箱', en: 'Bound email' },
  usernameRequired: { zh: '请输入用户名', en: 'Please enter username' },
  emailRequired: { zh: '请输入绑定邮箱', en: 'Please enter bound email' },
  emailInvalid: { zh: '邮箱格式不正确', en: 'Invalid email format' },
  sendReset: { zh: '发送重置邮件', en: 'Send Reset Email' },
  mailSent: { zh: '邮件已发送', en: 'Email sent' },
  notMatched: { zh: '账号校验未通过', en: 'Account verification failed' },
  sentMessage: { zh: '如果邮箱可用，请在收件箱中打开重置链接完成密码修改。', en: 'Please open the reset link in your inbox to set a new password.' },
  mismatchMessage: { zh: '用户名和邮箱不匹配，请检查后重试，或联系管理员。', en: 'Username and email do not match. Please check and retry, or contact an administrator.' },
  backLogin: { zh: '返回登录', en: 'Back to sign in' },
  requestFailed: { zh: '发送重置邮件失败', en: 'Failed to send reset email' }
})

const rules = computed(() => ({
  username: [{ required: true, message: t('usernameRequired'), trigger: 'blur' }],
  email: [
    { required: true, message: t('emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('emailInvalid'), trigger: 'blur' }
  ]
}))

async function handleStartReset() {
  await formRef.value.validate()
  loading.value = true
  resultMessage.value = ''
  sent.value = null
  try {
    const { data } = await startPasswordReset({
      username: form.username.trim(),
      email: form.email.trim()
    })
    sent.value = Boolean(data.sent)
    resultMessage.value = sent.value ? t('sentMessage') : t('mismatchMessage')
    if (sent.value) {
      ElMessage.success(t('mailSent'))
    } else {
      ElMessage.warning(t('notMatched'))
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || t('requestFailed'))
  } finally {
    loading.value = false
  }
}

function goLogin() {
  router.push('/login')
}
</script>
