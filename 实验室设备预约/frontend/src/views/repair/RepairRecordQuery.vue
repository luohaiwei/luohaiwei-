<template>
  <div class="repair-record-page">
    <div class="page-header">
      <h2>维修记录查询</h2>
    </div>

    <div class="search-bar">
      <el-input v-model="query.orderNo" placeholder="工单编号" clearable class="search-item" />
      <el-input v-model="query.deviceName" placeholder="设备名称" clearable class="search-item" />
      <div class="date-range-wrap">
        <el-date-picker
          v-model="query.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="报修开始"
          end-placeholder="报修结束"
          value-format="YYYY-MM-DD"
          class="date-range-picker"
        />
      </div>
      <el-select v-model="query.status" placeholder="全部状态" clearable class="search-item narrow">
        <el-option label="已完成" :value="2" />
        <el-option label="已关闭" :value="3" />
      </el-select>
      <el-button type="primary" @click="fetchData">查询</el-button>
      <el-button @click="resetQuery">重置</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="orderNo" label="工单编号" width="160" />
      <el-table-column prop="deviceName" label="设备名称" min-width="120" />
      <el-table-column prop="reporterName" label="报修人" width="100" />
      <el-table-column prop="handlerName" label="处理人" width="100" />
      <el-table-column prop="faultDescription" label="故障描述" show-overflow-tooltip min-width="140" />
      <el-table-column prop="reportTime" label="报修时间" width="170" />
      <el-table-column prop="handleEndTime" label="完成时间" width="170" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 2" type="success">已完成</el-tag>
          <el-tag v-else-if="row.status === 3" type="info">已关闭</el-tag>
          <el-tag v-else type="warning">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleView(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @size-change="fetchData"
      @current-change="fetchData"
    />

    <el-dialog v-model="detailVisible" title="维修记录详情" width="700px" class="repair-detail-dialog">
      <div v-if="detailData.orderNo" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="工单编号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag v-if="detailData.status === 2" type="success">已完成</el-tag>
            <el-tag v-else-if="detailData.status === 3" type="info">已关闭</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
          <el-descriptions-item label="设备编号">{{ detailData.deviceNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报修人">{{ detailData.reporterName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="处理人">{{ detailData.handlerName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报修时间" :span="2">{{ detailData.reportTime }}</el-descriptions-item>
          <el-descriptions-item label="故障描述" :span="2">{{ detailData.faultDescription }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detailData.imagePath" class="fault-img-block">
          <span class="label">故障图片</span>
          <img class="fault-img" :src="repairImageUrl(detailData.imagePath)" alt="" />
        </div>
        <el-descriptions v-if="detailData.status === 2 || detailData.status === 3" :column="1" border class="mt">
          <el-descriptions-item label="故障原因">{{ detailData.faultCause || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修方案">{{ detailData.repairSolution || '-' }}</el-descriptions-item>
          <el-descriptions-item label="维修费用">{{ detailData.repairCost != null ? '¥' + detailData.repairCost : '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ detailData.handleEndTime || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRepairRecords } from '@/api/repair'

const loading = ref(false)
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

const query = reactive({
  orderNo: '',
  deviceName: '',
  dateRange: null,
  status: undefined
})

const detailVisible = ref(false)
const detailData = ref({})

const repairImageUrl = (p) => {
  if (!p) return ''
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  const s = p.startsWith('/') ? p : `/${p}`
  return `/api${s}`
}

const buildParams = () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    orderNo: query.orderNo?.trim() || undefined,
    deviceName: query.deviceName?.trim() || undefined,
    status: query.status === '' || query.status === undefined ? undefined : query.status
  }
  if (query.dateRange && query.dateRange.length === 2) {
    params.reportDateFrom = query.dateRange[0]
    params.reportDateTo = query.dateRange[1]
  }
  return params
}

let fetchId = 0
const fetchData = async () => {
  const id = ++fetchId
  loading.value = true
  try {
    const res = await getRepairRecords(buildParams())
    if (id !== fetchId) return
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    if (id !== fetchId) return
    ElMessage.error(e.message || '查询失败')
  } finally {
    if (id === fetchId) loading.value = false
  }
}

const resetQuery = () => {
  query.orderNo = ''
  query.deviceName = ''
  query.dateRange = null
  query.status = undefined
  pageNum.value = 1
  fetchData()
}

const handleView = (row) => {
  detailData.value = row
  detailVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.repair-record-page {
  color: #e6edf3;
}
.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0 0 8px;
  color: #e6edf3;
}
.page-desc {
  margin: 0;
  font-size: 13px;
  color: #8b949e;
}
.search-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 20px;
}
.search-item {
  width: 180px;
  flex: 0 0 180px;
  max-width: 100%;
}
/* 限制日期范围选择器宽度，避免在 flex 布局下被撑满一行 */
.date-range-wrap {
  width: fit-content;
  max-width: 100%;
  flex: 0 0 fit-content;
}
.date-range-picker {
  width: 100%;
}
:deep(.date-range-wrap .el-date-editor.el-input__wrapper) {
  width: 100% !important;
  max-width: 100%;
}
.search-item.narrow {
  width: 140px;
  flex: 0 0 140px;
}
.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
.detail-content {
  color: #e6edf3;
}
.fault-img-block {
  margin: 16px 0;
}
.fault-img-block .label {
  display: block;
  color: #8b949e;
  margin-bottom: 8px;
}
.fault-img {
  max-width: 100%;
  max-height: 240px;
  border-radius: 8px;
  border: 1px solid #30363d;
}
.mt {
  margin-top: 16px;
}
:deep(.el-table) {
  background: #161b22;
  color: #e6edf3;
}
:deep(.el-dialog) {
  background: #161b22;
  border: 1px solid #30363d;
}
:deep(.el-descriptions__label) {
  background: #21262d !important;
  color: #8b949e !important;
}
:deep(.el-descriptions__content) {
  background: #161b22 !important;
  color: #e6edf3 !important;
}
</style>
