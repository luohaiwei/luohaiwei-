<template>
  <div class="repair-global-monitor">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>维修工单全局监控</h2>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="handleExport">
          <el-icon><Download /></el-icon>导出当前页(CSV)
        </el-button>
      </div>
    </div>

    <!-- 全局统计 -->
    <div class="page-card stats-card">
      <div class="stats-grid">
        <div class="stat-item">
          <span class="stat-num">{{ globalStats.total }}</span>
          <span class="stat-label">工单总数</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #FF9500">{{ globalStats.pending }}</span>
          <span class="stat-label">待处理</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #00D4FF">{{ globalStats.processing }}</span>
          <span class="stat-label">处理中</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #00FF88">{{ globalStats.completed }}</span>
          <span class="stat-label">已完成</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #FF4757">{{ globalStats.overdue }}</span>
          <span class="stat-label">超时未处理</span>
        </div>
      </div>
    </div>

    <div class="page-card search-card">
      <div class="search-row">
        <div class="search-field">
          <label>设备名称</label>
          <el-input v-model="searchForm.deviceName" placeholder="设备名称" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>工单编号</label>
          <el-input v-model="searchForm.orderNo" placeholder="工单编号" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>工单状态</label>
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 100%">
            <el-option label="待接单" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </div>
        <div class="search-field">
          <label>优先级</label>
          <el-select v-model="searchForm.priority" placeholder="全部" clearable style="width: 100%">
            <el-option label="紧急" value="URGENT" />
            <el-option label="高" value="HIGH" />
            <el-option label="普通" value="NORMAL" />
            <el-option label="低" value="LOW" />
          </el-select>
        </div>
        <div class="search-field">
          <label>指派给</label>
          <el-input v-model="searchForm.assignee" placeholder="维护人员" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="orderNo" label="工单编号" min-width="150" />
        <el-table-column label="设备信息" min-width="200">
          <template #default="{ row }">
            <div class="device-info">
              <span class="device-name">{{ row.deviceName }}</span>
              <span class="device-no">{{ row.deviceNo }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="getPriorityTag(row.priority)" :hit="false">
              {{ getPriorityText(row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="faultDescription" label="故障描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="报修人" width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reporterName || '-' }}</template>
        </el-table-column>
        <el-table-column label="指派给" width="100" show-overflow-tooltip>
          <template #default="{ row }">{{ row.handlerName || '-' }}</template>
        </el-table-column>
        <el-table-column
          label="状态"
          width="108"
          align="center"
          class-name="col-status-monitor"
          :show-overflow-tooltip="false"
        >
          <template #default="{ row }">
            <span class="status-tag-cell">
              <el-tag size="small" :type="getStatusTag(row.status)">{{ getStatusText(row.status) }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column label="剩余时间" width="110">
          <template #default="{ row }">
            <span :class="getTimeClass(row)">{{ getTimeLeft(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="handleView(row)">详情</el-button>
            <el-button v-if="row.status === 0" size="small" type="warning" @click="openAssignDialog(row)">
              重新指派
            </el-button>
            <el-button v-if="row.status !== 2" size="small" type="info" plain @click="handleAdjustPriority(row)">
              调整优先级
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="工单详情" width="600px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="工单编号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detailData.deviceNo }}</el-descriptions-item>
        <el-descriptions-item label="故障描述">{{ detailData.faultDescription }}</el-descriptions-item>
        <el-descriptions-item label="报修人">{{ detailData.reporter || detailData.reporterName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="指派给">{{ detailData.assignee || detailData.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag size="small" :type="getPriorityTag(detailData.priority)">{{ getPriorityText(detailData.priority) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="getStatusTag(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="指派时间">{{ detailData.assignTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detailData.completeTime || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.repairSolution" label="维修方案">{{ detailData.repairSolution }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 重新指派弹窗 -->
    <el-dialog v-model="assignVisible" title="重新指派工单" width="480px">
      <el-form label-width="90px">
        <el-form-item label="工单编号">
          <el-input :model-value="currentOrder?.orderNo" disabled />
        </el-form-item>
        <el-form-item label="设备名称">
          <el-input :model-value="currentOrder?.deviceName" disabled />
        </el-form-item>
        <el-form-item label="指派给" required>
          <el-select v-model="assignForm.assigneeId" placeholder="选择维护人员" filterable style="width: 100%">
            <el-option
              v-for="m in maintainerList"
              :key="m.id"
              :label="m.realName + ' (' + m.username + ')'"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assignLoading" @click="submitAssign">确认指派</el-button>
      </template>
    </el-dialog>

    <!-- 调整优先级弹窗 -->
    <el-dialog v-model="priorityVisible" title="调整优先级" width="420px">
      <el-form label-width="90px">
        <el-form-item label="工单编号">
          <el-input :model-value="currentOrder?.orderNo" disabled />
        </el-form-item>
        <el-form-item label="新优先级" required>
          <el-select v-model="priorityForm.priority" placeholder="选择优先级" style="width: 100%">
            <el-option label="紧急（红色）" value="URGENT" />
            <el-option label="高（橙色）" value="HIGH" />
            <el-option label="普通（蓝色）" value="NORMAL" />
            <el-option label="低（灰色）" value="LOW" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priorityVisible = false">取消</el-button>
        <el-button type="primary" :loading="priorityLoading" @click="submitPriority">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getGlobalRepairOrders, getRepairOrderDetail, reassignRepair, adjustRepairPriority } from '@/api/repair'
import { getUsersByType } from '@/api/user'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const searchForm = reactive({ deviceName: '', orderNo: '', status: null, priority: '', assignee: '' })
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const globalStats = reactive({ total: 0, pending: 0, processing: 0, completed: 0, overdue: 0 })
const detailVisible = ref(false)
const detailData = ref(null)
const assignVisible = ref(false)
const assignLoading = ref(false)
const currentOrder = ref(null)
const maintainerList = ref([])
const assignForm = reactive({ assigneeId: null })
const priorityVisible = ref(false)
const priorityLoading = ref(false)
const priorityForm = reactive({ priority: 'NORMAL' })

const getPriorityTag = (p) => ({ URGENT: 'danger', HIGH: 'warning', NORMAL: 'primary', LOW: 'info' }[p] || 'info')
const getPriorityText = (p) => ({ URGENT: '紧急', HIGH: '高', NORMAL: '普通', LOW: '低' }[p] || p)
const getStatusTag = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 3: 'info' })[s] || 'info'
const getStatusText = (s) => ({ 0: '待接单', 1: '处理中', 2: '已完成', 3: '已取消' })[s] || '未知'
/** 与后端 global 接口 SLA 一致：待接单 create+48h，处理中 handleStart+72h */
const inferDeadline = (row) => {
  if (row.deadline) return dayjs(row.deadline)
  if (row.status !== 0 && row.status !== 1) return null
  const base =
    row.status === 0
      ? row.createTime || row.reportTime
      : row.handleStartTime || row.createTime || row.reportTime
  if (!base) return null
  const hours = row.status === 0 ? 48 : 72
  return dayjs(base).add(hours, 'hour')
}

const getTimeLeft = (row) => {
  const end = inferDeadline(row)
  if (!end) return '-'
  const left = end.diff(dayjs(), 'hour', true)
  if (left < 0) return '已超时'
  if (left < 24) return Math.ceil(left) + 'h'
  return Math.floor(left / 24) + 'd'
}
const getTimeClass = (row) => {
  const end = inferDeadline(row)
  if (!end) return 'time-normal'
  const left = end.diff(dayjs(), 'hour', true)
  if (left < 0) return 'time-overdue'
  if (left < 24) return 'time-warning'
  return 'time-normal'
}

const loadData = async () => {
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (searchForm.deviceName) params.deviceName = searchForm.deviceName
    if (searchForm.orderNo) params.orderNo = searchForm.orderNo
    if (searchForm.status != null) params.status = searchForm.status
    if (searchForm.priority) params.priority = searchForm.priority
    if (searchForm.assignee) params.assignee = searchForm.assignee
    const res = await getGlobalRepairOrders(params)
    tableData.value = res.list || []
    total.value = res.total || 0
    if (res.stats) Object.assign(globalStats, res.stats)
  } catch (e) {
    const msg = e?.response?.data?.message || e?.message || '获取数据失败'
    ElMessage.error(msg)
  }
}

const resetSearch = () => {
  searchForm.deviceName = ''
  searchForm.orderNo = ''
  searchForm.status = null
  searchForm.priority = ''
  searchForm.assignee = ''
  pageNum.value = 1
  loadData()
}

const handleView = async (row) => {
  try {
    detailData.value = await getRepairOrderDetail(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取详情失败')
  }
}

const openAssignDialog = async (row) => {
  currentOrder.value = row
  assignForm.assigneeId = null
  try {
    maintainerList.value = (await getUsersByType('MAINTAINER')) || []
  } catch (e) {
    ElMessage.error(e.message || '获取维护人员列表失败')
    return
  }
  if (!maintainerList.value.length) {
    ElMessage.warning('系统中暂无维护人员账号，请先在用户管理中创建 MAINTAINER 角色用户')
  }
  assignVisible.value = true
}

const submitAssign = async () => {
  if (!assignForm.assigneeId) {
    ElMessage.warning('请选择维护人员')
    return
  }
  assignLoading.value = true
  try {
    await reassignRepair(currentOrder.value.id, assignForm.assigneeId)
    ElMessage.success('指派成功')
    assignVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '指派失败')
  } finally {
    assignLoading.value = false
  }
}

const handleAdjustPriority = (row) => {
  currentOrder.value = row
  priorityForm.priority = row.priority || 'NORMAL'
  priorityVisible.value = true
}

const submitPriority = async () => {
  priorityLoading.value = true
  try {
    await adjustRepairPriority(currentOrder.value.id, priorityForm.priority)
    ElMessage.success('优先级已调整')
    priorityVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '调整失败')
  } finally {
    priorityLoading.value = false
  }
}

const handleExport = () => {
  const rows = tableData.value || []
  if (!rows.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  const headers = ['工单编号', '设备名称', '设备编号', '状态', '优先级', '报修人', '处理人', '故障摘要']
  const lines = [headers.join(',')]
  for (const r of rows) {
    const statusText = getStatusText(r.status)
    const pri = getPriorityText(r.priority)
    const fault = (r.faultDescription || '').replace(/\r?\n/g, ' ').replace(/,/g, '，')
    lines.push(
      [r.orderNo, r.deviceName, r.deviceNo, statusText, pri, r.reporterName || '', r.handlerName || '', fault]
        .map((c) => `"${String(c ?? '').replace(/"/g, '""')}"`)
        .join(',')
    )
  }
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `维修工单监控_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('已导出当前页数据')
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.repair-global-monitor { color: #E6EDF3; min-height: 100%; }

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
  border-left: 4px solid #FF9500;
  padding: 18px 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  h2 { margin: 0; font-size: 18px; font-weight: 700; color: #E6EDF3; }
  .header-sub { font-size: 12px; color: #8B949E; }
}

.header-actions { display: flex; gap: 8px; }

.stats-card { padding: 16px 24px; }

.stats-grid {
  display: flex;
  gap: 40px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #E6EDF3;
}

.stat-label {
  font-size: 13px;
  color: #8B949E;
}

.search-card { padding-bottom: 16px; }

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
  flex: 1;
  min-width: 160px;
  max-width: 240px;

  :deep(.el-input__wrapper),
  :deep(.el-select__wrapper) {
    background: #21262D;
    border: 1px solid #30363D;
    box-shadow: none;
    border-radius: 6px;
    min-height: 32px;

    &:hover {
      border-color: #00D4FF;
    }
    &.is-focus {
      border-color: #00D4FF;
      box-shadow: 0 0 0 2px rgba(0, 212, 255, 0.12);
    }
  }

  :deep(.el-input__inner),
  :deep(.el-select__placeholder) {
    color: #E6EDF3;
    font-size: 13px;
  }

  label {
    font-size: 11px;
    font-weight: 700;
    color: #8B949E;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 2px;
}

.table-card {
  padding: 0;
  overflow: hidden;
  :deep(.el-table) {
    border: none;
    background: #161B22;
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
      color: #E6EDF3;
    }
    tr:hover > td { background: #1F2937 !important; }
  }
}

.device-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  .device-name { font-weight: 600; color: #E6EDF3; }
  .device-no { font-size: 12px; color: #8B949E; }
}

.time-normal { color: #00FF88; font-size: 13px; }
.time-warning { color: #FF9500; font-size: 13px; }
.time-overdue { color: #FF4757; font-size: 13px; font-weight: 600; }

/* 避免状态列 el-tag 被表格 .cell 的 text-overflow: ellipsis 裁成「…」 */
.status-tag-cell {
  display: inline-flex;
  align-items: center;
  vertical-align: middle;
}
.table-card :deep(.col-status-monitor .cell) {
  overflow: visible !important;
  text-overflow: clip;
}

.pagination-bar {
  padding: 14px 16px 10px;
  border-top: 1px solid #30363D;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-descriptions__body) { background: #161B22; }
:deep(.el-descriptions__cell) { border-color: #30363D !important; }
:deep(.el-descriptions__label) { background: #21262D !important; color: #8B949E !important; }
:deep(.el-descriptions__content) { background: #161B22 !important; color: #E6EDF3 !important; }
</style>
