<template>
  <div class="booking-list">
    <!-- 搜索区域 -->
    <div class="search-area">
      <el-select v-model="searchForm.status" placeholder="预约状态" clearable class="search-select">
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
        <el-option label="已完成" :value="3" />
        <el-option label="已取消" :value="4" />
        <el-option label="已签到" :value="5" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <el-button
        v-if="canShowBookingExport"
        type="success"
        plain
        :loading="exportLoading"
        @click="handleExportBookings"
      >
        <el-icon><Download /></el-icon>
        导出预约数据
      </el-button>
    </div>

    <!-- 预约列表 -->
    <el-table :data="bookingList" border stripe style="width: 100%" empty-text="暂无预约数据">
      <el-table-column prop="orderNo" label="预约单号" min-width="150" />
      <el-table-column label="设备名称" min-width="180">
        <template #default="{ row }">
          <span>{{ row.deviceName }}</span>
          <span v-if="row.replaceDeviceName" class="replace-hint">（拟换：{{ row.replaceDeviceName }}）</span>
        </template>
      </el-table-column>
      <el-table-column prop="bookingDate" label="预约日期" min-width="120" :formatter="formatDate" />
      <el-table-column prop="startTime" label="开始时间" min-width="80" />
      <el-table-column prop="endTime" label="结束时间" min-width="80" />
      <el-table-column prop="status" label="状态" min-width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最晚可取消" min-width="170">
        <template #default="{ row }">
          <span v-if="row.userId === myUserId && row.status === 1">{{ getCancelDeadlineText(row) }}</span>
          <span v-else style="color:#8b949e">-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="580" fixed="right">
        <template #default="{ row }">
          <el-button type="info" plain size="small" @click="handleDetail(row)">详情</el-button>
          <!-- 调整时间按钮：仅待审核的预约可调整 -->
          <el-button
            v-if="row.userId === myUserId && row.status === 0"
            type="primary"
            plain
            size="small"
            @click="handleAdjust(row)"
          >
            调整时间
          </el-button>
          <!-- 仅本人预约可签到/签退/取消 -->
          <el-button
            v-if="row.userId === myUserId && row.status === 1"
            type="success"
            size="small"
            @click="handleCheckIn(row)"
          >
            <el-icon><Position /></el-icon> 签到
          </el-button>
          <el-button
            v-if="row.userId === myUserId && row.status === 5"
            type="warning"
            size="small"
            @click="handleCheckOut(row)"
          >
            <el-icon><Switch /></el-icon> 签退
          </el-button>
          <el-button
            v-if="row.userId === myUserId && row.status === 1"
            type="success"
            size="small"
            plain
            @click="handleCompleteUse(row)"
          >完成使用</el-button>
          <el-button
            v-if="row.userId === myUserId && row.status === 0"
            type="warning"
            size="small"
            @click="handleReplace(row)"
          >申请替换</el-button>
          <el-button
            v-if="row.userId === myUserId && (row.status === 0 || row.status === 1)"
            type="danger"
            size="small"
            @click="handleCancel(row)"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 预约详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="560px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="预约单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.replaceDeviceName" label="替换设备">{{ detailData.replaceDeviceName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ formatDetailDate(detailData.bookingDate) }}</el-descriptions-item>
        <el-descriptions-item label="使用时段">{{ detailData.startTime }} - {{ detailData.endTime }}</el-descriptions-item>
        <el-descriptions-item label="参与人数">{{ detailData.participantCount }}</el-descriptions-item>
        <el-descriptions-item label="实验项目">{{ detailData.experimentProject || '-' }}</el-descriptions-item>
        <el-descriptions-item label="预约事由">{{ detailData.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.replaceReason" label="替换原因">{{ detailData.replaceReason }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusText(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.actualStartTime" label="签到时间">{{ formatDetailDateTime(detailData.actualStartTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.actualEndTime" label="签退时间">{{ formatDetailDateTime(detailData.actualEndTime) }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.evaluation" label="使用评价">{{ detailData.evaluation }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.auditOpinion" label="审核意见">{{ detailData.auditOpinion }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 签退评价弹窗 -->
    <el-dialog v-model="evaluationVisible" title="签退 - 使用评价" width="500px">
      <el-form :model="evaluationForm" label-width="100px">
        <el-form-item label="使用评价" required>
          <el-input
            v-model="evaluationForm.evaluation"
            type="textarea"
            :rows="4"
            placeholder="请描述本次使用设备的情况，如设备状态、操作体验等"
          />
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="evaluationForm.rating" allow-half show-text :texts="['极差', '较差', '一般', '良好', '优秀']" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="evaluationVisible = false">取消</el-button>
        <el-button type="primary" :loading="evaluationLoading" @click="submitEvaluation">确认签退</el-button>
      </template>
    </el-dialog>

    <!-- 设备替换弹窗 -->
    <el-dialog v-model="replaceVisible" title="申请设备替换" width="500px">
      <el-form v-if="currentBooking" :model="replaceForm" label-width="100px">
        <el-form-item label="原设备">
          <span>{{ currentBooking.deviceName }}</span>
        </el-form-item>
        <el-form-item label="可替换设备" required>
          <el-select v-model="replaceForm.newDeviceId" placeholder="请选择替换设备" filterable style="width: 100%">
            <el-option
              v-for="device in replaceableDevices"
              :key="device.id"
              :label="device.deviceName + ' (' + device.deviceNo + ')'"
              :value="device.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="替换原因">
          <el-input
            v-model="replaceForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入替换原因（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replaceVisible = false">取消</el-button>
        <el-button type="primary" :loading="replaceLoading" @click="submitReplace">确认替换</el-button>
      </template>
    </el-dialog>

    <!-- 时间调整弹窗 -->
    <el-dialog v-model="adjustVisible" title="调整预约时间" width="520px">
      <el-form v-if="currentBooking" :model="adjustForm" label-width="100px">
        <el-form-item label="原预约信息">
          <span>{{ currentBooking.deviceName }} | {{ formatDetailDate(currentBooking.bookingDate) }} {{ currentBooking.startTime }} - {{ currentBooking.endTime }}</span>
        </el-form-item>
        <el-form-item label="新预约日期" required>
          <el-date-picker
            v-model="adjustForm.newDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            style="width: 100%"
            :disabled-date="disabledPastDates"
          />
        </el-form-item>
        <el-form-item label="新开始时间" required>
          <el-select v-model="adjustForm.newStartTime" placeholder="选择" style="width: 100%">
            <el-option v-for="t in timeSlots" :key="t.value" :label="t.label" :value="t.value" :disabled="isTimePassed(t.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="新结束时间" required>
          <el-select v-model="adjustForm.newEndTime" placeholder="选择" style="width: 100%">
            <el-option v-for="t in timeSlots" :key="t.value" :label="t.label" :value="t.value" :disabled="isTimePassed(t.value)" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="请输入调整原因（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="adjustLoading" @click="submitAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getMyBookings,
  getBookingList,
  cancelBooking,
  getBookingDetail,
  applyDeviceReplace,
  getReplaceableDevices,
  completeBookingUse,
  checkInBooking,
  checkOutBooking,
  exportBookingList,
  adjustBooking,
  studentAdjustBooking
} from '@/api/booking'
import { getGlobalBookingRules } from '@/api/bookingRule'
import { getMyDataScope } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Position, Switch, Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'

const userStore = useUserStore()

const myUserId = computed(() => userStore.userInfo?.id ?? userStore.userInfo?.userId)

/** 实验室管理员 / 系统管理员 / 具备预约管理类权限的自定义角色：拉全局列表（与 /booking/my 区分） */
const useGlobalBookingList = computed(() => {
  const t = userStore.userInfo?.userType
  if (t === 'SYSTEM_ADMIN' || t === 'LAB_ADMIN') return true
  if (t === 'STUDENT' || t === 'TEACHER' || t === 'MAINTAINER') return false
  const perms = userStore.userInfo?.permissions || []
  return perms.some((p) => {
    if (!p) return false
    const s = String(p).toLowerCase()
    return (
      s === 'booking' ||
      s === 'booking-list' ||
      s.includes('booking-audit') ||
      s.includes('booking:audit') ||
      s.includes('booking:manage')
    )
  })
})

/** 与权限分配页「导出预约数据」一致；仅在看全局预约列表时展示导出（与 /booking/export 范围一致） */
const bookingScope = ref([])
const canBookingExport = computed(() => {
  const t = userStore.userInfo?.userType
  if (t === 'SYSTEM_ADMIN') return true
  if (!Array.isArray(bookingScope.value)) return false
  return bookingScope.value.some(
    s => String(s).trim().toUpperCase() === 'EXPORT'
  )
})
const canShowBookingExport = computed(() => canBookingExport.value && useGlobalBookingList.value)
const exportLoading = ref(false)

const loadBookingDataScope = async () => {
  try {
    const res = await getMyDataScope()
    bookingScope.value = res?.bookingScope || []
  } catch (e) {
    console.error(e)
    bookingScope.value = []
  }
}

const searchForm = reactive({ status: null })
const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const bookingList = ref([])
const detailVisible = ref(false)
const detailData = ref(null)

const cancelDeadlineHours = ref(0)
const replaceVisible = ref(false)
const replaceLoading = ref(false)
const currentBooking = ref(null)
const replaceableDevices = ref([])
const replaceForm = reactive({
  newDeviceId: null,
  reason: ''
})

// 签到签退相关
const evaluationVisible = ref(false)
const evaluationLoading = ref(false)
const evaluationForm = reactive({
  evaluation: '',
  rating: 0
})
const currentCheckOutBooking = ref(null)

// 调整时间相关
const adjustVisible = ref(false)
const adjustLoading = ref(false)
const adjustForm = reactive({
  newDate: '',
  newStartTime: '',
  newEndTime: '',
  reason: ''
})
const timeSlots = [
  { value: '08:00', label: '08:00' },
  { value: '09:00', label: '09:00' },
  { value: '10:00', label: '10:00' },
  { value: '11:00', label: '11:00' },
  { value: '12:00', label: '12:00' },
  { value: '13:00', label: '13:00' },
  { value: '14:00', label: '14:00' },
  { value: '15:00', label: '15:00' },
  { value: '16:00', label: '16:00' },
  { value: '17:00', label: '17:00' },
  { value: '18:00', label: '18:00' },
  { value: '19:00', label: '19:00' },
  { value: '20:00', label: '20:00' }
]
const disabledPastDates = (date) => date < new Date(new Date().setHours(0, 0, 0, 0))
const isToday = (dateStr) => {
  if (!dateStr) return false
  const today = new Date().toISOString().slice(0, 10)
  return dateStr === today
}
const getCurrentTimeStr = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}
const isTimePassed = (timeStr) => {
  if (!isToday(adjustForm.newDate)) return false
  return timeStr <= getCurrentTimeStr()
}

const resetSearch = () => {
  searchForm.status = null
  pagination.pageNum = 1
  loadData()
}

const handleExportBookings = async () => {
  try {
    exportLoading.value = true
    const params = {
      status: searchForm.status === null || searchForm.status === undefined ? undefined : searchForm.status
    }
    const res = await exportBookingList(params)
    if (!res) return
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预约数据_${new Date().toISOString().slice(0, 10)}.csv`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error(e?.message || '导出失败')
  } finally {
    exportLoading.value = false
  }
}

const loadData = async () => {
  try {
    const params = { pageNum: pagination.pageNum, pageSize: pagination.pageSize, status: searchForm.status }
    const res = useGlobalBookingList.value
      ? await getBookingList(params)
      : await getMyBookings({ ...pagination, ...searchForm })
    bookingList.value = res.list
    pagination.total = res.total
  } catch (e) {
    console.error(e)
  }
}

const formatDate = (row) => dayjs(row.bookingDate).format('YYYY-MM-DD')

const getStatusType = (status) => {
  const types = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info', 5: 'primary' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已完成', 4: '已取消', 5: '已签到' }
  return texts[status] || '未知'
}

const formatDetailDate = (v) => v ? dayjs(v).format('YYYY-MM-DD') : '-'

const formatDetailDateTime = (v) => v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'

const loadCancelDeadlineRule = async () => {
  try {
    const res = await getGlobalBookingRules()
    const hrs = Number(res?.rule?.cancelDeadlineHours)
    cancelDeadlineHours.value = Number.isFinite(hrs) ? hrs : 0
  } catch {
    cancelDeadlineHours.value = 0
  }
}

const getCancelDeadlineText = (row) => {
  const h = cancelDeadlineHours.value
  if (!h || !row?.bookingDate || !row?.startTime) return '-'
  const dateStr = dayjs(row.bookingDate).format('YYYY-MM-DD')
  const start = dayjs(`${dateStr} ${row.startTime}`)
  if (!start.isValid()) return '-'
  return start.subtract(h, 'hour').format('YYYY-MM-DD HH:mm')
}

const handleDetail = async (row) => {
  try {
    const res = await getBookingDetail(row.id)
    detailData.value = res
    detailVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取详情失败')
  }
}

const parseErrMsg = (e, fallback = '操作失败') => {
  const d = e?.response?.data
  if (typeof d === 'string' && d.trim()) return d
  if (d && typeof d === 'object' && d.message) return String(d.message)
  return e?.message || fallback
}

const handleCancel = async (row) => {
  try {
    await ElMessageBox.confirm('确定要取消该预约吗？', '提示', { type: 'warning' })
    await cancelBooking(row.id)
    ElMessage.success('取消成功')
    loadData()
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.error(parseErrMsg(e, '取消失败'))
  }
}

const handleCheckIn = async (row) => {
  try {
    await ElMessageBox.confirm(
      `您确定要签到吗？\n预约设备：${row.deviceName}\n预约时间：${dayjs(row.bookingDate).format('YYYY-MM-DD')} ${row.startTime}-${row.endTime}`,
      '签到确认',
      { type: 'info' }
    )
    await checkInBooking(row.id)
    ElMessage.success('签到成功！设备使用中')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '签到失败')
  }
}

const handleCheckOut = async (row) => {
  currentCheckOutBooking.value = row
  evaluationForm.evaluation = ''
  evaluationForm.rating = 0
  evaluationVisible.value = true
}

const submitEvaluation = async () => {
  if (!evaluationForm.evaluation.trim()) {
    ElMessage.warning('请输入使用评价')
    return
  }
  try {
    evaluationLoading.value = true
    const evaluationText = `${evaluationForm.evaluation}${evaluationForm.rating ? ' [评分：' + evaluationForm.rating + '/5]' : ''}`
    await checkOutBooking(currentCheckOutBooking.value.id, evaluationText)
    ElMessage.success('签退成功！感谢您的评价')
    evaluationVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '签退失败')
  } finally {
    evaluationLoading.value = false
  }
}

const handleCompleteUse = async (row) => {
  try {
    await ElMessageBox.confirm('确认已结束本次设备使用？系统将标记预约为已完成并释放设备占用。', '完成使用', { type: 'info' })
    await completeBookingUse(row.id)
    ElMessage.success('已标记使用完成')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const handleReplace = async (row) => {
  try {
    currentBooking.value = row
    replaceForm.newDeviceId = null
    replaceForm.reason = ''
    const res = await getReplaceableDevices(row.id)
    replaceableDevices.value = res.devices || []
    if (replaceableDevices.value.length === 0) {
      ElMessage.warning('暂无可替换的设备')
      return
    }
    replaceVisible.value = true
  } catch (e) {
    ElMessage.error(e.message || '获取可替换设备失败')
  }
}

const submitReplace = async () => {
  if (!replaceForm.newDeviceId) {
    ElMessage.warning('请选择替换设备')
    return
  }
  try {
    replaceLoading.value = true
    await applyDeviceReplace(currentBooking.value.id, replaceForm)
    ElMessage.success('设备替换申请成功')
    replaceVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '替换申请失败')
  } finally {
    replaceLoading.value = false
  }
}

// 调整预约时间
const handleAdjust = (row) => {
  currentBooking.value = row
  adjustForm.newDate = dayjs(row.bookingDate).format('YYYY-MM-DD')
  adjustForm.newStartTime = row.startTime
  adjustForm.newEndTime = row.endTime
  adjustForm.reason = ''
  adjustVisible.value = true
}

const submitAdjust = async () => {
  if (!adjustForm.newDate) {
    ElMessage.warning('请选择新预约日期')
    return
  }
  if (!adjustForm.newStartTime || !adjustForm.newEndTime) {
    ElMessage.warning('请选择新时间段')
    return
  }
  const startH = parseInt(adjustForm.newStartTime.split(':')[0]) * 60 + parseInt(adjustForm.newStartTime.split(':')[1] || '0')
  const endH = parseInt(adjustForm.newEndTime.split(':')[0]) * 60 + parseInt(adjustForm.newEndTime.split(':')[1] || '0')
  if (endH <= startH) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  try {
    adjustLoading.value = true
    await studentAdjustBooking(currentBooking.value.id, adjustForm)
    ElMessage.success('预约时间已调整，重新提交审核')
    adjustVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '调整失败')
  } finally {
    adjustLoading.value = false
  }
}

onMounted(async () => {
  await loadBookingDataScope()
  await loadCancelDeadlineRule()
  loadData()
})
</script>

<style lang="scss" scoped>
.booking-list {
  width: 100%;
}

.search-area {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-select {
  width: 150px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.replace-hint {
  color: #8b949e;
  font-size: 12px;
  margin-left: 4px;
}
</style>
