<template>
  <div class="login-container">
    <div class="login-bg"></div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo">
          <el-icon :size="40"><Monitor /></el-icon>
        </div>
        <h1>实验室设备预约系统</h1>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
        <div class="login-tip">
          <span class="register-link" @click="showRegisterDialog = true">立即注册</span>
          <span class="divider">|</span>
          <span class="forgot-link" @click="showForgotDialog = true">忘记密码？/找回密码</span>
        </div>
      </el-form>
    </div>

    <!-- 注册对话框 -->
    <el-dialog v-model="showRegisterDialog" title="用户注册" width="480px" class="auth-dialog">
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请确认密码" show-password />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="registerForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="学号/工号" prop="studentStaffNo">
          <el-input v-model="registerForm.studentStaffNo" placeholder="学生填学号，教师/其他填工号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="registerForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="用户类型" prop="userType">
          <el-select v-model="registerForm.userType" placeholder="请选择用户类型" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="设备维护人员" value="MAINTAINER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegisterDialog = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>

    <!-- 忘记密码对话框 -->
    <el-dialog v-model="showForgotDialog" title="找回密码" width="400px" class="auth-dialog">
      <el-form ref="forgotFormRef" :model="forgotForm" :rules="forgotRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="forgotForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="forgotForm.phone" placeholder="请输入注册时的手机号" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="forgotForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="forgotForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForgotDialog = false">取消</el-button>
        <el-button type="primary" :loading="forgotLoading" @click="handleForgotPassword">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'
import { User, Lock, Monitor } from '@element-plus/icons-vue'
import { login, register, resetPasswordByPhone } from '../../api/auth'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 注册相关
const showRegisterDialog = ref(false)
const registerFormRef = ref()
const registerLoading = ref(false)
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  studentStaffNo: '',
  gender: undefined,
  phone: '',
  email: '',
  userType: 'STUDENT'
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  userType: [{ required: true, message: '请选择用户类型', trigger: 'change' }]
}

const handleRegister = async () => {
  if (registerLoading.value) return
  try {
    await registerFormRef.value.validate()
  } catch {
    return
  }
  registerLoading.value = true
  try {
    await register({
      username: registerForm.username,
      password: registerForm.password,
      realName: registerForm.realName,
      studentStaffNo: registerForm.studentStaffNo,
      gender: registerForm.gender,
      phone: registerForm.phone,
      email: registerForm.email,
      userType: registerForm.userType
    })
    ElMessage.success('注册成功，请登录')
    showRegisterDialog.value = false
    // 重置表单
    Object.assign(registerForm, {
      username: '',
      password: '',
      confirmPassword: '',
      realName: '',
      studentStaffNo: '',
      gender: undefined,
      phone: '',
      email: '',
      userType: 'STUDENT'
    })
  } catch {
    // 错误提示由 api/request.js 响应拦截器统一展示，避免重复 Toast
  } finally {
    registerLoading.value = false
  }
}

// 忘记密码相关
const showForgotDialog = ref(false)
const forgotFormRef = ref()
const forgotLoading = ref(false)
const forgotForm = reactive({
  username: '',
  phone: '',
  newPassword: '',
  confirmPassword: ''
})

const validateForgotConfirmPassword = (rule, value, callback) => {
  if (value !== forgotForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const forgotRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateForgotConfirmPassword, trigger: 'blur' }
  ]
}

const handleForgotPassword = async () => {
  if (forgotLoading.value) return
  try {
    await forgotFormRef.value.validate()
  } catch {
    return
  }
  forgotLoading.value = true
  try {
    await resetPasswordByPhone({
      username: forgotForm.username,
      phone: forgotForm.phone,
      newPassword: forgotForm.newPassword
    })
    ElMessage.success('密码重置成功，请使用新密码登录')
    showForgotDialog.value = false
    // 重置表单
    Object.assign(forgotForm, {
      username: '',
      phone: '',
      newPassword: '',
      confirmPassword: ''
    })
  } catch {
    // 错误提示由 api/request.js 响应拦截器统一展示，避免重复 Toast
  } finally {
    forgotLoading.value = false
  }
}

const handleLogin = async () => {
  if (loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch {
    // 错误提示由 api/request.js 响应拦截器统一展示，避免重复 Toast
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0D1117 0%, #1A1F35 100%);
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  background-image:
    radial-gradient(circle at 20% 50%, rgba(0, 212, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(123, 97, 255, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 80%, rgba(0, 255, 136, 0.05) 0%, transparent 50%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: rgba(22, 27, 34, 0.9);
  border-radius: 16px;
  border: 1px solid #30363D;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

.login-header {
  text-align: center;
  margin-bottom: 40px;

  .logo {
    width: 80px;
    height: 80px;
    margin: 0 auto 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #00D4FF 0%, #7B61FF 100%);
    border-radius: 20px;
    color: white;
    box-shadow: 0 0 30px rgba(0, 212, 255, 0.4);
  }

  h1 {
    font-size: 24px;
    font-weight: 600;
    color: #E6EDF3;
    margin-bottom: 8px;
  }

  p {
    font-size: 12px;
    color: #8B949E;
    letter-spacing: 2px;
  }
}

.login-form {
  .el-input__wrapper {
    background: #21262D !important;
    border-radius: 8px;
  }

  .el-input__inner {
    color: #E6EDF3 !important;
    &::placeholder {
      color: #6E7681 !important;
    }
  }
}

.login-btn {
  width: 100%;
  height: 48px;
  background: linear-gradient(90deg, #00D4FF 0%, #7B61FF 100%) !important;
  border: none !important;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 0 30px rgba(0, 212, 255, 0.5);
    transform: translateY(-2px);
  }
}

.login-tip {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #6E7681;

  .register-link,
  .forgot-link {
    cursor: pointer;
    color: #00D4FF;
    transition: color 0.3s;

    &:hover {
      color: #7B61FF;
    }
  }

  .divider {
    margin: 0 10px;
    color: #30363D;
  }
}

.auth-dialog {
  .el-dialog {
    background: #161B22 !important;
    border: 1px solid #30363D;
  }

  .el-dialog__title {
    color: #E6EDF3 !important;
  }

  .el-dialog__body {
    color: #E6EDF3;
  }

  .el-form-item__label {
    color: #E6EDF3 !important;
  }

  .el-input__wrapper {
    background: #21262D !important;
  }

  .el-input__inner {
    color: #E6EDF3 !important;
    &::placeholder {
      color: #6E7681 !important;
    }
  }

  .el-select {
    width: 100%;
  }

  .el-select .el-input__wrapper {
    background: #21262D !important;
  }
}
</style>
