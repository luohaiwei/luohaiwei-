<template>
  <div class="health-record">
    <div class="page-header">
      <h2>设备健康档案</h2>
    </div>

    <div class="filter-row">
      <el-select
        v-model="deviceId"
        placeholder="全部设备（可筛选具体设备）"
        clearable
        filterable
        class="device-filter"
        @change="onDeviceChange"
      >
        <el-option v-for="d in deviceList" :key="d.id" :label="`${d.deviceName} (${d.deviceNo})`" :value="d.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>
    <p v-if="!deviceId" class="scope-tip">
      当前展示<strong>全部设备</strong>的维修与校准记录；选择设备可查看该设备详情与对应记录。
    </p>

    <template v-if="deviceId">
      <el-descriptions :column="2" border class="device-info">
        <el-descriptions-item label="设备名称">{{ deviceDetail?.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ deviceDetail?.deviceNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(deviceDetail?.status)">{{ getStatusText(deviceDetail?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="下次校准">{{
          deviceDetail?.nextCalibrationDate ? formatDate(deviceDetail.nextCalibrationDate) : '-'
        }}</el-descriptions-item>
      </el-descriptions>
    </template>

    <div class="table-wrapper" :class="{ 'is-loading': loading }">
      <h3 class="section-title">维修记录{{ deviceId ? '' : '（全部）' }}</h3>
      <el-table v-loading="loading" :data="repairList" border stripe size="small">
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deviceNo" label="设备编号" width="120" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="工单号" width="120" />
        <el-table-column prop="faultDescription" label="故障描述" show-overflow-tooltip />
        <el-table-column prop="reportTime" label="报修时间" width="160" :formatter="(r, c, v) => formatDate(v)" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'success' : row.status === 1 ? 'primary' : 'warning'">
              {{ row.status === 0 ? '待处理' : row.status === 1 ? '处理中' : row.status === 2 ? '已完成' : '已关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="repairSolution" label="维修方案" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无维修记录" :image-size="80" />
        </template>
      </el-table>

      <h3 class="section-title">校准记录{{ deviceId ? '' : '（全部）' }}</h3>
      <el-table v-loading="loading" :data="calibrationList" border stripe size="small">
        <el-table-column prop="deviceName" label="设备名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="deviceNo" label="设备编号" width="120" show-overflow-tooltip />
        <el-table-column prop="calibrationDate" label="校准时间" width="160" :formatter="(r, c, v) => formatDate(v)" />
        <el-table-column prop="result" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.result === 1 ? 'success' : 'danger'">{{ row.result === 1 ? '合格' : '不合格' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="nextCalibrationDate" label="下次校准" width="120" :formatter="(r, c, v) => formatDate(v)" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无校准记录" :image-size="80" />
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllDevices, getDeviceDetail } from '@/api/device'
import { getRepairList } from '@/api/repair'
import { getCalibrationList } from '@/api/calibration'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'

/** 列表单次加载条数（健康档案汇总） */
const LIST_PAGE_SIZE = 300

const deviceId = ref(null)
const deviceList = ref([])
const deviceDetail = ref(null)
const repairList = ref([])
const calibrationList = ref([])
const loading = ref(true)  // 初始 true，加载完成前表格不可交互

const formatDate = (v) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-')
const getStatusType = (s) => ({ 0: 'success', 1: 'primary', 2: 'warning', 3: 'info', 4: 'danger' }[s] || 'info')
const getStatusText = (s) => ({ 0: '空闲', 1: '使用中', 2: '维修中', 3: '校准中', 4: '报废' }[s] || '未知')

const loadDevices = async () => {
  try {
    deviceList.value = (await getAllDevices()) || []
  } catch (e) {
    console.error(e)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const paramsBase = { pageNum: 1, pageSize: LIST_PAGE_SIZE }
    if (deviceId.value) {
      paramsBase.deviceId = deviceId.value
    }

    if (deviceId.value) {
      deviceDetail.value = await getDeviceDetail(deviceId.value)
    } else {
      deviceDetail.value = null
    }

    const [repairRes, calRes] = await Promise.all([
      getRepairList(paramsBase),
      getCalibrationList(paramsBase)
    ])
    repairList.value = repairRes.list || []
    calibrationList.value = calRes.list || []
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败')
    repairList.value = []
    calibrationList.value = []
  } finally {
    loading.value = false
  }
}

const onDeviceChange = () => {
  loadData()
}

const resetFilter = () => {
  deviceId.value = null
  loadData()
}

onMounted(async () => {
  await loadDevices()
  await loadData()
})
</script>

<style lang="scss" scoped>
.health-record {
  min-height: 100vh;
  background: #0d1117;      // 消除白色 flash：和 layout 背景一致
  padding: 0 24px 24px;
  box-sizing: border-box;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.device-filter {
  width: 360px;
  max-width: 100%;
}

.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  color: #e6edf3;
}
.scope-tip {
  margin: 0 0 16px;
  font-size: 13px;
  color: #8b949e;
  line-height: 1.5;
}
.scope-tip strong {
  color: #00d4ff;
}
.table-wrapper {
  // 加载时整体变暗，避免空白表格闪现
  &.is-loading .el-table__body-wrapper {
    opacity: 0.3;
    pointer-events: none;
  }
}
.section-title {
  margin: 20px 0 12px;
  color: #e6edf3;
  font-size: 16px;
  font-weight: 600;
}
.device-info {
  background: #161b22;
}

// ---- 表格深色主题（全局穿透，非 scoped 防止样式丢失） ----
:deep(.el-table) {
  --el-table-bg-color: #161b22;
  --el-table-tr-bg-color: #161b22;
  --el-table-header-bg-color: #1a1f35;
  --el-table-row-hover-bg-color: #1f2937;
  --el-table-border-color: #30363d;
  --el-table-text-color: #e6edf3;
  --el-table-header-text-color: #8b949e;

  background: #161b22;
  color: #e6edf3;

  thead th.el-table__cell {
    background: #1a1f35 !important;
    color: #8b949e !important;
    font-size: 12px;
    font-weight: 700;
    padding: 12px 8px;
    border-bottom: 1px solid #30363d !important;
  }

  td.el-table__cell {
    background: #161b22 !important;
    padding: 10px 8px;
    border-bottom: 1px solid #21262d !important;
    color: #e6edf3;
  }

  tr {
    background: #161b22 !important;
    &:hover > td,
    &.hover-row > td {
      background: #1f2937 !important;
    }
  }

  // 表尾空白区
  .el-table__empty-text {
    color: #8b949e;
  }

  // 排序图标
  .ascending .ascending,
  .descending .descending {
    color: #00d4ff;
  }
}

// el-descriptions 深色
:deep(.el-descriptions__body) {
  background: #161b22;
}
:deep(.el-descriptions__cell) {
  border-color: #30363d !important;
}
:deep(.el-descriptions__label) {
  background: #21262d !important;
  color: #8b949e !important;
}
:deep(.el-descriptions__content) {
  background: #161b22 !important;
  color: #e6edf3 !important;
}

// el-empty 深色
:deep(.el-empty__description) {
  color: #8b949e;
}
</style>
