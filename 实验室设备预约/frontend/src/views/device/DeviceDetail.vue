<template>
  <div class="device-detail" v-loading="loading">
    <template v-if="device">
      <div class="detail-header">
        <h2>{{ device.deviceName }}</h2>
        <el-tag :type="getStatusType(device.status)">{{ getStatusText(device.status) }}</el-tag>
      </div>

      <div v-if="device.imagePath" class="detail-cover">
        <img :src="coverSrc" alt="设备图片" />
      </div>

      <el-descriptions :column="2" border class="detail-card">
        <el-descriptions-item label="设备编号">{{ device.deviceNo }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ device.model }}</el-descriptions-item>
        <el-descriptions-item label="所在实验室">{{ device.laboratory }}</el-descriptions-item>
        <el-descriptions-item label="详细位置">{{ device.location }}</el-descriptions-item>
        <el-descriptions-item v-if="!isStudent" label="精度等级">{{ getPrecisionText(device.precisionLevel) }}</el-descriptions-item>
        <el-descriptions-item v-if="!isStudent" label="生产厂商">{{ device.manufacturer }}</el-descriptions-item>
        <el-descriptions-item v-if="!isStudent" label="校准周期">{{ device.calibrationCycle }}天</el-descriptions-item>
        <el-descriptions-item v-if="!isStudent" label="下次校准">{{ device.nextCalibrationDate ? formatDate(device.nextCalibrationDate) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="设备简介" :span="2">{{ device.description || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="!isStudent" label="适配实验项目" :span="2">{{ device.adaptProject || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-actions">
        <el-button v-if="device.status === 0" type="primary" @click="goBooking">预约此设备</el-button>
        <el-button type="warning" @click="goRepair">故障报修</el-button>
        <el-button v-if="canApplyScrap && device.status !== 4" type="danger" plain class="scrap-btn" @click="goScrap">申请报废</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>

      <!-- ========== 相关知识 ========== -->
      <div class="knowledge-section">
        <div class="section-header">
          <span class="section-title">
            <el-icon><Collection /></el-icon>
            相关知识
          </span>
          <el-tag size="small" type="info">共 {{ knowledgeList.length }} 条</el-tag>
        </div>

        <div v-loading="knowledgeLoading" class="knowledge-body">
          <el-empty v-if="!knowledgeLoading && knowledgeList.length === 0" description="暂无关联知识" />

          <div v-else class="knowledge-grid">
            <div
              v-for="item in knowledgeList"
              :key="item.id"
              class="knowledge-card"
              :class="{ 'is-danger': item.isDanger === 1 }"
            >
              <div class="knowledge-card-header">
                <span class="knowledge-question">{{ item.question }}</span>
                <el-tag v-if="item.isDanger === 1" type="danger" size="small" effect="plain">
                  <el-icon style="margin-right:2px"><WarningFilled /></el-icon>
                  危险操作
                </el-tag>
              </div>
              <div class="knowledge-answer">{{ truncateAnswer(item.answer) }}</div>
              <div class="knowledge-footer">
                <el-tag size="small" type="info">{{ item.category || '设备操作手册' }}</el-tag>
                <el-button
                  v-if="hasGuideSteps(item)"
                  size="small"
                  type="primary"
                  plain
                  class="guide-btn"
                  @click="openGuide(item)"
                >
                  <el-icon style="margin-right:4px"><Guide /></el-icon>
                  查看引导
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- ========== 状态变更记录 ========== -->
      <div v-if="!isStudent" class="log-section">
        <div class="section-header">
          <span class="section-title">
            <el-icon><Clock /></el-icon>
            状态变更记录
          </span>
          <el-tag size="small" type="info">共 {{ statusLogs.length }} 条</el-tag>
        </div>

        <div v-loading="logLoading" class="log-body">
          <el-empty v-if="!logLoading && statusLogs.length === 0" description="暂无状态变更记录" />

          <el-timeline v-else :hover="true">
            <el-timeline-item
              v-for="(log, index) in statusLogs"
              :key="log.id"
              :type="getLogItemType(log.newStatus)"
              :hollow="index === 0"
              placement="top"
            >
              <div class="log-item">
                <div class="log-item-top">
                  <span class="log-time">{{ formatLogTime(log.createTime) }}</span>
                  <span class="log-operator" v-if="log.operatorName">
                    操作人：{{ log.operatorName }}
                  </span>
                </div>
                <div class="log-item-content">
                  <span class="log-status-badge old">
                    {{ getStatusText(log.oldStatus) }}
                  </span>
                  <el-icon class="log-arrow"><Right /></el-icon>
                  <span class="log-status-badge new" :class="`status-type-${log.newStatus}`">
                    {{ getStatusText(log.newStatus) }}
                  </span>
                </div>
                <div class="log-item-reason" v-if="log.changeReason">
                  <span class="reason-label">变更原因：</span>{{ log.changeReason }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
    </template>
    <el-empty v-else-if="!loading" description="设备不存在" />

    <!-- 图文引导弹窗 -->
    <OperationGuide
      v-model="guideVisible"
      :guide-steps="guideSteps"
      :guide-duration="guideDuration"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDeviceDetail, getDeviceStatusLogsByDeviceId, submitScrapApplication } from '@/api/device'
import { getKnowledgeByDeviceId } from '@/api/ai'
import OperationGuide from '@/components/OperationGuide.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Right, Collection, Guide, WarningFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const deviceId = computed(() => route.params.id)
const userType = computed(() => userStore.userInfo?.userType || userStore.userType || '')
const isStudent = computed(() => userType.value === 'STUDENT')
const canApplyScrap = computed(() => ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER'].includes(userType.value))
const canOpenScrapPage = computed(() => ['SYSTEM_ADMIN', 'LAB_ADMIN'].includes(userType.value))
const device = ref(null)
const loading = ref(true)
const statusLogs = ref([])
const logLoading = ref(true)
const knowledgeList = ref([])
const knowledgeLoading = ref(true)

// 图文引导
const guideVisible = ref(false)
const guideSteps = ref([])
const guideDuration = ref(0)

const coverSrc = computed(() => {
  const p = device.value?.imagePath
  if (!p) return ''
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  const s = p.startsWith('/') ? p : `/${p}`
  return `/api${s}`
})

const formatDate = (v) => v ? dayjs(v).format('YYYY-MM-DD') : '-'
const formatLogTime = (v) => v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'

const STATUS_MAP = {
  0: { text: '空闲', type: 'success' },
  1: { text: '使用中', type: 'primary' },
  2: { text: '维修中', type: 'warning' },
  3: { text: '校准中', type: 'info' },
  4: { text: '报废', type: 'danger' },
}

const getStatusType = (s) => (STATUS_MAP[s] || { type: 'info' }).type
const getStatusText = (s) => (STATUS_MAP[s] || { text: '未知' }).text
const getPrecisionText = (p) => ({ 1: '低', 2: '中', 3: '高' }[p] || '中')
const getLogItemType = (newStatus) => {
  const map = { 0: 'success', 1: 'primary', 2: 'warning', 3: 'info', 4: 'danger' }
  return map[newStatus] || 'info'
}

const truncateAnswer = (text, maxLen = 120) => {
  if (!text) return ''
  const clean = text.replace(/\n/g, ' ')
  return clean.length > maxLen ? clean.slice(0, maxLen) + '…' : clean
}

const hasGuideSteps = (item) => {
  if (!item) return false
  if (item.guideSteps) return true
  if (item.answer) {
    return /^\s*\d+[.．、）)]/.test(item.answer)
  }
  return false
}

const parseGuideStepsFromAnswer = (rawAnswer) => {
  if (!rawAnswer) return []
  const stepRe = /^\s*(\d+)[\.．、）)]\s*(.+)$/m
  const steps = []
  for (const line of rawAnswer.split('\n')) {
    const m = line.trim().match(stepRe)
    if (m) {
      steps.push({
        step: parseInt(m[1]),
        title: `第${m[1]}步`,
        content: m[2].trim()
      })
    }
  }
  return steps
}

const openGuide = (item) => {
  guideSteps.value = item.guideSteps
    ? (typeof item.guideSteps === 'string' ? JSON.parse(item.guideSteps) : item.guideSteps)
    : parseGuideStepsFromAnswer(item.answer)
  guideDuration.value = item.guideDuration || Math.max(1, Math.round(guideSteps.value.length / 2))
  guideVisible.value = true
}

const loadData = async () => {
  if (!deviceId.value) return
  loading.value = true
  logLoading.value = true
  knowledgeLoading.value = true
  try {
    const dev = await getDeviceDetail(deviceId.value)
    device.value = dev

    if (!isStudent.value) {
      const [logs, knowledge] = await Promise.all([
        getDeviceStatusLogsByDeviceId(deviceId.value).catch(() => []),
        getKnowledgeByDeviceId(deviceId.value).catch(() => []),
      ])
      statusLogs.value = logs || []
      knowledgeList.value = knowledge || []
    } else {
      const knowledge = await getKnowledgeByDeviceId(deviceId.value).catch(() => [])
      statusLogs.value = []
      knowledgeList.value = knowledge || []
    }
  } catch (e) {
    ElMessage.error(e.message || '获取失败')
  } finally {
    loading.value = false
    logLoading.value = false
    knowledgeLoading.value = false
  }
}

const goBooking = () => {
  router.push({ path: '/device', query: { book: deviceId.value } })
}

const goRepair = () => {
  router.push({ path: '/device', query: { repair: deviceId.value } })
}

const goScrap = async () => {
  if (canOpenScrapPage.value) {
    router.push({ path: '/device-scrap', query: { deviceId: deviceId.value } })
    return
  }
  // 教师等无报废管理页权限角色：在详情页直接提交申请
  if (!device.value?.id) return
  try {
    const deviceName = device.value?.deviceName || '该设备'
    const { value } = await ElMessageBox.prompt('请输入报废原因', `提交报废申请 - ${deviceName}`, {
      inputType: 'textarea',
      inputPlaceholder: '请详细描述报废原因',
      inputValidator: (v) => (v && v.trim().length > 0 ? true : '报废原因不能为空')
    })
    const reason = (value || '').trim()
    if (!reason) return
    await submitScrapApplication({ deviceId: device.value.id, scrapReason: reason })
    ElMessage.success('报废申请已提交')
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    const msg = e?.response?.data?.message || e.message || '提交失败'
    ElMessage.error(msg)
  }
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.device-detail {
  color: #E6EDF3;
  background: #0D1117;
  min-height: 100%;
  padding: 20px;
  border-radius: 12px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #30363D;
}

.detail-header h2 {
  margin: 0;
  color: #E6EDF3;
  font-size: 24px;
}

.detail-cover {
  margin-bottom: 20px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #30363d;
  max-height: 320px;
  background: #161b22;
}

.detail-cover img {
  width: 100%;
  max-height: 320px;
  object-fit: contain;
  object-position: center;
  image-rendering: auto;
  display: block;
}

.detail-card {
  background: #161B22 !important;
  border: 1px solid #30363D;
  border-radius: 8px;
}

:deep(.el-descriptions__body) {
  background: #161B22;
}

:deep(.el-descriptions__label) {
  background: #21262D !important;
  color: #8B949E !important;
  border-bottom: 1px solid #30363D;
}

:deep(.el-descriptions__content) {
  background: #161B22 !important;
  color: #E6EDF3 !important;
  border-bottom: 1px solid #30363D;
}

:deep(.el-descriptions__cell) {
  border-color: #30363D !important;
}

.detail-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #30363D;
}

.scrap-btn {
  background: rgba(245, 108, 108, 0.1) !important;
  border-color: #F56C6C !important;
  color: #F56C6C !important;

  &:hover {
    background: rgba(245, 108, 108, 0.2) !important;
    border-color: #f89898 !important;
    color: #f89898 !important;
  }
}

:deep(.el-tag) {
  font-size: 14px;
  padding: 4px 12px;
}

/* ========== 通用区块 ========== */
.log-section,
.knowledge-section {
  margin-top: 32px;
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 12px;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #1C2128;
  border-bottom: 1px solid #30363D;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #E6EDF3;

  .el-icon {
    color: #00D4FF;
  }
}

/* ========== 知识区块 ========== */
.knowledge-body {
  padding: 20px 24px;
  min-height: 100px;
}

.knowledge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.knowledge-card {
  background: #21262D;
  border: 1px solid #30363D;
  border-radius: 10px;
  padding: 16px;
  transition: border-color 0.2s, transform 0.2s;
  display: flex;
  flex-direction: column;
  gap: 10px;

  &:hover {
    border-color: #484F58;
    transform: translateY(-2px);
  }

  &.is-danger {
    border-color: rgba(245, 108, 108, 0.4);
    background: rgba(245, 108, 108, 0.04);

    &:hover {
      border-color: rgba(245, 108, 108, 0.6);
    }
  }
}

.knowledge-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.knowledge-question {
  font-size: 14px;
  font-weight: 600;
  color: #E6EDF3;
  line-height: 1.4;
}

.knowledge-answer {
  font-size: 13px;
  color: #8B949E;
  line-height: 1.6;
  flex: 1;
}

.knowledge-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.guide-btn {
  font-size: 12px;
  padding: 4px 10px;
}

/* ========== 状态记录 ========== */
.log-body {
  padding: 20px 24px;
  min-height: 100px;
}

/* 时间线定制 */
:deep(.el-timeline) {
  margin: 0;
  padding: 0;
}

:deep(.el-timeline-item__wrapper) {
  padding-top: 2px;
}

:deep(.el-timeline-item__node) {
  background: #30363D;
  border: 2px solid transparent;
}

:deep(.el-timeline-item--hollow .el-timeline-item__node) {
  background: transparent;
}

:deep(.el-timeline-item__tail) {
  border-left: 2px solid #30363D;
}

.log-item {
  background: #21262D;
  border: 1px solid #30363D;
  border-radius: 8px;
  padding: 12px 16px;
  transition: border-color 0.2s;

  &:hover {
    border-color: #484F58;
  }
}

.log-item-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  gap: 12px;
  flex-wrap: wrap;
}

.log-time {
  font-size: 12px;
  color: #8B949E;
  font-family: 'JetBrains Mono', monospace;
}

.log-operator {
  font-size: 12px;
  color: #6E7681;
}

.log-item-content {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.log-status-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;

  &.old {
    background: rgba(139, 148, 158, 0.15);
    color: #8B949E;
    border: 1px solid rgba(139, 148, 158, 0.3);
  }

  &.new {
    &.status-type-0 {
      background: rgba(0, 201, 140, 0.12);
      color: #00C98C;
      border: 1px solid rgba(0, 201, 140, 0.3);
    }
    &.status-type-1 {
      background: rgba(64, 158, 255, 0.12);
      color: #409EFF;
      border: 1px solid rgba(64, 158, 255, 0.3);
    }
    &.status-type-2 {
      background: rgba(230, 162, 60, 0.12);
      color: #E6A23C;
      border: 1px solid rgba(230, 162, 60, 0.3);
    }
    &.status-type-3 {
      background: rgba(144, 147, 153, 0.12);
      color: #909399;
      border: 1px solid rgba(144, 147, 153, 0.3);
    }
    &.status-type-4 {
      background: rgba(245, 108, 108, 0.12);
      color: #F56C6C;
      border: 1px solid rgba(245, 108, 108, 0.3);
    }
  }
}

.log-arrow {
  color: #484F58;
  font-size: 14px;
  flex-shrink: 0;
}

.log-item-reason {
  font-size: 12px;
  color: #6E7681;
  line-height: 1.4;

  .reason-label {
    color: #8B949E;
  }
}

/* 空状态 */
:deep(.el-empty__description) {
  color: #484F58;
}
</style>
