<template>
  <div class="expert-page">
    <AiExpertPanel />
  </div>
</template>

<script setup>
import AiExpertPanel from '../components/AiExpertPanel.vue'
</script>

<style scoped>
.expert-page {
  height: calc(100vh - 164px);
  min-height: 680px;
}

.expert-page :deep(.ai-expert-panel) {
  height: 100%;
  border-left: none;
  border: 1px solid var(--line);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

@media (max-width: 1280px) {
  .expert-page {
    height: auto;
    min-height: 640px;
  }

  .expert-page :deep(.ai-expert-panel) {
    border-top: 1px solid var(--line);
  }
}
</style>
