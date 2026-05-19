<template>
  <div class="booking-global-container">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>{{ pageTitle }}</h2>
      </div>
      <el-button type="primary" plain @click="loadData">
        <el-icon><Refresh /></el-icon> 刷新
      </el-button>
    </div>

    <div class="page-card search-card">
      <div class="search-row">
        <div class="search-field">
          <label>预约单号</label>
          <el-input v-model="searchForm.orderNo" placeholder="预约单号" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>设备名称</label>
          <el-input v-model="searchForm.deviceName" placeholder="设备名称" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>申请人</label>
          <el-input v-model="searchForm.userName" placeholder="申请人" clearable @keyup.enter="loadData" />
        </div>
        <div class="search-field">
          <label>预约状态</label>
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 100%">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已拒绝" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </div>
        <div class="search-field">
          <label>审核结果</label>
          <el-select v-model="searchForm.auditStatus" placeholder="全部" clearable style="width: 100%">
            <el-option label="正常" value="normal" />
            <el-option label="有驳回" value="rejected" />
            <el-option label="高取消率" value="highCancel" />
          </el-select>
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button v-if="canExportBookings" type="success" @click="handleExport">
            <el-icon><Download /></el-icon>导出
          </el-button>
        </div>
      </div>
      <div class="search-summary">
        共 <span class="highlight">{{ total }}</span> 条预约记录
      </div>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="orderNo" label="预约单号" min-width="150" />
        <el-table-column label="设备名称" min-width="180">
          <template #default="{ row }">
            <span>{{ row.deviceName || '-' }}</span>
            <span v-if="row.replaceDeviceName" class="replace-hint">（拟换：{{ row.replaceDeviceName }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="申请人" min-width="120">
          <template #default="{ row }">
            <span>{{ row.userName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="bookingDate" label="预约日期" min-width="120" />
        <el-table-column label="时段" min-width="120">
          <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column prop="experimentProject" label="实验项目" min-width="140" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="420" fixed="right">
          <template #default="{ row }">
            <div class="op-actions">
              <el-button size="small" type="primary" plain @click="handleView(row)">详情</el-button>
              <el-button
                v-if="row.status === 1 || row.status === 5"
                size="small"
                type="success"
                plain
                @click="handleAdminComplete(row)"
              >
                标记完成
              </el-button>
              <el-button
                v-if="row.status === 1 || row.status === 5"
                size="small"
                type="warning"
                plain
                @click="handleAdminNoShow(row)"
              >
                标记爽约
              </el-button>
              <el-button
                v-if="isSystemAdmin && row.status !== 3 && row.status !== 4"
                size="small"
                type="danger"
                plain
                @click="handleForceCancel(row)"
              >
                强制关闭
              </el-button>
            </div>
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
    <el-dialog v-model="detailVisible" title="预约详情" width="600px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="预约单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.replaceDeviceName" label="替换设备">{{ detailData.replaceDeviceName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailData.userName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ detailData.bookingDate }}</el-descriptions-item>
        <el-descriptions-item label="使用时段">{{ detailData.startTime }} - {{ detailData.endTime }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ detailData.participantCount }}</el-descriptions-item>
        <el-descriptions-item label="实验项目">{{ detailData.experimentProject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约事由">{{ detailData.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.auditOpinion" label="审核意见">{{ detailData.auditOpinion }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getBookingList, getBookingDetail, forceCloseBooking, exportBookingList, adminCompleteBooking, adminNoShowBooking } from '@/api/booking'
import { getMyDataScope } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Download } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isSystemAdmin = computed(() => userStore.userInfo?.userType === 'SYSTEM_ADMIN')
const bookingScope = ref([])
const canExportBookings = computed(() => {
  if (isSystemAdmin.value) return true
  if (!Array.isArray(bookingScope.value)) return false
  return bookingScope.value.some(s => String(s).trim().toUpperCase() === 'EXPORT')
})
const pageTitle = computed(() =>
  isSystemAdmin.value ? '预约全局列表' : '预约列表'
)
const pageSub = computed(() =>
  isSystemAdmin.value
    ? '系统全量预约申请查看、状态监控与异常干预（无审核权限，仅供监控）'
    : '本实验室（数据权限范围内）预约记录查询与状态查看；审核请在「预约审核」中处理'
)

const searchForm = reactive({
  orderNo: '',
  deviceName: '',
  userName: '',
  status: null,
  auditStatus: ''
})
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const detailData = ref(null)

const getStatusType = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info' })[s] || 'info'
const getStatusText = (s) => ({ 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已完成', 4: '已取消' })[s] || '未知'

const loadData = async () => {
  try {
    const res = await getBookingList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      orderNo: searchForm.orderNo || null,
      deviceName: searchForm.deviceName || null,
      userName: searchForm.userName || null,
      status: searchForm.status ?? null,
      auditStatus: searchForm.auditStatus || null
    })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    // 业务异常由 catch 捕获；网络层异常（无 response）在 request 拦截器已弹窗
    if (e?.response) {
      ElMessage.error('加载数据失败：' + (e.response.data?.message || e.message || '未知错误'))
    }
    tableData.value = []
  }
}

const resetSearch = () => {
  searchForm.orderNo = ''
  searchForm.deviceName = ''
  searchForm.userName = ''
  searchForm.status = null
  searchForm.auditStatus = ''
  pageNum.value = 1
  loadData()
}

const handleView = async (row) => {
  try {
    detailData.value = await getBookingDetail(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取详情失败')
  }
}

const handleAdminComplete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定将预约「${row.orderNo}」标记为已完成？`,
      '标记完成',
      { type: 'info', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await adminCompleteBooking(row.id)
    ElMessage.success('已标记完成')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

const handleAdminNoShow = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定将预约「${row.orderNo}」标记为爽约？此操作将累加该学生的爽约次数。`,
      '标记爽约',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' }
    )
    await adminNoShowBooking(row.id)
    ElMessage.success('已标记爽约')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || e.message || '操作失败')
  }
}

