<template>
  <router-view v-if="!auth.isLoggedIn" />
  <AppShell v-else />
</template>

<script setup>
import AppShell from './components/AppShell.vue'
import { useAuthStore } from './stores/auth'
const auth = useAuthStore()
</script>
