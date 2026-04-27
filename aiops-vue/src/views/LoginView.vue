<template>
  <div class="min-h-screen flex items-center justify-center bg-slate-950">
    <div class="w-full max-w-md p-8 rounded-2xl border border-slate-700 bg-slate-900/80 shadow-2xl">
      <h1 class="text-2xl font-bold text-slate-100 mb-2 text-center">🛡️ AIOps Monitor</h1>
      <p class="text-slate-400 text-sm text-center mb-8">登录以访问监控面板</p>

      <el-form :model="form" :rules="rules" ref="formRef" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large"
            prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="w-full mt-2" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <p class="text-center text-slate-400 text-sm mt-6">
        还没有账号？
        <router-link to="/register" class="text-cyan-400 hover:text-cyan-300">立即注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const { data } = await login(form)
    auth.setAuth(data)
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '用户名或密码错误')
  } finally {
    loading.value = false
  }
}
</script>
