<template>
  <div class="ai-log-audit">
    <div class="page-header">
      <h2>AI日志审计</h2>
      <div class="header-actions">
        <el-button type="primary" :icon="Download" @click="handleExport">导出日志</el-button>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon normal"><el-icon><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.normal || 0 }}</div>
          <div class="stat-label">正常</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon abnormal"><el-icon><WarningFilled /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.abnormal || 0 }}</div>
          <div class="stat-label">异常</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon low-quality"><el-icon><InfoFilled /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.lowQuality || 0 }}</div>
          <div class="stat-label">低质量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon safety"><el-icon><Warning /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.safetyAlert || 0 }}</div>
          <div class="stat-label">安全预警</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-bar">
      <el-input
        v-model="filterForm.username"
        placeholder="用户账号"
        clearable
        style="width: 150px"
        @keyup.enter="loadData"
      />
      <el-select v-model="filterForm.userType" placeholder="用户角色" clearable style="width: 130px" @change="loadData">
        <el-option label="系统管理员" value="SYSTEM_ADMIN" />
        <el-option label="实验室管理员" value="LAB_ADMIN" />
        <el-option label="教师" value="TEACHER" />
        <el-option label="学生" value="STUDENT" />
        <el-option label="维护人员" value="MAINTAINER" />
      </el-select>
      <el-select v-model="filterForm.sessionType" placeholder="会话类型" clearable style="width: 150px" @change="loadData">
        <el-option label="设备操作手册" value="DEVICE_OPERATION" />
        <el-option label="实验流程" value="EXPERIMENT_PROCESS" />
        <el-option label="故障排查" value="TROUBLESHOOTING" />
        <el-option label="安全规范" value="SAFETY" />
        <el-option label="常见问题" value="COMMON_QUESTION" />
      </el-select>
      <el-select v-model="filterForm.quality" placeholder="质量状态" clearable style="width: 130px" @change="loadData">
        <el-option label="正常" value="NORMAL" />
        <el-option label="异常" value="ABNORMAL" />
        <el-option label="低质量" value="LOW_QUALITY" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 200px"
        @change="handleDateChange"
      />
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <!-- 日志列表 -->
    <el-table :data="list" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户账号" width="120" />
      <el-table-column prop="userType" label="用户角色" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="getUserTypeTag(row.userType)">{{ formatUserType(row.userType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sessionType" label="会话类型" width="120">
        <template #default="{ row }">
          {{ formatSessionType(row.sessionType) }}
        </template>
      </el-table-column>
      <el-table-column prop="question" label="问题摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="quality" label="质量" width="90" align="center">
        <template #default="{ row }">
          <el-tag size="small" :type="getQualityTag(row.quality)">{{ formatQuality(row.quality) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responseTime" label="响应时长" width="90" align="center">
        <template #default="{ row }">
          {{ row.responseTime ? row.responseTime + 'ms' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="confidence" label="置信度" width="90" align="center">
        <template #default="{ row }">
          {{ row.confidence ? (row.confidence * 100).toFixed(0) + '%' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="安全预警" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.safetyAlert" type="danger" size="small">安全提示</el-tag>
          <span v-else style="color: #6E7681">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提问时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right" align="center">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <div class="detail-content" v-if="currentLog">
        <div class="detail-row">
          <span class="detail-label">ID：</span>
          <span class="detail-value">{{ currentLog.id }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">用户账号：</span>
          <span class="detail-value">{{ currentLog.username || '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">用户角色：</span>
          <span class="detail-value">{{ formatUserType(currentLog.userType) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">会话类型：</span>
          <span class="detail-value">{{ formatSessionType(currentLog.sessionType) }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">质量状态：</span>
          <el-tag size="small" :type="getQualityTag(currentLog.quality)">{{ formatQuality(currentLog.quality) }}</el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">响应时长：</span>
          <span class="detail-value">{{ currentLog.responseTime ? currentLog.responseTime + 'ms' : '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">置信度：</span>
          <span class="detail-value">{{ currentLog.confidence ? (currentLog.confidence * 100).toFixed(2) + '%' : '-' }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">安全预警：</span>
          <el-tag v-if="currentLog.safetyAlert" type="danger" size="small">安全提示</el-tag>
          <span v-else class="detail-value">无</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">提问时间：</span>
          <span class="detail-value">{{ formatTime(currentLog.createTime) }}</span>
        </div>
        <div class="detail-section">
          <div class="section-title">问题</div>
          <div class="section-content">{{ currentLog.question }}</div>
        </div>
        <div class="detail-section">
          <div class="section-title">AI回复</div>
          <div class="section-content answer-content">{{ currentLog.answer }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAiChatLogs, getAiChatLogDetail, exportAiChatLogs } from '@/api/ai'
import { Download, Refresh, CircleCheck, WarningFilled, InfoFilled, Warning } from '@element-plus/icons-vue'

const loading = ref(false)
const list = ref([])
const stats = ref({})
const detailVisible = ref(false)
const currentLog = ref(null)
const dateRange = ref([])

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filterForm = reactive({
  username: '',
  userType: '',
  sessionType: '',
  quality: '',
  startTime: '',
  endTime: ''
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAiChatLogs({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...filterForm
    })
    list.value = res.list || []
    pagination.total = res.total || 0
    stats.value = res.stats || {}
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const showDetail = async (row) => {
  try {
    const res = await getAiChatLogDetail(row.id)
    currentLog.value = res
    detailVisible.value = true
  } catch (e) {
    ElMessage.error('获取详情失败')
  }
}

const handleExport = async () => {
  try {
    await ElMessageBox.confirm('确定要导出AI聊天日志吗？', '导出确认', { type: 'info' })
    const blob = await exportAiChatLogs(filterForm)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = `ai_chat_logs_${new Date().toLocaleDateString()}.csv`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('导出失败')
    }
  }
}

const handleDateChange = (val) => {
  if (val && val.length === 2) {
    filterForm.startTime = val[0] + ' 00:00:00'
    filterForm.endTime = val[1] + ' 23:59:59'
  } else {
    filterForm.startTime = ''
    filterForm.endTime = ''
  }
  loadData()
}

const resetFilter = () => {
  filterForm.username = ''
  filterForm.userType = ''
  filterForm.sessionType = ''
  filterForm.quality = ''
  filterForm.startTime = ''
  filterForm.endTime = ''
  dateRange.value = []
  pagination.pageNum = 1
  loadData()
}

const formatTime = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString()
}

const formatUserType = (type) => {
  const map = {
    SYSTEM_ADMIN: '系统管理员',
    LAB_ADMIN: '实验室管理员',
    TEACHER: '教师',
    STUDENT: '学生',
    MAINTAINER: '维护人员'
  }
  return map[type] || type || '-'
}

const formatSessionType = (type) => {
  const map = {
    DEVICE_OPERATION: '设备操作手册',
    EXPERIMENT_PROCESS: '实验流程',
    TROUBLESHOOTING: '故障排查',
    SAFETY: '安全规范',
    COMMON_QUESTION: '常见问题'
  }
  return map[type] || type || '-'
}

const formatQuality = (q) => {
  const map = { NORMAL: '正常', ABNORMAL: '异常', LOW_QUALITY: '低质量' }
  return map[q] || q || '-'
}

const getUserTypeTag = (type) => {
  const map = {
    SYSTEM_ADMIN: 'danger',
    LAB_ADMIN: 'warning',
    TEACHER: 'success',
    STUDENT: 'primary',
    MAINTAINER: 'info'
  }
  return map[type] || 'info'
}

const getQualityTag = (q) => {
  const map = { NORMAL: 'success', ABNORMAL: 'danger', LOW_QUALITY: 'warning' }
  return map[q] || 'info'
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.ai-log-audit {
  color: #E6EDF3;
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  h2 { margin: 0; color: #E6EDF3; font-size: 24px; }
}
.header-actions { display: flex; gap: 12px; }

.stats-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;

  .stat-card {
    flex: 1;
    background: #161B22;
    border: 1px solid #30363D;
    border-radius: 12px;
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 16px;

    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;

      &.normal { background: rgba(103, 194, 58, 0.15); color: #67C23A; }
      &.abnormal { background: rgba(245, 108, 108, 0.15); color: #F56C6C; }
      &.low-quality { background: rgba(230, 162, 60, 0.15); color: #E6A23C; }
      &.safety { background: rgba(64, 158, 255, 0.15); color: #409EFF; }
    }

    .stat-info {
      .stat-value { font-size: 28px; font-weight: 700; color: #E6EDF3; }
      .stat-label { font-size: 14px; color: #8B949E; margin-top: 4px; }
    }
  }
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

:deep(.el-date-editor.el-date-editor--daterange) {
  width: 200px !important;
  max-width: 200px !important;
  min-width: 200px !important;
  flex: none !important;
}



.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.detail-content {
  .detail-row {
    display: flex;
    align-items: center;
    margin-bottom: 12px;
    .detail-label { color: #8B949E; width: 100px; }
    .detail-value { color: #E6EDF3; }
  }
  .detail-section {
    margin-top: 16px;
    .section-title { color: #8B949E; margin-bottom: 8px; font-weight: 600; }
    .section-content {
      background: #21262D;
      border-radius: 8px;
      padding: 12px;
      color: #E6EDF3;
      line-height: 1.8;
      white-space: pre-wrap;
      max-height: 200px;
      overflow-y: auto;
      &.answer-content { border-left: 3px solid #409EFF; }
    }
  }
}

// 深色主题覆盖
:deep(.el-table) {
  background: #161B22;
  color: #E6EDF3;
  --el-table-border-color: #30363D;
}
:deep(.el-table__header) { background: #21262D; }
:deep(.el-table__row:hover > td) { background: #1F2937 !important; }
:deep(.el-pagination) { justify-content: flex-end; }
</style>
