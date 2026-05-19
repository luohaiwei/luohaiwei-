<template>
  <div class="calibration-page">
    <div class="page-header">
      <h2>校准管理</h2>
      <el-button type="primary" @click="openAdd">新增校准记录</el-button>
    </div>

    <!-- 校准到期提醒看板 -->
    <div v-if="upcomingList.length > 0" class="upcoming-alert">
      <div class="alert-title">
        <el-icon><Warning /></el-icon>
        校准到期提醒（未来30天内）
      </div>
      <div class="alert-list">
        <div v-for="item in upcomingList" :key="item.id" class="alert-item">
          <span class="device-name">{{ item.deviceName }}</span>
          <span class="device-no">{{ item.deviceNo }}</span>
          <span class="days-left" :class="item.daysLeft <= 3 ? 'urgent' : ''">
            {{ item.daysLeft }}天后到期
          </span>
          <el-button size="small" type="primary" @click="openAddWithDevice(item)">去校准</el-button>
        </div>
      </div>
    </div>

    <div class="search-area">
      <el-select v-model="searchDeviceId" placeholder="选择设备" clearable style="width: 200px" filterable>
        <el-option v-for="d in deviceList" :key="d.id" :label="d.deviceName" :value="d.id" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading" :scrollbar-always-on="true">
      <el-table-column prop="deviceName" label="设备名称" width="140" show-overflow-tooltip />
      <el-table-column prop="deviceNo" label="设备编号" width="120" show-overflow-tooltip />
      <el-table-column prop="calibrationDate" label="校准时间" width="150" :formatter="formatDate" />
      <el-table-column prop="result" label="结果" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">{{ row.result === 1 ? '合格' : '不合格' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nextCalibrationDate" label="下次校准" width="120" :formatter="formatDate" />
      <el-table-column prop="remark" label="备注" min-width="100" show-overflow-tooltip />
      <el-table-column label="操作" align="center" fixed="right">
        <template #default="{ row }">
          <div class="action-btns">
            <el-button size="small" type="primary" @click="openEdit(row)">
              <el-icon><Edit /></el-icon><span>编辑</span>
            </el-button>
            <el-button size="small" type="success" @click="openReport(row)">
              <el-icon><Document /></el-icon><span>报告</span>
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon><span>删除</span>
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 16px"
      @current-change="loadData"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑校准记录' : '新增校准记录'" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="设备" prop="deviceId">
          <el-select v-model="form.deviceId" placeholder="请选择设备" style="width: 100%" filterable>
            <el-option v-for="d in deviceList" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="校准周期(天)">
          <el-input-number v-model="form.calibrationCycle" :min="1" />
        </el-form-item>
        <el-form-item label="校准结果">
          <el-radio-group v-model="form.result">
            <el-radio :label="1">合格</el-radio>
            <el-radio :label="0">不合格</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="下次校准日期" prop="nextCalibrationDate">
          <el-date-picker
            v-model="form.nextCalibrationDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择下次校准日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 校准报告对话框 -->
    <el-dialog v-model="reportVisible" title="校准报告" width="600px">
      <div v-if="reportData.record" class="report-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="设备名称">{{ reportData.deviceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备编号">{{ reportData.deviceNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="设备型号">{{ reportData.deviceModel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="生产厂商">{{ reportData.manufacturer || '-' }}</el-descriptions-item>
          <el-descriptions-item label="校准日期" :span="2">{{ reportData.record.calibrationDate }}</el-descriptions-item>
          <el-descriptions-item label="校准结果" :span="2">
            <el-tag :type="reportData.record.result === 1 ? 'success' : 'danger'">
              {{ reportData.record.result === 1 ? '合格' : '不合格' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="下次校准日期">{{ reportData.record.nextCalibrationDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="校准人员">{{ reportData.calibratorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ reportData.record.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="reportVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCalibrationList, getCalibrationReport, getUpcomingCalibrations, addCalibration, updateCalibration, deleteCalibration } from '@/api/calibration'
import { getAllDevices } from '@/api/device'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Delete, Warning, Document } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const searchDeviceId = ref(null)
const tableData = ref([])
const deviceList = ref([])
const upcomingList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({
  deviceId: null,
  calibrationCycle: 180,
  result: 1,
  nextCalibrationDate: '',
  remark: ''
})
const rules = {
  deviceId: [{ required: true, message: '请选择设备', trigger: 'change' }],
  nextCalibrationDate: [{ required: true, message: '请选择下次校准日期', trigger: 'change' }]
}

// 报告相关
const reportVisible = ref(false)
const reportData = ref({})

const formatDate = (row, col, val) => val ? dayjs(val).format('YYYY-MM-DD') : '-'

const loadDevices = async () => {
  try {
    deviceList.value = await getAllDevices() || []
  } catch (e) { console.error(e) }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCalibrationList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      deviceId: searchDeviceId.value
    })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

const loadUpcoming = async () => {
  try {
    const res = await getUpcomingCalibrations(30)
    upcomingList.value = res.list || []
  } catch (e) { console.error(e) }
}

const openReport = async (row) => {
  try {
    reportData.value = await getCalibrationReport(row.id)
    reportVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '加载报告失败')
  }
}

const openAddWithDevice = (item) => {
  form.deviceId = item.id
  openAdd()
}

const openAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  form.nextCalibrationDate = row.nextCalibrationDate ? dayjs(row.nextCalibrationDate).format('YYYY-MM-DD') : ''
  dialogVisible.value = true
}

const resetForm = () => {
  form.deviceId = null
  form.calibrationCycle = 180
  form.result = 1
  form.nextCalibrationDate = ''
  form.remark = ''
}

const resetSearch = () => {
  searchDeviceId.value = null
  pageNum.value = 1
  loadData()
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      const data = { ...form }
      if (data.nextCalibrationDate) {
        data.nextCalibrationDate = `${data.nextCalibrationDate}T00:00:00`
      }
      if (isEdit.value) {
        await updateCalibration(data)
      } else {
        await addCalibration(data)
      }
      ElMessage.success('操作成功')
      dialogVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该记录吗？', '提示', { type: 'warning' })
    await deleteCalibration(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadDevices()
  loadData()
  loadUpcoming()
})
</script>

<style lang="scss" scoped>
.calibration-page { color: #E6EDF3; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; color: #E6EDF3; }
.search-area { margin-bottom: 20px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }

.upcoming-alert {
  background: #1f1500;
  border: 1px solid #7b6100;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}
.alert-title {
  color: #ff9500;
  font-weight: bold;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.alert-list { display: flex; flex-direction: column; gap: 8px; }
.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #161b22;
  border-radius: 6px;
  padding: 8px 12px;
}
.device-name { color: #e6edf3; font-weight: bold; min-width: 160px; }
.device-no { color: #8b949e; font-size: 12px; }
.days-left { color: #ff9500; font-size: 13px; }
.days-left.urgent { color: #ff4757; font-weight: bold; }

.report-content { color: #e6edf3; }

:deep(.el-table) {
  background: #161B22;
  color: #E6EDF3;
  --el-table-border-color: #30363d;
  --el-table-header-bg-color: #21262D;
  --el-table-header-text-color: #E6EDF3;
  --el-table-row-hover-bg-color: #1F2937;
}
:deep(.el-table__header) {
  background: #21262D;
}
:deep(.el-table__row) {
  background: #161B22;
}
:deep(.el-table__row:hover) {
  background: #1F2937 !important;
}
:deep(.el-table th.el-table__cell) {
  background: #21262D;
  color: #E6EDF3;
  padding: 8px 0;
}
:deep(.el-table td.el-table__cell) {
  color: #E6EDF3;
  padding: 10px 0;
}
:deep(.el-table__body-wrapper) {
  background: #161B22;
}

.action-btns {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}
.action-btns .el-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  font-size: 13px;
}
.action-btns .el-button .el-icon {
  margin: 0;
  font-size: 14px;
}

:deep(.el-dialog) { background: #161B22; border: 1px solid #30363d; }
:deep(.el-descriptions__label) { background: #21262D !important; color: #8b949e !important; }
:deep(.el-descriptions__content) { background: #161B22 !important; color: #E6EDF3 !important; }
:deep(.el-pagination) { justify-content: flex-end; }
:deep(.el-pagination button) { background: #161B22 !important; color: #E6EDF3 !important; }
:deep(.el-pager li) { background: #161B22 !important; color: #E6EDF3 !important; }
:deep(.el-pager li.is-active) { color: #00D4FF !important; }
:deep(.el-pagination .el-pagination__total) { color: #8b949e; }
</style>
