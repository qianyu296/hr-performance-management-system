<script setup lang="ts">
import { ref } from 'vue'
import { http } from '@/api/http'

const health = ref('Checking API...')

async function checkApi() {
  try {
    const response = await http.get('/health')
    health.value = response.data.data.status
  } catch {
    health.value = 'Unavailable'
  }
}

void checkApi()
</script>

<template>
  <main class="shell">
    <header><span class="brand">HRPM</span><span class="environment">Development</span></header>
    <section>
      <h1>工作台</h1>
      <p>人力资源与绩效管理系统的开发基础已就绪。</p>
      <dl><dt>API 状态</dt><dd>{{ health }}</dd></dl>
    </section>
  </main>
</template>
