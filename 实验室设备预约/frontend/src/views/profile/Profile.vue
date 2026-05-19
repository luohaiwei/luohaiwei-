<template>
  <div class="profile">
    <el-card class="summary-card">
      <template #header>
        <div class="card-header">
          <span>账号概览</span>
        </div>
      </template>
      <div class="summary-grid">
        <div class="summary-item">
          <div class="k">账号ID</div>
          <div class="v">{{ displayField(userStore.userInfo?.id ?? userStore.userInfo?.userId) }}</div>
        </div>
        <div class="summary-item">
          <div class="k">学号/工号</div>
          <div class="v">{{ displayField(userStore.userInfo?.studentStaffNo) }}</div>
        </div>
        <div class="summary-item">
          <div class="k">所属实验室</div>
          <div class="v">{{ displayField(userStore.userInfo?.laboratory) }}</div>
        </div>
        <div class="summary-item">
          <div class="k">实验偏好</div>
          <div class="v">{{ displayField(userStore.userInfo?.experimentType) }}</div>
        </div>
        <div class="summary-item">
          <div class="k">技能等级</div>
          <div class="v">{{ skillLevelLabel }}</div>
        </div>
        <div class="summary-item">
          <div class="k">爽约次数</div>
          <div class="v">{{ displayField(userStore.userInfo?.missedCount) }}</div>
        </div>
        <div class="summary-item">
          <div class="k">账号状态</div>
          <div class="v">{{ statusLabel }}</div>
        </div>
        <div class="summary-item">
          <div class="k">最近登录</div>
          <div class="v">{{ formatDateTime(userStore.userInfo?.lastLoginTime) }}</div>
        </div>
        <div class="summary-item">
          <div class="k">注册时间</div>
          <div class="v">{{ formatDateTime(userStore.userInfo?.createTime) }}</div>
        </div>
      </div>
    </el-card>

    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
        </div>
      </template>
      <el-form :model="form" label-width="100px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="用户类型">
          <el-input :model-value="typeLabel" disabled />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="部门/班级">
          <el-input v-model="form.department" placeholder="请输入部门或班级" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <span>修改密码</span>
        </div>
      </template>
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useUserStore } from '../../stores/user'
import { updateProfile, changePassword } from '../../api/auth'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const typeLabel = computed(() => {
  const t = userStore.userInfo?.userType
  return ({ SYSTEM_ADMIN: '系统管理员', LAB_ADMIN: '实验室管理员', TEACHER: '教师', STUDENT: '学生', MAINTAINER: '设备维护人员' }[t] || t) || '-'
})
const skillLevelLabel = computed(() => {
  const lv = Number(userStore.userInfo?.skillLevel || 0)
  return ({ 1: '初学', 2: '一般', 3: '熟练' }[lv] || '-')
})
const statusLabel = computed(() => {
  const s = Number(userStore.userInfo?.status)
  return s === 1 ? '正常' : (s === 0 ? '禁用' : '-')
})

const form = reactive({
  username: '',
  realName: '',
  phone: '',
  email: '',
  department: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const displayField = (v) => {
  if (v === null || v === undefined || v === '') return '-'
  return String(v)
}

const formatDateTime = (v) => {
  if (!v) return '-'
  const d = new Date(String(v).replace(' ', 'T'))
  if (Number.isNaN(d.getTime())) return String(v)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
}

onMounted(async () => {
  try {
    await userStore.getUserInfo()
    Object.assign(form, userStore.userInfo)
  } catch (e) {
    console.error(e)
  }
})

const handleSave = async () => {
  try {
    await updateProfile({
      realName: form.realName,
      phone: form.phone,
      email: form.email,
      department: form.department
    })
    ElMessage.success('保存成功')
    userStore.getUserInfo()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

const handleChangePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    return ElMessage.warning('请填写完整')
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    return ElMessage.error('两次密码输入不一致')
  }
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (e) {
    ElMessage.error(e.message || '密码修改失败')
  }
}
</script>

<style lang="scss" scoped>
.profile {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-card,
.summary-card,
.password-card {
  background: #161B22;
  border-radius: 12px;
  border: 1px solid #30363D;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.summary-item {
  background: #0d1117;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 12px;
}

.summary-item .k {
  color: #8b949e;
  font-size: 12px;
  margin-bottom: 6px;
}

.summary-item .v {
  color: #e6edf3;
  font-size: 14px;
  word-break: break-all;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
  color: #E6EDF3;
}
</style>
