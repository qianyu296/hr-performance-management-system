<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageFrame from '@/components/common/PageFrame.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter(); const authStore = useAuthStore(); const saving = ref(false)
const form = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
async function submit() { if (form.newPassword.length < 12) return ElMessage.warning('新密码至少需要 12 个字符'); if (form.newPassword !== form.confirmPassword) return ElMessage.warning('两次输入的新密码不一致'); saving.value = true; try { await authStore.completePasswordChange(form.currentPassword, form.newPassword); ElMessage.success('密码已修改'); await router.replace('/dashboard') } catch { ElMessage.error('当前密码不正确或新密码不可用') } finally { saving.value = false } }
</script>
<template><PageFrame title="修改初始密码" description="首次登录必须设置新的个人密码后才能使用系统。"><section class="password-panel"><el-form label-position="top"><el-form-item label="当前临时密码"><el-input v-model="form.currentPassword" type="password" show-password/></el-form-item><el-form-item label="新密码"><el-input v-model="form.newPassword" type="password" show-password/></el-form-item><el-form-item label="确认新密码"><el-input v-model="form.confirmPassword" type="password" show-password @keyup.enter="submit"/></el-form-item><el-button type="primary" :loading="saving" @click="submit">确认修改</el-button></el-form></section></PageFrame></template>
<style scoped>.password-panel{max-width:480px}</style>
