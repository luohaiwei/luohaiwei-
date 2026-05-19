<template>
  <div class="device-track-tab">
    <div class="page-card search-card">
      <div class="search-row">
        <div class="search-field">
          <label>设备名称</label>
          <el-input v-model="searchForm.deviceName" placeholder="设备名称" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>设备编号</label>
          <el-input v-model="searchForm.deviceNo" placeholder="设备编号" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>变更类型</label>
          <el-select v-model="searchForm.changeType" placeholder="全部" clearable style="width: 100%">
            <el-option label="空闲" value="IDLE" />
            <el-option label="使用中" value="IN_USE" />
            <el-option label="维修中" value="MAINTAINING" />
            <el-option label="校准中" value="CALIBRATING" />
            <el-option label="报废" value="SCRAPPED" />
          </el-select>
        </div>
        <div class="search-field">
          <label>操作人</label>
          <el-input v-model="searchForm.operator" placeholder="操作人" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field search-field--daterange">
          <label>时间范围</label>
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="daterange-picker"
          />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="handleExport" style="margin-left: 8px">导出日志</el-button>
        </div>
      </div>
    </div>

    <!-- 状态变更统计 -->
    <div class="page-card stats-card">
      <div class="stats-grid">
        <div class="stat-item">
          <span class="stat-num">{{ total }}</span>
          <span class="stat-label">总变更记录</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #00FF88">{{ stats.idle }}</span>
          <span class="stat-label">→ 空闲</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #00D4FF">{{ stats.inUse }}</span>
          <span class="stat-label">→ 使用中</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #FF9500">{{ stats.maintaining }}</span>
          <span class="stat-label">→ 维修中</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #7B61FF">{{ stats.calibrating }}</span>
          <span class="stat-label">→ 校准中</span>
        </div>
        <div class="stat-item">
          <span class="stat-num" style="color: #FF4757">{{ stats.scrapped }}</span>
          <span class="stat-label">→ 报废</span>
        </div>
      </div>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="deviceName" label="设备名称" min-width="180" />
        <el-table-column prop="deviceNo" label="设备编号" min-width="130" />
        <el-table-column label="变更类型" width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="getChangeTypeTag(row.changeType)">
              {{ getChangeTypeText(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="变更详情" min-width="200">
          <template #default="{ row }">
            <span class="change-detail">
              {{ row.fromStatus }} → {{ row.toStatus }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="变更时间" width="170" :formatter="formatDate" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getGlobalDeviceStatusLogs, exportDeviceStatusLogs } from '@/api/device'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

const searchForm = reactive({
  deviceName: '',
  deviceNo: '',
  changeType: '',
  operator: '',
  dateRange: null
})
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const stats = reactive({ idle: 0, inUse: 0, maintaining: 0, calibrating: 0, scrapped: 0 })

const getChangeTypeTag = (t) => ({ IDLE: 'success', IN_USE: 'primary', MAINTAINING: 'warning', CALIBRATING: 'info', SCRAPPED: 'danger' }[t] || 'info')
const getChangeTypeText = (t) => ({ IDLE: '空闲', IN_USE: '使用中', MAINTAINING: '维修中', CALIBRATING: '校准中', SCRAPPED: '报废' }[t] || t)
const formatDate = (row) => dayjs(row.createTime).format('YYYY-MM-DD HH:mm:ss')

const buildQueryParams = (includePaging = true) => {
  const params = {}
  if (includePaging) {
    params.pageNum = pageNum.value
    params.pageSize = pageSize.value
  }
  if (searchForm.deviceName?.trim()) params.deviceName = searchForm.deviceName.trim()
  if (searchForm.deviceNo?.trim()) params.deviceNo = searchForm.deviceNo.trim()
  if (searchForm.changeType) params.changeType = searchForm.changeType
  if (searchForm.operator?.trim()) params.operator = searchForm.operator.trim()
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startDate = searchForm.dateRange[0]
    params.endDate = searchForm.dateRange[1]
  }
  return params
}

const loadData = async () => {
  try {
    const res = await getGlobalDeviceStatusLogs(buildQueryParams(true))
    tableData.value = res.list || []
    total.value = res.total || 0
    if (res.stats) Object.assign(stats, res.stats)
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败')
  }
}

const resetSearch = () => {
  searchForm.deviceName = ''
  searchForm.deviceNo = ''
  searchForm.changeType = ''
  searchForm.operator = ''
  searchForm.dateRange = null
  pageNum.value = 1
  loadData()
}

const handleExport = async () => {
  try {
    const res = await exportDeviceStatusLogs(buildQueryParams(false))
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `device_status_logs_${dayjs().format('YYYY-MM-DD')}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.device-track-tab {
  color: #E6EDF3;
  min-height: 100%;
}

.page-card {
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
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
  min-width: 140px;
  max-width: 200px;
  label {
    font-size: 11px;
    font-weight: 700;
    color: #8B949E;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
}

.search-field--daterange {
  flex: 0 1 auto;
  min-width: 260px;
  max-width: 320px;
}

.daterange-picker {
  width: 100% !important;
  max-width: 300px;
}

.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
  padding-bottom: 2px;
  margin-left: auto;
}

.stats-card {
  padding: 16px 24px;
}

.stats-grid {
  display: flex;
  gap: 32px;
  flex-wrap: wrap;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #E6EDF3;
}

.stat-label {
  font-size: 12px;
  color: #8B949E;
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

.change-detail {
  font-size: 13px;
  color: #E6EDF3;
  font-family: monospace;
}

.pagination-bar {
  padding: 14px 16px 10px;
  border-top: 1px solid #30363D;
  display: flex;
  justify-content: flex-end;
}
</style>
