<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules<typeof form> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  try {
    await authStore.signIn(form)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect)
  } catch {
    ElMessage.error('账号或密码不正确，请重新输入')
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-panel" aria-labelledby="login-title">
      <div class="login-brand">
        <span class="brand-mark">H</span>
        <div>
          <strong>HRPM</strong>
          <span>人力绩效管理系统</span>
        </div>
      </div>

      <div class="login-copy">
        <h1 id="login-title">登录工作台</h1>
        <p>进入组织、人事、考勤、绩效与审批模块。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="login-form" @submit.prevent="submit">
        <el-form-item label="账号" prop="username">
          <el-input v-model.trim="form.username" size="large" autocomplete="username" placeholder="请输入账号">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" size="large" type="password" autocomplete="current-password" show-password placeholder="请输入密码">
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-button type="primary" size="large" native-type="submit" :loading="authStore.loading" class="login-submit">
          登录
        </el-button>
      </el-form>
    </section>
  </main>
</template>