const handleForceCancel = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定强制关闭预约「${row.orderNo}」？此操作不可逆。`,
      '强制关闭',
      { type: 'warning', confirmButtonText: '确定关闭', cancelButtonText: '取消' }
    )
    await forceCloseBooking(row.id)
    ElMessage.success('已强制关闭')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleExport = async () => {
  try {
    const res = await exportBookingList({ ...searchForm })
    if (!res) return
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/octet-stream' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预约全局列表_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  }
}

const loadBookingScope = async () => {
  try {
    const res = await getMyDataScope()
    bookingScope.value = res?.bookingScope || []
  } catch (e) {
    console.error(e)
    bookingScope.value = []
  }
}

onMounted(async () => {
  await loadBookingScope()
  loadData()
})
</script>

<style scoped lang="scss">
.booking-global-container {
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

.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 2px;
}

.search-summary {
  margin-top: 12px;
  font-size: 13px;
  color: #8B949E;
  border-top: 1px solid #30363D;
  padding-top: 10px;
  .highlight { color: #00D4FF; font-weight: 700; font-size: 15px; }
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
      vertical-align: middle;
      color: #E6EDF3;
    }
    tr:hover > td { background: #1F2937 !important; }
  }
}

.pagination-bar {
  padding: 14px 16px 10px;
  border-top: 1px solid #30363D;
  display: flex;
  justify-content: flex-end;
}

.replace-hint { color: #8B949E; font-size: 12px; margin-left: 4px; }

.op-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 8px;
  align-items: center;
}
.op-actions .el-button {
  margin: 0 !important;
}

:deep(.el-descriptions__body) { background: #161B22; }
:deep(.el-descriptions__cell) { border-color: #30363D !important; }
:deep(.el-descriptions__label) { background: #21262D !important; color: #8B949E !important; }
:deep(.el-descriptions__content) { background: #161B22 !important; color: #E6EDF3 !important; }
</style>
