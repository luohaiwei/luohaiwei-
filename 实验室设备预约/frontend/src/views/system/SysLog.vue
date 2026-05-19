<template>
  <div class="sys-log-container">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>日志审计管理</h2>
      </div>
      <div style="display: flex; gap: 12px;">
        <el-button type="warning" @click="showCleanupDialog = true">
          <el-icon><Delete /></el-icon> 清理旧日志
        </el-button>
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon> 导出当前日志
        </el-button>
      </div>
    </div>

    <!-- 日志类型标签页 -->
    <div class="page-card tabs-card">
      <el-tabs v-model="activeTab" class="log-tabs">
        <el-tab-pane label="用户操作日志" name="operation">
          <template #label>
            <span class="tab-label">
              <el-icon><Document /></el-icon>
              用户操作日志
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="登录日志" name="login">
          <template #label>
            <span class="tab-label">
              <el-icon><Key /></el-icon>
              登录日志
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="设备状态变更日志" name="device">
          <template #label>
            <span class="tab-label">
              <el-icon><Monitor /></el-icon>
              设备状态变更日志
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane label="权限变更审计" name="permission">
          <template #label>
            <span class="tab-label">
              <el-icon><Grid /></el-icon>
              权限变更审计
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- ========== 用户操作日志 ========== -->
      <div v-if="activeTab === 'operation'" class="tab-content">
        <div class="search-row">
          <div class="search-field">
            <label>用户名</label>
            <el-input v-model="operationSearch.username" placeholder="用户名" clearable @keyup.enter="loadOperation" />
          </div>
          <div class="search-field">
            <label>操作模块</label>
            <el-input v-model="operationSearch.module" placeholder="操作模块" clearable @keyup.enter="loadOperation" />
          </div>
          <div class="search-field">
            <label>操作类型</label>
            <el-select v-model="operationSearch.operationType" placeholder="全部" clearable style="width: 100%">
              <el-option label="新增" value="CREATE" />
              <el-option label="修改" value="UPDATE" />
              <el-option label="删除" value="DELETE" />
              <el-option label="查询" value="QUERY" />
              <el-option label="登录" value="LOGIN" />
              <el-option label="登出" value="LOGOUT" />
              <el-option label="审核" value="AUDIT" />
              <el-option label="导出" value="EXPORT" />
              <el-option label="保存" value="SAVE" />
            </el-select>
          </div>
          <div class="search-field">
            <label>结果状态</label>
            <el-select v-model="operationSearch.status" placeholder="全部" clearable style="width: 100%">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </div>
          <div class="search-field search-field--range">
            <label>时间范围</label>
            <el-date-picker
              v-model="operationSearch.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="range-picker-el"
            />
          </div>
          <div class="search-actions">
            <el-button type="primary" @click="loadOperation">搜索</el-button>
            <el-button @click="resetOperation">重置</el-button>
          </div>
        </div>
        <el-table :data="operationList" border stripe class="mt16">
          <el-table-column prop="username" label="操作用户" width="120" />
          <el-table-column prop="userType" label="用户角色" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getRoleText(row.userType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="module" label="操作模块" width="130" />
          <el-table-column prop="operationType" label="操作类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getOpTypeTag(row.operationType || row.operation)">{{
                getOpTypeText(row.operationType || row.operation)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="operationDesc" label="操作描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="requestUrl" label="请求URL" min-width="200" show-overflow-tooltip />
          <el-table-column prop="ipAddress" label="网络来源" width="160" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="操作时间" width="170" :formatter="formatDateTime" />
        </el-table>
        <el-pagination
          v-model:current-page="operationPage"
          v-model:page-size="operationSize"
          :total="operationTotal"
          layout="total, prev, pager, next, sizes"
          style="margin-top: 16px; justify-content: flex-end;"
          @current-change="loadOperation"
          @size-change="loadOperation"
        />
      </div>

      <!-- ========== 登录日志 ========== -->
      <div v-if="activeTab === 'login'" class="tab-content">
        <div class="search-row">
          <div class="search-field">
            <label>用户名</label>
            <el-input v-model="loginSearch.username" placeholder="用户名" clearable @keyup.enter="loadLogin" />
          </div>
          <div class="search-field">
            <label>登录结果</label>
            <el-select v-model="loginSearch.status" placeholder="全部" clearable style="width: 100%">
              <el-option label="成功" :value="1" />
              <el-option label="失败" :value="0" />
            </el-select>
          </div>
          <div class="search-field search-field--range">
            <label>时间范围</label>
            <el-date-picker
              v-model="loginSearch.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="range-picker-el"
            />
          </div>
          <div class="search-actions">
            <el-button type="primary" @click="loadLogin">搜索</el-button>
            <el-button @click="resetLogin">重置</el-button>
          </div>
        </div>
        <el-table :data="loginList" border stripe class="mt16">
          <el-table-column prop="username" label="登录用户" width="130" />
          <el-table-column prop="userType" label="用户角色" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ getRoleText(row.userType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="loginType" label="登录方式" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="row.loginType === 'NORMAL' ? 'primary' : 'info'">{{ row.loginType === 'NORMAL' ? '正常登录' : '其他' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" label="登录来源" width="160" show-overflow-tooltip />
          <el-table-column prop="ipSource" label="归属说明" width="180" show-overflow-tooltip />
          <el-table-column prop="userAgent" label="浏览器UA" min-width="200" show-overflow-tooltip />
          <el-table-column prop="status" label="结果" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '成功' : '失败' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="failReason" label="失败原因" min-width="140" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.status === 0" style="color: #FF4757;">{{ row.failReason || '未知' }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="登录时间" width="170" :formatter="formatDateTime" />
        </el-table>
        <el-pagination
          v-model:current-page="loginPage"
          v-model:page-size="loginSize"
          :total="loginTotal"
          layout="total, prev, pager, next, sizes"
          style="margin-top: 16px; justify-content: flex-end;"
          @current-change="loadLogin"
          @size-change="loadLogin"
        />
      </div>

      <!-- ========== 设备状态变更日志 ========== -->
      <div v-if="activeTab === 'device'" class="tab-content">
        <div class="search-row">
          <div class="search-field">
            <label>设备名称</label>
            <el-input v-model="deviceSearch.deviceName" placeholder="设备名称" clearable @keyup.enter="loadDevice" />
          </div>
          <div class="search-field">
            <label>变更类型</label>
            <el-select v-model="deviceSearch.changeType" placeholder="全部" clearable style="width: 100%">
              <el-option label="空闲" value="IDLE" />
              <el-option label="使用中" value="IN_USE" />
              <el-option label="维修中" value="MAINTAINING" />
              <el-option label="校准中" value="CALIBRATING" />
              <el-option label="报废" value="SCRAPPED" />
            </el-select>
          </div>
          <div class="search-field">
            <label>操作人</label>
            <el-input v-model="deviceSearch.operator" placeholder="操作人" clearable @keyup.enter="loadDevice" />
          </div>
          <div class="search-field search-field--range">
            <label>时间范围</label>
            <el-date-picker
              v-model="deviceSearch.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              class="range-picker-el"
            />
          </div>
          <div class="search-actions">
            <el-button type="primary" @click="loadDevice">搜索</el-button>
            <el-button @click="resetDevice">重置</el-button>
          </div>
        </div>
        <el-table :data="deviceList" border stripe class="mt16">
          <el-table-column prop="deviceName" label="设备名称" min-width="180" />
          <el-table-column prop="deviceNo" label="设备编号" min-width="130" />
          <el-table-column label="变更类型" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="getChangeTypeTag(row.toStatus)">{{ row.toStatusText }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="变更详情" min-width="180">
            <template #default="{ row }">
              <span class="change-detail">{{ row.fromStatusText }} → {{ row.toStatusText }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="operator" label="操作人" width="120" />
          <el-table-column prop="ipAddress" label="网络来源" width="160" show-overflow-tooltip />
          <el-table-column prop="remark" label="变更原因" min-width="160" show-overflow-tooltip />
          <el-table-column prop="createTime" label="变更时间" width="170" :formatter="formatDateTime" />
        </el-table>
        <el-pagination
          v-model:current-page="devicePage"
          v-model:page-size="deviceSize"
          :total="deviceTotal"
          layout="total, prev, pager, next, sizes"
          style="margin-top: 16px; justify-content: flex-end;"
          @current-change="loadDevice"
          @size-change="loadDevice"
        />
      </div>

      <!-- ========== 权限变更审计 ========== -->
      <div v-if="activeTab === 'permission'" class="tab-content">
        <div class="search-row">
          <div class="search-field">
            <label>操作用户</label>
            <el-input v-model="permSearch.operator" placeholder="操作用户" clearable @keyup.enter="loadPermission" />
          </div>
          <div class="search-field">
            <label>变更类型</label>
            <el-select v-model="permSearch.changeType" placeholder="全部" clearable style="width: 100%">
              <el-option label="角色新增" value="ROLE_CREATE" />
              <el-option label="角色修改" value="ROLE_UPDATE" />
              <el-option label="角色删除" value="ROLE_DELETE" />
              <el-option label="权限分配" value="PERM_ASSIGN" />
              <el-option label="权限撤销" value="PERM_REVOKE" />
              <el-option label="用户角色变更" value="USER_ROLE_CHANGE" />
              <el-option label="数据权限保存" value="DATA_SCOPE_SAVE" />
            </el-select>
          </div>
          <div class="search-field search-field--range">
            <label>时间范围</label>
            <el-date-picker
              v-model="permSearch.dateRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD HH:mm:ss"
              class="range-picker-el"
            />
          </div>
          <div class="search-actions">
            <el-button type="primary" @click="loadPermission">搜索</el-button>
            <el-button @click="resetPerm">重置</el-button>
          </div>
        </div>
        <el-table :data="permList" border stripe class="mt16">
          <el-table-column prop="operator" label="操作用户" width="120" />
          <el-table-column prop="targetUser" label="目标用户" width="120">
            <template #default="{ row }">
              {{ row.targetUser || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="changeType" label="变更类型" width="130">
            <template #default="{ row }">
              <el-tag size="small" :type="getPermTypeTag(row.changeType)">{{ getPermTypeText(row.changeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetRole" label="目标角色" width="140">
            <template #default="{ row }">
              <el-tag size="small" type="warning">{{ row.targetRole || '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="changeDetail" label="变更详情" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              <span style="color: #8B949E; font-size: 13px;">{{ row.changeDetail || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="ipAddress" label="网络来源" width="180" show-overflow-tooltip />
          <el-table-column prop="createTime" label="变更时间" width="170" :formatter="formatDateTime" />
        </el-table>
        <el-pagination
          v-model:current-page="permPage"
          v-model:page-size="permSize"
          :total="permTotal"
          layout="total, prev, pager, next, sizes"
          style="margin-top: 16px; justify-content: flex-end;"
          @current-change="loadPermission"
          @size-change="loadPermission"
        />
      </div>
    </div>

    <!-- 清理日志对话框 -->
    <el-dialog v-model="showCleanupDialog" title="清理旧日志" width="500px">
      <div style="padding: 20px 0;">
        <el-form label-width="120px">
          <el-form-item label="保留日志条数">
            <el-input-number v-model="cleanupKeepCount" :min="10" :max="10000" :step="100" style="width: 100%;" />
          </el-form-item>
          <el-form-item>
            <div style="color: #8B949E; font-size: 13px;">
              <p>⚠️ 警告：此操作将删除旧日志，仅保留最近的 {{ cleanupKeepCount }} 条日志。</p>
              <p>删除的日志无法恢复，请谨慎操作！</p>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="showCleanupDialog = false">取消</el-button>
        <el-button type="danger" :loading="cleanupLoading" @click="handleCleanup">确认清理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { getOperationLogs, getLoginLogs, getDeviceStatusLogs, getPermissionAuditLogs, exportLog, cleanupOldLogs } from '@/api/syslog'
import { ElMessage } from 'element-plus'
import { Download, Document, Key, Monitor, Grid, Delete } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const activeTab = ref('operation')

// 清理日志相关
const showCleanupDialog = ref(false)
const cleanupKeepCount = ref(100)
const cleanupLoading = ref(false)

const getRoleText = (t) =>
  ({
    SYSTEM_ADMIN: '系统管理员',
    LAB_ADMIN: '实验室管理员',
    TEACHER: '教师',
    STUDENT: '学生',
    MAINTAINER: '设备维护人员'
  }[t] || (t ? `自定义(${t})` : '-'))
const getOpTypeTag = (t) => ({ CREATE: 'success', UPDATE: 'primary', DELETE: 'danger', QUERY: 'info', LOGIN: 'warning', LOGOUT: 'warning', AUDIT: 'primary', EXPORT: 'info', INSERT: 'success', SAVE: 'success', MODIFY: 'primary' }[t] || 'info')
const getOpTypeText = (t) => ({ CREATE: '新增', UPDATE: '修改', DELETE: '删除', QUERY: '查询', LOGIN: '登录', LOGOUT: '登出', AUDIT: '审核', EXPORT: '导出', INSERT: '新增', SAVE: '保存', MODIFY: '修改' }[t] || t)
const getChangeTypeTag = (t) => ({ IDLE: 'success', IN_USE: 'primary', MAINTAINING: 'warning', CALIBRATING: 'info', SCRAPPED: 'danger' }[t] || 'info')
const getPermTypeTag = (t) => ({ ROLE_CREATE: 'success', ROLE_UPDATE: 'primary', ROLE_DELETE: 'danger', PERM_ASSIGN: 'success', PERM_REVOKE: 'warning', USER_ROLE_CHANGE: 'primary', DATA_SCOPE_SAVE: 'warning' }[t] || 'info')
const getPermTypeText = (t) => ({ ROLE_CREATE: '角色新增', ROLE_UPDATE: '角色修改', ROLE_DELETE: '角色删除', PERM_ASSIGN: '权限分配', PERM_REVOKE: '权限撤销', USER_ROLE_CHANGE: '用户角色变更', DATA_SCOPE_SAVE: '数据权限保存' }[t] || t || '-')

/** 解析 axios 错误中的后端 message（含 success:false 的 JSON 体） */
const axiosErrMsg = (e, fb) => {
  const d = e?.response?.data
  if (d && typeof d === 'object' && d.message != null && String(d.message).trim() !== '') {
    return String(d.message)
  }
  if (typeof d === 'string' && d.trim()) return d
  return e?.message || fb || '请求失败'
}
const formatDateTime = (row) => dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss')

// ========== 用户操作日志 ==========
const operationSearch = reactive({ username: '', module: '', operationType: '', status: null, dateRange: null })
const operationList = ref([])
const operationPage = ref(1)
const operationSize = ref(10)
const operationTotal = ref(0)

const loadOperation = async () => {
  try {
    const params = { pageNum: operationPage.value, pageSize: operationSize.value, username: operationSearch.username, module: operationSearch.module, operationType: operationSearch.operationType, status: operationSearch.status }
    if (operationSearch.dateRange && operationSearch.dateRange.length === 2) {
      params.startTime = operationSearch.dateRange[0]
      params.endTime = operationSearch.dateRange[1]
    }
    const res = await getOperationLogs(params)
    operationList.value = res.list || []
    operationTotal.value = res.total || 0
  } catch (e) { ElMessage.error(e.message || '获取数据失败') }
}

const resetOperation = () => {
  operationSearch.username = ''
  operationSearch.module = ''
  operationSearch.operationType = ''
  operationSearch.status = null
  operationSearch.dateRange = null
  operationPage.value = 1
  loadOperation()
}

// ========== 登录日志 ==========
const loginSearch = reactive({ username: '', status: null, dateRange: null })
const loginList = ref([])
const loginPage = ref(1)
const loginSize = ref(10)
const loginTotal = ref(0)

const loadLogin = async () => {
  try {
    const params = { pageNum: loginPage.value, pageSize: loginSize.value, username: loginSearch.username, status: loginSearch.status }
    if (loginSearch.dateRange && loginSearch.dateRange.length === 2) {
      params.startTime = loginSearch.dateRange[0]
      params.endTime = loginSearch.dateRange[1]
    }
    const res = await getLoginLogs(params)
    loginList.value = res.list || []
    loginTotal.value = res.total || 0
  } catch (e) { ElMessage.error(e.message || '获取数据失败') }
}

const resetLogin = () => {
  loginSearch.username = ''
  loginSearch.status = null
  loginSearch.dateRange = null
  loginPage.value = 1
  loadLogin()
}

// ========== 设备状态变更日志 ==========
const deviceSearch = reactive({ deviceName: '', changeType: '', operator: '', dateRange: null })
const deviceList = ref([])
const devicePage = ref(1)
const deviceSize = ref(10)
const deviceTotal = ref(0)

const loadDevice = async () => {
  try {
    const params = { pageNum: devicePage.value, pageSize: deviceSize.value, deviceName: deviceSearch.deviceName, changeType: deviceSearch.changeType, operator: deviceSearch.operator }
    if (deviceSearch.dateRange && deviceSearch.dateRange.length === 2) {
      params.startDate = deviceSearch.dateRange[0]
      params.endDate = deviceSearch.dateRange[1]
    }
    const res = await getDeviceStatusLogs(params)
    deviceList.value = res.list || []
    deviceTotal.value = res.total || 0
  } catch (e) {
    ElMessage.error(axiosErrMsg(e, '获取设备状态变更日志失败'))
  }
}

const resetDevice = () => {
  deviceSearch.deviceName = ''
  deviceSearch.changeType = ''
  deviceSearch.operator = ''
  deviceSearch.dateRange = null
  devicePage.value = 1
  loadDevice()
}

// ========== 权限变更审计 ==========
const permSearch = reactive({ operator: '', changeType: '', dateRange: null })
const permList = ref([])
const permPage = ref(1)
const permSize = ref(10)
const permTotal = ref(0)

const loadPermission = async () => {
  try {
    const params = { pageNum: permPage.value, pageSize: permSize.value, operator: permSearch.operator, changeType: permSearch.changeType }
    if (permSearch.dateRange && permSearch.dateRange.length === 2) {
      params.startTime = permSearch.dateRange[0]
      params.endTime = permSearch.dateRange[1]
    }
    const res = await getPermissionAuditLogs(params)
    permList.value = res.list || []
    permTotal.value = res.total || 0
  } catch (e) { ElMessage.error(e.message || '获取数据失败') }
}

const resetPerm = () => {
  permSearch.operator = ''
  permSearch.changeType = ''
  permSearch.dateRange = null
  permPage.value = 1
  loadPermission()
}

const handleExport = async () => {
  try {
    const params = { logType: activeTab.value }
    if (activeTab.value === 'operation') {
      params.username = operationSearch.username || undefined
      params.module = operationSearch.module || undefined
      params.operationType = operationSearch.operationType || undefined
      params.status = operationSearch.status ?? undefined
      if (operationSearch.dateRange?.length === 2) {
        params.startTime = operationSearch.dateRange[0]
        params.endTime = operationSearch.dateRange[1]
      }
    } else if (activeTab.value === 'login') {
      params.username = loginSearch.username || undefined
      params.status = loginSearch.status ?? undefined
      if (loginSearch.dateRange?.length === 2) {
        params.startTime = loginSearch.dateRange[0]
        params.endTime = loginSearch.dateRange[1]
      }
    } else if (activeTab.value === 'device') {
      params.deviceName = deviceSearch.deviceName || undefined
      params.changeType = deviceSearch.changeType || undefined
      params.operator = deviceSearch.operator || undefined
      if (deviceSearch.dateRange?.length === 2) {
        params.startDate = deviceSearch.dateRange[0]
        params.endDate = deviceSearch.dateRange[1]
      }
    } else if (activeTab.value === 'permission') {
      params.operator = permSearch.operator || undefined
      params.changeType = permSearch.changeType || undefined
      if (permSearch.dateRange?.length === 2) {
        params.startTime = permSearch.dateRange[0]
        params.endTime = permSearch.dateRange[1]
      }
    }

    const blob = await exportLog(params)
    const file = blob instanceof Blob ? blob : new Blob([blob], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(file)
    const a = document.createElement('a')
    a.href = url
    a.download = `sys_log_${activeTab.value}_${dayjs().format('YYYY-MM-DD')}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(axiosErrMsg(e, '导出失败'))
  }
}

// 清理旧日志
const handleCleanup = async () => {
  try {
    cleanupLoading.value = true
    const res = await cleanupOldLogs(cleanupKeepCount.value)
    
    if (res.success) {
      ElMessage.success(res.message)
      showCleanupDialog.value = false
      
      // 重新加载当前标签页的数据
      if (activeTab.value === 'operation') loadOperation()
      else if (activeTab.value === 'login') loadLogin()
      else if (activeTab.value === 'device') loadDevice()
      else if (activeTab.value === 'permission') loadPermission()
    } else {
      ElMessage.error(res.message || '清理失败')
    }
  } catch (e) {
    ElMessage.error(axiosErrMsg(e, '清理失败'))
  } finally {
    cleanupLoading.value = false
  }
}

watch(activeTab, (name) => {
  if (name === 'operation') loadOperation()
  else if (name === 'login') loadLogin()
  else if (name === 'device') loadDevice()
  else if (name === 'permission') loadPermission()
})

onMounted(() => loadOperation())
</script>

<style scoped lang="scss">
.sys-log-container { color: #E6EDF3; }

.page-card {
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.page-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #161B22 0%, #1A1F35 100%);
  border-left: 4px solid #00D4FF;
  padding: 18px 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  h2 { margin: 0; font-size: 18px; font-weight: 700; color: #E6EDF3; }
  .header-sub { font-size: 12px; color: #8B949E; }
}

.tabs-card {
  padding: 0;
  overflow: visible;
  :deep(.el-tabs) {
    overflow: visible;
  }
}

.log-tabs {
  :deep(.el-tabs__header) {
    background: #21262D;
    border-radius: 10px 10px 0 0;
    margin: 0;
    padding: 0 16px;
  }

  :deep(.el-tabs__nav-wrap::after) { display: none; }

  :deep(.el-tabs__item) {
    color: #8B949E;
    font-weight: 600;
    height: 48px;
    line-height: 48px;
    &.is-active { color: #00D4FF; }
    &:hover { color: #E6EDF3; }
  }

  :deep(.el-tabs__active-bar) {
    background-color: #00D4FF;
    height: 3px;
    border-radius: 2px;
  }
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 6px;
  .el-icon { font-size: 14px; }
}

.tab-content {
  padding: 20px 24px;
}

.search-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.search-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 0 0 auto;
  width: 160px;
  min-width: 120px;
  max-width: 200px;
  label {
    font-size: 11px;
    font-weight: 700;
    color: #8B949E;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.search-field--range {
  width: 300px;
  max-width: min(100%, 360px);
}
.range-picker-el {
  width: 100%;
}
:deep(.search-field--range .el-date-editor) {
  width: 100% !important;
  box-sizing: border-box;
}

.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 2px;
  flex: 0 0 auto;
  margin-left: auto;
}

.mt16 { margin-top: 16px; }

:deep(.el-table) {
  border: none;
  background: #161B22;
  color: #E6EDF3;
  th.el-table__cell {
    background: #1A1F35 !important;
    color: #8B949E !important;
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    padding: 12px 8px;
    border-bottom: 1px solid #30363D !important;
  }
  td.el-table__cell {
    padding: 10px 8px;
    border-bottom: 1px solid #21262D !important;
    vertical-align: middle;
  }
  tr:hover > td { background: #1F2937 !important; }
}

.change-detail {
  font-size: 13px;
  color: #E6EDF3;
  font-family: monospace;
}
</style>
