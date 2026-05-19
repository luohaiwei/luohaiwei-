<template>
  <div class="device-scrap">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>设备报废管理</h2>
      </div>
      <el-button v-if="canSubmitApply" type="primary" @click="openApplyDialog">
        <el-icon><Plus /></el-icon> 提交报废申请
      </el-button>
    </div>

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
          <label>报废状态</label>
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 100%">
            <el-option label="申请中" :value="0" />
            <el-option label="审批通过" :value="1" />
            <el-option label="审批拒绝" :value="2" />
            <el-option label="已归档" :value="3" />
          </el-select>
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>导出
          </el-button>
        </div>
      </div>
      <div class="search-summary">
        共 <span class="highlight">{{ total }}</span> 条报废记录
      </div>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="deviceName" label="设备名称" min-width="180" />
        <el-table-column prop="deviceNo" label="设备编号" min-width="130" />
        <el-table-column prop="model" label="设备型号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="purchaseDate" label="购买日期" min-width="120" />
        <el-table-column prop="purchasePrice" label="购买价格(元)" min-width="110" align="right">
          <template #default="{ row }">{{ row.purchasePrice ? '¥' + row.purchasePrice.toLocaleString() : '-' }}</template>
        </el-table-column>
        <el-table-column prop="scrapReason" label="报废原因" min-width="200" show-overflow-tooltip />
        <el-table-column prop="applicant" label="申请人" min-width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getStatusTag(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" min-width="170" :formatter="formatDate" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" plain @click="handleView(row)">详情</el-button>
              <el-button v-if="canAudit && row.status === 0" size="small" type="success" @click="handleApprove(row)">
                审批通过
              </el-button>
              <el-button v-if="canAudit && row.status === 0" size="small" type="danger" plain @click="handleReject(row)">
                拒绝
              </el-button>
              <el-button v-if="canAudit && row.status === 1" size="small" type="warning" @click="handleArchive(row)">
                归档
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
    <el-dialog v-model="detailVisible" title="报废详情" width="600px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item label="设备编号">{{ detailData.deviceNo }}</el-descriptions-item>
        <el-descriptions-item label="设备型号">{{ detailData.model || '-' }}</el-descriptions-item>
        <el-descriptions-item label="购买日期">{{ detailData.purchaseDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="购买价格">{{ detailData.purchasePrice ? '¥' + detailData.purchasePrice.toLocaleString() : '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用年限">{{ detailData.usageYears || '-' }} 年</el-descriptions-item>
        <el-descriptions-item label="报废原因">{{ detailData.scrapReason }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailData.applicant }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="审批意见">{{ detailData.auditOpinion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批人">{{ detailData.auditor || '-' }}</el-descriptions-item>
        <el-descriptions-item label="审批时间">{{ detailData.auditTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="归档时间">{{ detailData.archiveTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="getStatusTag(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 报废申请弹窗 -->
    <el-dialog v-model="applyVisible" title="提交报废申请" width="560px" @close="resetApplyForm">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="110px">
        <el-form-item label="设备名称" prop="deviceId">
          <el-select v-model="applyForm.deviceId" placeholder="请选择要报废的设备" filterable style="width: 100%">
            <el-option
              v-for="device in deviceList"
              :key="device.id"
              :label="device.deviceName + ' (' + device.deviceNo + ')'"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="报废原因" prop="scrapReason">
          <el-input v-model="applyForm.scrapReason" type="textarea" :rows="4" placeholder="请详细描述报废原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applyLoading" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>

    <!-- 拒绝弹窗 -->
    <el-dialog v-model="rejectVisible" title="审批拒绝" width="480px">
      <el-form label-width="90px">
        <el-form-item label="拒绝原因" required>
          <el-input v-model="rejectOpinion" type="textarea" :rows="3" placeholder="请输入拒绝原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getScrapList, getScrapDetail, approveScrap, rejectScrap, archiveScrap, submitScrapApplication, exportScrapList } from '@/api/device'
import { getDeviceList } from '@/api/device'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const userType = computed(() => userStore.userInfo?.userType || userStore.userType || '')
const canSubmitApply = computed(() => ['SYSTEM_ADMIN', 'LAB_ADMIN'].includes(userType.value))
const canAudit = computed(() => ['SYSTEM_ADMIN', 'LAB_ADMIN'].includes(userType.value))

const searchForm = reactive({ deviceName: '', deviceNo: '', status: null })
const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const detailData = ref(null)
const applyVisible = ref(false)
const applyLoading = ref(false)
const applyFormRef = ref()
const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectOpinion = ref('')
const currentScrap = ref(null)
const deviceList = ref([])
const applyForm = reactive({ deviceId: null, scrapReason: '' })
const applyRules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  scrapReason: [{ required: true, message: '请填写报废原因', trigger: 'blur' }]
}

const getStatusTag = (s) => ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' })[s] || 'info'
const getStatusText = (s) => ({ 0: '申请中', 1: '审批通过', 2: '审批拒绝', 3: '已归档' })[s] || '未知'
const formatDate = (row) => dayjs(row.createTime).format('YYYY-MM-DD HH:mm')

const loadData = async () => {
  try {
    const res = await getScrapList({ pageNum: pageNum.value, pageSize: pageSize.value, ...searchForm })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败')
  }
}

const resetSearch = () => {
  searchForm.deviceName = ''
  searchForm.deviceNo = ''
  searchForm.status = null
  pageNum.value = 1
  loadData()
}

const loadDevices = async () => {
  try {
    const res = await getDeviceList({ pageNum: 1, pageSize: 1000 })
    deviceList.value = res.list || []
  } catch (e) {
    console.error(e)
  }
}

const handleView = async (row) => {
  try {
    detailData.value = await getScrapDetail(row.id)
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取详情失败')
  }
}

const handleApprove = async (row) => {
  try {
    await ElMessageBox.confirm(`确定审批通过设备「${row.deviceName}」的报废申请？`, '审批确认', { type: 'warning' })
    await approveScrap(row.id)
    ElMessage.success('审批通过')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleReject = (row) => {
  currentScrap.value = row
  rejectOpinion.value = ''
  rejectVisible.value = true
}

const submitReject = async () => {
  if (!rejectOpinion.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  rejectLoading.value = true
  try {
    await rejectScrap(currentScrap.value.id, rejectOpinion.value)
    ElMessage.success('已拒绝')
    rejectVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    rejectLoading.value = false
  }
}

const handleArchive = async (row) => {
  try {
    await ElMessageBox.confirm(`确定归档设备「${row.deviceName}」的报废记录？`, '归档确认', { type: 'warning' })
    await archiveScrap(row.id)
    ElMessage.success('已归档')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const openApplyDialog = () => {
  applyVisible.value = true
  loadDevices()
}

const resetApplyForm = () => {
  applyForm.deviceId = null
  applyForm.scrapReason = ''
}

const submitApply = async () => {
  if (!applyFormRef.value) return
  await applyFormRef.value.validate(async (valid) => {
    if (!valid) return
    applyLoading.value = true
    try {
      await submitScrapApplication(applyForm)
      ElMessage.success('报废申请已提交')
      applyVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error(e.message || '提交失败')
    } finally {
      applyLoading.value = false
    }
  })
}

const handleExport = async () => {
  try {
    const blob = await exportScrapList({
      deviceName: searchForm.deviceName || undefined,
      deviceNo: searchForm.deviceNo || undefined,
      status: searchForm.status ?? undefined
    })
    const file = blob instanceof Blob ? blob : new Blob([blob], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(file)
    const a = document.createElement('a')
    a.href = url
    a.download = `device_scrap_${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(e.message || '导出失败')
  }
}

// 如果从设备详情页跳转过来，自动打开申请对话框
const preSelectedDeviceId = computed(() => route.query.deviceId)

onMounted(() => {
  loadData()
  // 如果有预选的设备ID，自动打开申请对话框
  if (preSelectedDeviceId.value) {
    setTimeout(() => {
      openApplyDialog()
      // 设置预选的设备ID
      applyForm.deviceId = Number(preSelectedDeviceId.value)
    }, 100)
  }
})
</script>

<style scoped lang="scss">
.device-scrap { color: #E6EDF3; min-height: 100%; }

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
  border-left: 4px solid #FF4757;
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
  gap: 16px;
  flex-wrap: wrap;
}

.search-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 140px;
  max-width: 220px;
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
  .highlight { color: #FF4757; font-weight: 700; font-size: 15px; }
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
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
