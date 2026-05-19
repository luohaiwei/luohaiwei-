<template>
  <div class="device-list">
    <el-alert
      v-if="!canViewDeviceData"
      type="warning"
      :closable="false"
      show-icon
      class="device-scope-alert"
      title="当前角色在「权限分配」中未勾选设备数据的「查看设备信息」，无法浏览设备列表与智能推荐。如需查看请联系管理员调整数据权限。"
    />
    <!-- 有「查看设备信息」数据权限时才展示列表与推荐（与后端 /device/list 一致） -->
    <template v-if="canViewDeviceData">
    <!-- 智能推荐（学生/教师可见，默认展开） -->
    <el-collapse v-if="showRecommend" v-model="recommendActive" @change="handleRecommendChange">
      <el-collapse-item title="智能推荐 🔥" name="recommend">
        <div v-if="recommendLoading" class="recommend-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>正在加载推荐设备...</span>
        </div>
        <div v-if="recommendList.length > 0" class="recommend-list">
          <div v-for="item in recommendList" :key="item.device?.id" class="recommend-item">
            <span class="rec-name">{{ item.device?.deviceName }}</span>
            <el-tag size="small">{{ item.reason }}</el-tag>
            <el-button v-if="item.device?.status === 0" type="primary" size="small" @click="openBookingDialog(item.device)">预约</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无推荐设备（请确认已维护设备数据；有预约记录后将优先按偏好推荐）" />
      </el-collapse-item>
    </el-collapse>

    <!-- 搜索区域 -->
    <div class="search-area" style="flex-wrap: wrap;">
      <el-input
        v-model="searchForm.deviceName"
        placeholder="搜索设备名称"
        clearable
        class="search-input"
        @keyup.enter="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="searchForm.categoryId" placeholder="设备分类" clearable class="search-select">
        <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
      </el-select>
      <el-select v-model="searchForm.status" placeholder="设备状态" clearable class="search-select">
        <el-option label="空闲" :value="0" />
        <el-option label="使用中" :value="1" />
        <el-option label="维修中" :value="2" />
        <el-option label="校准中" :value="3" />
      </el-select>
      <el-select v-model="searchForm.laboratory" placeholder="所属实验室" clearable filterable class="search-select search-select-wide">
        <el-option v-for="lab in labOptions" :key="lab" :label="lab" :value="lab" />
      </el-select>
      <el-select v-model="searchForm.precisionLevel" placeholder="精度等级" clearable class="search-select">
        <el-option label="低" :value="1" />
        <el-option label="中" :value="2" />
        <el-option label="高" :value="3" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetForm">重置</el-button>
      <el-button v-if="canAddDevice" type="success" @click="openAddDevice">设备入库</el-button>
      <el-button v-if="canExportDevices" :loading="exportLoading" @click="handleExportDevices">
        <el-icon style="margin-right: 4px"><Download /></el-icon>导出设备
      </el-button>
    </div>

    <!-- 设备列表 -->
    <div class="device-grid">
      <div class="device-card" v-for="device in deviceList" :key="device.id">
        <div class="device-image">
          <img
            v-if="device.imagePath"
            class="device-thumb"
            :src="resolvePublicUpload(device.imagePath)"
            alt=""
          />
          <el-icon v-else :size="60"><Picture /></el-icon>
        </div>
        <div class="device-info">
          <div class="device-name">{{ device.deviceName }}</div>
          <div class="device-no">编号：{{ device.deviceNo }}</div>
          <div class="device-meta">
            <el-tag :type="getStatusType(device.status)" size="small">
              {{ getStatusText(device.status) }}
            </el-tag>
            <span class="device-location">{{ device.laboratory }}</span>
          </div>
        </div>
        <div class="device-actions">
          <el-button
            v-if="device.status === 0"
            type="primary"
            size="small"
            @click="openBookingDialog(device)"
          >预约</el-button>
          <el-button type="warning" size="small" @click="openRepairDialog(device)">报修</el-button>
          <el-button size="small" @click="goDetail(device)">详情</el-button>
          <template v-if="canEditDeviceRow || canChangeDeviceStatusRow || canDeleteDeviceRow">
            <el-button v-if="canEditDeviceRow" size="small" @click="openEditDevice(device)">编辑</el-button>
            <el-select
              v-if="canChangeDeviceStatusRow"
              v-model="device._editStatus"
              size="small"
              placeholder="状态"
              style="width: 90px"
              @change="(v) => updateStatus(device, v)"
            >
              <el-option label="空闲" :value="0" />
              <el-option label="使用中" :value="1" />
              <el-option label="维修中" :value="2" />
              <el-option label="校准中" :value="3" />
              <el-option label="报废" :value="4" />
            </el-select>
            <el-button v-if="canDeleteDeviceRow" size="small" type="danger" @click="handleDelete(device)">删除</el-button>
          </template>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        :total="pagination.total"
        layout="total, prev, pager, next, jumper"
        background
        @current-change="loadData"
      />
    </div>
    </template>

    <!-- 预约弹窗 -->
    <el-dialog v-model="bookingVisible" title="预约设备" width="500px" @close="resetBookingForm">
      <el-form ref="bookingFormRef" :model="bookingForm" :rules="bookingRules" label-width="100px">
        <el-form-item label="设备名称">
          <el-input :model-value="currentDevice?.deviceName" disabled />
        </el-form-item>
        <el-form-item label="预约日期" prop="bookingDate">
          <el-date-picker
            v-model="bookingForm.bookingDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :disabled-date="disabledPastDates"
            @change="loadRecommendedSlots"
          />
        </el-form-item>
        <!-- 推荐时段 -->
        <el-form-item v-if="recommendedSlots.length > 0" label="推荐时段">
          <div class="recommended-slots">
            <el-tag
              v-for="slot in recommendedSlots"
              :key="slot.startTime"
              :type="slot.available ? 'success' : 'info'"
              size="small"
              class="slot-tag"
              :effect="bookingForm.startTime === slot.startTime ? 'dark' : 'light'"
              @click="slot.available && selectSlot(slot)"
            >
              {{ slot.label }}
              <span v-if="!slot.available" style="font-size:10px">（已预约）</span>
              <span v-else style="font-size:10px">空闲</span>
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-select v-model="bookingForm.startTime" placeholder="选择" style="width: 100%">
            <el-option
              v-for="t in availableTimeSlots"
              :key="t.value"
              :label="t.label"
              :value="t.value"
              :disabled="isTimePassed(t.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-select v-model="bookingForm.endTime" placeholder="选择" style="width: 100%">
            <el-option
              v-for="t in availableTimeSlots"
              :key="t.value"
              :label="t.label"
              :value="t.value"
              :disabled="isTimePassed(t.value)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="参与人数" prop="participantCount">
          <el-input-number v-model="bookingForm.participantCount" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="实验项目" prop="experimentProject">
          <el-input v-model="bookingForm.experimentProject" placeholder="请输入实验项目名称" />
        </el-form-item>
        <el-form-item label="预约事由" prop="reason">
          <el-input v-model="bookingForm.reason" type="textarea" rows="3" placeholder="请输入预约事由" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bookingVisible = false">取消</el-button>
        <el-button type="primary" :loading="bookingLoading" @click="submitBooking">提交预约</el-button>
      </template>
    </el-dialog>

    <!-- 设备入库/编辑弹窗 -->
    <el-dialog v-model="deviceFormVisible" :title="deviceForm.id ? '编辑设备' : '设备入库'" width="650px" @close="resetDeviceForm">
      <el-form ref="deviceFormRef" :model="deviceForm" :rules="deviceFormRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备编号" prop="deviceNo">
              <el-input v-model="deviceForm.deviceNo" placeholder="例：MIC-010（字母/数字/连字符，勿含空格）" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备名称" prop="deviceName">
              <el-input v-model="deviceForm.deviceName" placeholder="设备显示名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="设备分类" prop="categoryId">
              <el-select v-model="deviceForm.categoryId" placeholder="选择分类" style="width: 100%">
                <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.categoryName" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备型号" prop="model">
              <el-input v-model="deviceForm.model" placeholder="型号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生产厂商" prop="manufacturer">
              <el-input v-model="deviceForm.manufacturer" placeholder="生产厂商" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="设备价格(元)" prop="price">
              <el-input-number v-model="deviceForm.price" :min="0" :precision="2" :controls="false" style="width: 100%" placeholder="设备价格" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="购买日期" prop="purchaseDate">
              <el-date-picker v-model="deviceForm.purchaseDate" type="date" placeholder="购买日期" value-format="YYYY-MM-DD" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="精度等级" prop="precisionLevel">
              <el-select v-model="deviceForm.precisionLevel" placeholder="精度等级" style="width: 100%">
                <el-option label="低" :value="1" />
                <el-option label="中" :value="2" />
                <el-option label="高" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所在实验室" prop="laboratory">
              <el-select v-model="deviceForm.laboratory" placeholder="选择实验室" filterable allow-create default-first-option style="width: 100%">
                <el-option v-for="lab in labOptions" :key="lab" :label="lab" :value="lab" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细位置" prop="location">
              <el-input v-model="deviceForm.location" placeholder="详细位置" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="deviceForm.status" placeholder="状态" style="width: 100%">
                <el-option label="空闲" :value="0" />
                <el-option label="使用中" :value="1" />
                <el-option label="维修中" :value="2" />
                <el-option label="校准中" :value="3" />
                <el-option label="报废" :value="4" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="校准周期(天)" prop="calibrationCycle">
              <el-input-number v-model="deviceForm.calibrationCycle" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="适配实验项目" prop="adaptProject">
          <el-input v-model="deviceForm.adaptProject" type="textarea" :rows="2" placeholder="适配的实验项目，多个用逗号分隔" />
        </el-form-item>
        <el-form-item label="设备图片">
          <div class="upload-optional-hint">选填；不上传时列表使用默认占位图，不影响保存</div>
          <div class="device-image-upload-row">
            <el-upload
              class="device-img-uploader"
              :show-file-list="false"
              :http-request="handleDeviceImageUpload"
              accept="image/jpeg,image/png,image/gif,image/webp"
            >
              <template v-if="deviceForm.imagePath">
                <img :src="deviceImagePreview" class="device-img-preview" alt="预览" />
              </template>
              <el-icon v-else class="device-img-placeholder"><Plus /></el-icon>
            </el-upload>
            <div class="device-upload-side">
              <div class="upload-hint">点击左侧上传，支持 jpg/png/gif/webp，最大 20MB，建议使用高清原图</div>
              <el-button v-if="deviceForm.imagePath" type="danger" link @click="deviceForm.imagePath = ''">清除图片</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="设备简介" prop="description">
          <el-input
            v-model="deviceForm.description"
            type="textarea"
            :rows="4"
            placeholder="设备简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deviceFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="deviceFormLoading" @click="submitDeviceForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 报修弹窗 -->
    <el-dialog v-model="repairVisible" title="故障报修" width="520px" @close="resetRepairForm">
      <el-form ref="repairFormRef" :model="repairForm" :rules="repairRules" label-width="100px">
        <el-form-item label="设备名称">
          <el-input :model-value="currentDevice?.deviceName" disabled />
        </el-form-item>
        <el-form-item label="故障描述" prop="faultDescription">
          <el-input v-model="repairForm.faultDescription" type="textarea" rows="4" placeholder="请详细描述故障现象" />
        </el-form-item>
        <el-form-item label="故障图片">
          <div class="device-image-upload-row">
            <el-upload
              class="device-img-uploader"
              :show-file-list="false"
              :http-request="handleRepairImageUpload"
              accept="image/jpeg,image/png,image/gif,image/webp"
            >
              <template v-if="repairForm.imagePath">
                <img :src="repairImagePreview" class="device-img-preview" alt="预览" />
              </template>
              <el-icon v-else class="device-img-placeholder"><Plus /></el-icon>
            </el-upload>
            <div class="device-upload-side">
              <div class="upload-hint">选填，与功能说明一致；支持 jpg/png/gif/webp，最大 20MB</div>
              <el-button v-if="repairForm.imagePath" type="danger" link @click="repairForm.imagePath = ''">清除图片</el-button>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="repairVisible = false">取消</el-button>
        <el-button type="primary" :loading="repairLoading" @click="submitRepair">提交报修</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getDeviceList, getDeviceDetail, addDevice, updateDevice, deleteDevice, updateDeviceStatus, getRecommendDevices, getRecommendTimeSlots, uploadDeviceImage, exportDeviceList } from '@/api/device'
import { getPermLabList, getMyDataScope } from '@/api/user'
import { getAllCategories } from '@/api/category'
import { createBooking, checkConflict, checkConflictDetail } from '@/api/booking'
import { createRepair, uploadRepairImage } from '@/api/repair'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Picture, Plus, Download, Loading } from '@element-plus/icons-vue'

const IMAGE_MAX_SIZE_MB = 20
const IMAGE_ACCEPT_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']

const validateImageFile = (file) => {
  const okType = IMAGE_ACCEPT_TYPES.includes(file.type)
  if (!okType) {
    ElMessage.warning('仅支持 jpg / jpeg / png / gif / webp 图片')
    return false
  }
  const okSize = file.size / 1024 / 1024 <= IMAGE_MAX_SIZE_MB
  if (!okSize) {
    ElMessage.warning(`图片大小不能超过 ${IMAGE_MAX_SIZE_MB}MB`)
    return false
  }
  return true
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
/** 后端 /permission/my-data-scope 合并后的设备数据权限，与权限分配页勾选一致 */
const myDataScope = ref({ deviceScope: [] })

const permList = computed(() => userStore.userInfo?.permissions || [])
const hasPerm = (code) => permList.value.includes(code)

const isSysAdmin = computed(() => userStore.userInfo?.userType === 'SYSTEM_ADMIN')
const isLabAdmin = computed(() => userStore.userInfo?.userType === 'LAB_ADMIN')

const fnDeviceAdd = computed(() => isSysAdmin.value || isLabAdmin.value || hasPerm('device:add'))
const fnDeviceEdit = computed(() => isSysAdmin.value || isLabAdmin.value || hasPerm('device:edit'))
const fnDeviceDelete = computed(() => isSysAdmin.value || isLabAdmin.value || hasPerm('device:delete'))

/** 系统管理员、维护员前端不挡数据权限（与后端特例一致） */
const bypassDeviceDataScopeUi = computed(() => {
  const t = userStore.userInfo?.userType
  return t === 'SYSTEM_ADMIN' || t === 'MAINTAINER'
})

const deviceDataFlags = computed(() => {
  if (bypassDeviceDataScopeUi.value) {
    return { view: true, edit: true, delete: true, status: true }
  }
  const arr = myDataScope.value?.deviceScope
  const s = new Set((Array.isArray(arr) ? arr : []).map((x) => String(x).toUpperCase()))
  return {
    view: s.has('VIEW'),
    edit: s.has('EDIT'),
    delete: s.has('DELETE'),
    status: s.has('STATUS')
  }
})

// 功能权限 + 数据权限同时满足（未勾选数据侧则隐藏按钮；后端仍会 403）
const canAddDevice = computed(() => fnDeviceAdd.value && deviceDataFlags.value.edit)
const canExportDevices = computed(() => fnDeviceEdit.value && deviceDataFlags.value.view)
const canEditDeviceRow = computed(() => fnDeviceEdit.value && deviceDataFlags.value.edit)
const canDeleteDeviceRow = computed(() => fnDeviceDelete.value && deviceDataFlags.value.delete)
const canChangeDeviceStatusRow = computed(() => fnDeviceEdit.value && deviceDataFlags.value.status)

/** 是否与后端设备列表一致：自定义角色无 VIEW 不可见；学生/教师/实验室管理员为预约默认可见（与后端 canBrowseReservableDevices 一致） */
const canViewDeviceData = computed(() => {
  if (bypassDeviceDataScopeUi.value) return true
  if (deviceDataFlags.value.view) return true
  const t = userStore.userInfo?.userType
  if (t === 'STUDENT' || t === 'TEACHER' || t === 'LAB_ADMIN') return true
  return false
})

const loadMyDataScope = async () => {
  try {
    const res = await getMyDataScope()
    myDataScope.value = res && typeof res === 'object' ? res : { deviceScope: [] }
  } catch (e) {
    console.error(e)
    myDataScope.value = { deviceScope: [] }
  }
}

/** 拼接可访问的上传资源地址（经 Vite 代理 /api） */
const resolvePublicUpload = (p) => {
  if (!p) return ''
  if (p.startsWith('http://') || p.startsWith('https://')) return p
  const s = p.startsWith('/') ? p : `/${p}`
  return `/api${s}`
}

const showRecommend = computed(() => {
  const t = userStore.userInfo?.userType
  if (t === 'STUDENT' || t === 'TEACHER' || t === 'LAB_ADMIN' || t === 'SYSTEM_ADMIN') return true
  if (userStore.isCustomRole) {
    const perms = userStore.userInfo?.permissions || []
    return perms.some((p) =>
      ['booking', 'booking-list', 'booking:add', 'device-list', 'device'].includes(p)
    )
  }
  return false
})

const recommendList = ref([])
const recommendActive = ref([])
const recommendLoading = ref(false)
const recommendLoaded = ref(false)

const searchForm = reactive({
  deviceName: '',
  categoryId: null,
  status: null,
  laboratory: '',
  precisionLevel: null
})

const labOptions = ref([])
const exportLoading = ref(false)

const pagination = reactive({
  pageNum: 1,
  pageSize: 8,
  total: 0
})

const deviceList = ref([])
const categoryList = ref([])

function buildBookingTimeSlots () {
  const slots = []
  for (let h = 6; h <= 22; h++) {
    for (const m of [0, 30]) {
      if (h === 22 && m === 30) continue
      const mm = m === 0 ? '00' : '30'
      const v = `${String(h).padStart(2, '0')}:${mm}`
      slots.push({ value: v, label: v })
    }
  }
  return slots
}
const timeSlots = buildBookingTimeSlots()

const recommendedSlots = ref([])

// 禁用过去的日期（只能选择今天及以后）
const disabledPastDates = (date) => {
  return date < new Date(new Date().setHours(0, 0, 0, 0))
}

// 判断是否为今天
const isToday = (dateStr) => {
  if (!dateStr) return false
  const today = new Date().toISOString().slice(0, 10)
  return dateStr === today
}

// 获取当前时间的小时分钟（格式 HH:MM）
const getCurrentTimeStr = () => {
  const now = new Date()
  const h = String(now.getHours()).padStart(2, '0')
  const m = String(now.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

// 判断某个时间点是否已过期（仅针对"今天"）
const isTimePassed = (timeStr) => {
  if (!isToday(bookingForm.bookingDate)) return false
  return timeStr <= getCurrentTimeStr()
}

// 过滤后可用时间列表（今天会移除已过期的时段）
const availableTimeSlots = computed(() => {
  return timeSlots.filter(slot => !isTimePassed(slot.value))
})

const loadRecommendedSlots = async () => {
  if (!bookingForm.bookingDate || !currentDevice.value?.id) {
    recommendedSlots.value = []
    return
  }
  // 选择日期时清空已选时段，避免跨日期遗留
  bookingForm.startTime = ''
  bookingForm.endTime = ''
  try {
    const res = await getRecommendTimeSlots(currentDevice.value.id, bookingForm.bookingDate)
    // 后端返回 { list: [...] }，勿把整个对象赋给 v-for
    recommendedSlots.value = Array.isArray(res) ? res : (res?.list || [])
  } catch (e) {
    console.error(e)
    recommendedSlots.value = []
  }
}

const selectSlot = (slot) => {
  // 今天不能选择已过期的时间段
  if (isTimePassed(slot.startTime)) {
    ElMessage.warning('该时段已过期，请选择其他时段')
    return
  }
  bookingForm.startTime = slot.startTime
  // 自动选择结束时间（下一个时段）
  const idx = timeSlots.findIndex(t => t.value === slot.startTime)
  if (idx >= 0 && idx + 1 < timeSlots.length) {
    const nextSlot = timeSlots[idx + 1]
    // 如果下一个时段也已过期，则结束时间与开始时间相同（用户需手动选择）
    if (!isTimePassed(nextSlot.value)) {
      bookingForm.endTime = nextSlot.value
    }
  }
}

const bookingVisible = ref(false)
const bookingLoading = ref(false)
const bookingFormRef = ref()
const currentDevice = ref(null)
const bookingForm = reactive({
  bookingDate: '',
  startTime: '',
  endTime: '',
  participantCount: 1,
  experimentProject: '',
  reason: ''
})
const bookingRules = {
  bookingDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  experimentProject: [{ required: true, message: '请输入实验项目', trigger: 'blur' }]
}

const parseErrMsg = (e, fallback = '操作失败') => {
  const d = e?.response?.data
  if (d && typeof d === 'object' && d.message) return String(d.message)
  if (typeof d === 'string' && d.trim()) return d
  return e?.message || fallback
}

const repairVisible = ref(false)
const repairLoading = ref(false)
const repairFormRef = ref()
const repairForm = reactive({ faultDescription: '', imagePath: '' })
const repairImagePreview = computed(() => resolvePublicUpload(repairForm.imagePath))
const repairRules = {
  faultDescription: [{ required: true, message: '请描述故障现象', trigger: 'blur' }]
}

const deviceFormVisible = ref(false)
const deviceFormLoading = ref(false)
const deviceFormRef = ref()
const deviceForm = reactive({
  id: null,
  deviceNo: '',
  deviceName: '',
  categoryId: null,
  model: '',
  manufacturer: '',
  price: null,
  purchaseDate: '',
  precisionLevel: 2,
  laboratory: '',
  location: '',
  status: 0,
  calibrationCycle: 365,
  adaptProject: '',
  imagePath: '',
  description: ''
})

const deviceImagePreview = computed(() => resolvePublicUpload(deviceForm.imagePath))

const handleRepairImageUpload = async (options) => {
  const { file, onSuccess, onError } = options
  if (!validateImageFile(file)) {
    onError(new Error('图片格式或大小不符合要求'))
    return
  }
  try {
    const res = await uploadRepairImage(file)
    if (res.path) {
      repairForm.imagePath = res.path
      onSuccess(res)
      ElMessage.success('图片上传成功')
    } else {
      const msg = res.message || '上传失败'
      ElMessage.error(msg)
      onError(new Error(msg))
    }
  } catch (e) {
    const msg = e.message || '上传失败，请稍后重试'
    ElMessage.error(msg)
    onError(e)
  }
}

const handleDeviceImageUpload = async (options) => {
  const { file, onSuccess, onError } = options
  if (!validateImageFile(file)) {
    onError(new Error('图片格式或大小不符合要求'))
    return
  }
  try {
    const res = await uploadDeviceImage(file)
    if (res.path) {
      deviceForm.imagePath = res.path
      onSuccess(res)
      ElMessage.success('图片上传成功')
    } else {
      const msg = res.message || '上传失败'
      ElMessage.error(msg)
      onError(new Error(msg))
    }
  } catch (e) {
    const msg = e.message || '上传失败，请稍后重试'
    ElMessage.error(msg)
    onError(e)
  }
}
const deviceFormRules = {
  deviceNo: [
    { required: true, message: '请输入设备编号', trigger: 'blur' },
    { min: 2, max: 50, message: '编号长度为 2～50 个字符', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._-]+$/, message: '编号仅允许字母、数字、点、下划线、连字符', trigger: 'blur' }
  ],
  deviceName: [{ required: true, message: '请输入设备名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择设备分类', trigger: 'change' }],
  laboratory: [{ required: true, message: '请选择或输入所在实验室', trigger: 'change' }],
  precisionLevel: [{ required: true, message: '请选择精度等级', trigger: 'change' }],
  status: [{ required: true, message: '请选择设备状态', trigger: 'change' }]
}

const loadData = async () => {
  if (!canViewDeviceData.value) {
    deviceList.value = []
    pagination.total = 0
    return
  }
  try {
    const res = await getDeviceList({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      ...searchForm
    })
    deviceList.value = (res.list || []).map(d => ({ ...d, _editStatus: d.status }))
    pagination.total = res.total
  } catch (e) {
    console.error(e)
  }
}

const loadCategories = async () => {
  try {
    const list = await getAllCategories()
    categoryList.value = list || []
  } catch (e) {
    console.error(e)
  }
}

const loadLabOptions = async () => {
  try {
    const rows = await getPermLabList()
    const names = (rows || []).map(r => r.labName).filter(Boolean)
    labOptions.value = [...new Set(names.map(String))]
  } catch {
    // /permission/lab-list 仅 LAB_ADMIN 可访问，学生/教师等无权限，忽略错误避免 403 中断设备列表加载
    labOptions.value = []
  }
}

const loadRecommend = async () => {
  if (!showRecommend.value || !canViewDeviceData.value) {
    recommendList.value = []
    recommendLoaded.value = false
    return
  }
  recommendLoading.value = true
  try {
    const res = await getRecommendDevices(5)
    recommendList.value = res?.list || []
    recommendLoaded.value = true
  } catch (e) {
    console.error(e)
    recommendList.value = []
    recommendLoaded.value = true
  } finally {
    recommendLoading.value = false
  }
}

const handleRecommendChange = async (activeNames) => {
  const names = Array.isArray(activeNames) ? activeNames : [activeNames]
  const opened = names.includes('recommend')
  if (opened && !recommendLoaded.value) {
    await loadRecommend()
  }
}

const resetForm = () => {
  searchForm.deviceName = ''
  searchForm.categoryId = null
  searchForm.status = null
  searchForm.laboratory = ''
  searchForm.precisionLevel = null
  pagination.pageNum = 1
  loadData()
}

const handleExportDevices = async () => {
  exportLoading.value = true
  try {
    const params = {
      deviceName: searchForm.deviceName || undefined,
      categoryId: searchForm.categoryId ?? undefined,
      status: searchForm.status ?? undefined,
      laboratory: searchForm.laboratory || undefined,
      precisionLevel: searchForm.precisionLevel ?? undefined
    }
    const raw = await exportDeviceList(params)
    const blob =
      raw instanceof Blob
        ? raw
        : new Blob([raw], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    if (blob.type && blob.type.includes('application/json')) {
      const text = await blob.text()
      const j = JSON.parse(text)
      throw new Error(j.message || '导出失败')
    }
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `设备列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    let msg = e.message || '导出失败'
    const data = e.response?.data
    if (data instanceof Blob) {
      try {
        const text = await data.text()
        const j = JSON.parse(text)
        if (j.message) msg = j.message
      } catch (_) {
        /* ignore */
      }
    }
    ElMessage.error(msg)
  } finally {
    exportLoading.value = false
  }
}

const getStatusType = (status) => {
  const types = { 0: 'success', 1: 'primary', 2: 'warning', 3: 'info', 4: 'danger' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '空闲', 1: '使用中', 2: '维修中', 3: '校准中', 4: '报废' }
  return texts[status] || '未知'
}

const openBookingDialog = (device) => {
  currentDevice.value = device
  bookingVisible.value = true
}

const resetBookingForm = () => {
  bookingForm.bookingDate = ''
  bookingForm.startTime = ''
  bookingForm.endTime = ''
  bookingForm.participantCount = 1
  bookingForm.experimentProject = ''
  bookingForm.reason = ''
  recommendedSlots.value = []
}

const submitBooking = async () => {
  if (!bookingFormRef.value) return
  await bookingFormRef.value.validate(async (valid) => {
    if (!valid) return
    if (!currentDevice.value) return
    const startH = parseInt(bookingForm.startTime.split(':')[0]) * 60 + parseInt(bookingForm.startTime.split(':')[1] || 0)
    const endH = parseInt(bookingForm.endTime.split(':')[0]) * 60 + parseInt(bookingForm.endTime.split(':')[1] || 0)
    if (endH <= startH) {
      ElMessage.warning('结束时间必须晚于开始时间')
      return
    }
    const durationHours = (endH - startH) / 60
    bookingLoading.value = true
    try {
      const bookingDate = bookingForm.bookingDate + 'T00:00:00'
      const res = await createBooking({
        deviceId: currentDevice.value.id,
        bookingDate,
        startTime: bookingForm.startTime,
        endTime: bookingForm.endTime,
        duration: durationHours,
        participantCount: bookingForm.participantCount,
        experimentProject: bookingForm.experimentProject,
        reason: bookingForm.reason
      })
      if (res && res.status === 0) {
        ElMessage.success('预约已提交，学生预约需教师或管理员审核，请等待人工审批')
      } else {
        ElMessage.success('预约成功，系统已自动通过')
      }
      bookingVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error(parseErrMsg(e, '预约失败'))
    } finally {
      bookingLoading.value = false
    }
  })
}

const openRepairDialog = (device) => {
  currentDevice.value = device
  repairForm.faultDescription = ''
  repairForm.imagePath = ''
  repairVisible.value = true
}

const resetRepairForm = () => {
  repairForm.faultDescription = ''
  repairForm.imagePath = ''
}

const submitRepair = async () => {
  if (!repairFormRef.value) return
  await repairFormRef.value.validate(async (valid) => {
    if (!valid) return
    if (!currentDevice.value) return
    repairLoading.value = true
    try {
      await createRepair({
        deviceId: currentDevice.value.id,
        faultDescription: repairForm.faultDescription,
        imagePath: repairForm.imagePath || undefined
      })
      ElMessage.success('报修已提交')
      repairVisible.value = false
      loadData()
    } catch (e) {
      ElMessage.error(e.message || '报修失败')
    } finally {
      repairLoading.value = false
    }
  })
}

const goDetail = (device) => {
  router.push(`/device/${device.id}`)
}

const openAddDevice = () => {
  Object.assign(deviceForm, {
    id: null,
    deviceNo: '',
    deviceName: '',
    categoryId: null,
    model: '',
    manufacturer: '',
    price: null,
    purchaseDate: '',
    precisionLevel: 2,
    laboratory: '',
    location: '',
    status: 0,
    calibrationCycle: 365,
    adaptProject: '',
    imagePath: '',
    description: ''
  })
  deviceFormVisible.value = true
}

const openEditDevice = (device) => {
  Object.assign(deviceForm, {
    id: device.id,
    deviceNo: device.deviceNo,
    deviceName: device.deviceName,
    categoryId: device.categoryId,
    model: device.model || '',
    manufacturer: device.manufacturer || '',
    price: device.price ?? null,
    purchaseDate: device.purchaseDate ? String(device.purchaseDate).substring(0, 10) : '',
    precisionLevel: device.precisionLevel ?? 2,
    laboratory: device.laboratory || '',
    location: device.location || '',
    status: device.status ?? 0,
    calibrationCycle: device.calibrationCycle ?? 365,
    adaptProject: device.adaptProject || '',
    imagePath: device.imagePath || '',
    description: device.description || ''
  })
  deviceFormVisible.value = true
}

const resetDeviceForm = () => {
  deviceForm.id = null
  deviceForm.deviceNo = ''
  deviceForm.deviceName = ''
  deviceForm.categoryId = null
  deviceForm.model = ''
  deviceForm.manufacturer = ''
  deviceForm.price = null
  deviceForm.purchaseDate = ''
  deviceForm.precisionLevel = 2
  deviceForm.laboratory = ''
  deviceForm.location = ''
  deviceForm.status = 0
  deviceForm.calibrationCycle = 365
  deviceForm.adaptProject = ''
  deviceForm.imagePath = ''
  deviceForm.description = ''
}

const buildDevicePayload = () => {
  const cid = deviceForm.categoryId
  const categoryId =
    cid === '' || cid === undefined || cid === null ? null : Number(cid)
  const p = {
    deviceNo: String(deviceForm.deviceNo || '').trim(),
    deviceName: String(deviceForm.deviceName || '').trim(),
    categoryId: Number.isFinite(categoryId) ? categoryId : null,
    model: deviceForm.model ? String(deviceForm.model).trim() : null,
    manufacturer: deviceForm.manufacturer ? String(deviceForm.manufacturer).trim() : null,
    price: deviceForm.price != null && deviceForm.price !== '' ? Number(deviceForm.price) : null,
    purchaseDate: deviceForm.purchaseDate ? String(deviceForm.purchaseDate).trim() : null,
    precisionLevel:
      deviceForm.precisionLevel != null ? Number(deviceForm.precisionLevel) : 2,
    laboratory: deviceForm.laboratory ? String(deviceForm.laboratory).trim() : null,
    location: deviceForm.location ? String(deviceForm.location).trim() : null,
    status: deviceForm.status != null ? Number(deviceForm.status) : 0,
    calibrationCycle:
      deviceForm.calibrationCycle != null ? Number(deviceForm.calibrationCycle) : 180,
    adaptProject: deviceForm.adaptProject ? String(deviceForm.adaptProject).trim() : null,
    imagePath: deviceForm.imagePath ? String(deviceForm.imagePath).trim() : null,
    description: deviceForm.description ? String(deviceForm.description).trim() : null
  }
  if (deviceForm.id) {
    p.id = Number(deviceForm.id)
  }
  return p
}

const submitDeviceForm = async () => {
  if (!deviceFormRef.value) return
  await deviceFormRef.value.validate(async (valid) => {
    if (!valid) return
    deviceFormLoading.value = true
    try {
      const payload = buildDevicePayload()
      if (deviceForm.id) {
        await updateDevice(payload.id, payload)
        ElMessage.success('更新成功')
      } else {
        await addDevice(payload)
        ElMessage.success('添加成功')
      }
      deviceFormVisible.value = false
      loadData()
      loadLabOptions()
    } catch (e) {
      let errorMsg = '操作失败，请稍后重试'
      if (e?.response?.data) {
        const data = e.response.data
        if (typeof data === 'string') {
          errorMsg = data.trim()
        } else if (typeof data === 'object' && data !== null) {
          errorMsg = data.message || data.msg || data.error || JSON.stringify(data)
        }
      } else if (e?.message && e.message !== 'Request failed with status code 400') {
        errorMsg = e.message
      }
      ElMessage.error({ message: errorMsg, duration: 5000, showClose: true })
    } finally {
      deviceFormLoading.value = false
    }
  })
}

const handleDelete = async (device) => {
  try {
    await ElMessageBox.confirm(`确定删除设备「${device.deviceName}」？`, '确认删除', {
      type: 'warning'
    })
    await deleteDevice(device.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const updateStatus = async (device, status) => {
  try {
    await updateDeviceStatus(device.id, status)
    ElMessage.success('状态已更新')
    device._editStatus = status
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '更新失败')
  }
}

// 从设备详情页跳转回来时，根据 query 自动打开预约/报修
watch(() => route.query, async (q) => {
  if (!q) return
  const bookId = q.book ? Number(q.book) : null
  const repairId = q.repair ? Number(q.repair) : null
  const getDevice = async (id) => {
    const d = deviceList.value.find(x => x.id === id)
    if (d) return d
    try {
      return await getDeviceDetail(id)
    } catch { return null }
  }
  if (bookId) {
    const d = await getDevice(bookId)
    if (d) {
      currentDevice.value = d
      bookingVisible.value = true
    }
  }
  if (repairId) {
    const d = await getDevice(repairId)
    if (d) {
      currentDevice.value = d
      repairForm.faultDescription = ''
      repairForm.imagePath = ''
      repairVisible.value = true
    }
  }
}, { immediate: true })

onMounted(async () => {
  await loadMyDataScope()
  loadData()
  loadCategories()
  loadLabOptions()
})
</script>

<style lang="scss" scoped>
.device-list {
  width: 100%;
}

.device-scope-alert {
  margin-bottom: 16px;
}

.upload-optional-hint {
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 8px;
  line-height: 1.4;
}

.search-area {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-input {
  width: 300px;
}

.search-select {
  width: 150px;
}

.search-select-wide {
  width: 180px;
}

.device-grid {
  display: grid;
  gap: 20px;
  margin-bottom: 24px;
}

/* 响应式布局 - 确保8个卡片刚好填满 */
@media (min-width: 1920px) {
  .device-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (min-width: 1440px) and (max-width: 1919px) {
  .device-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (min-width: 1024px) and (max-width: 1439px) {
  .device-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 1023px) {
  .device-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.device-card {
  background: #161B22;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363D;
  transition: all 0.3s ease;
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;

  &:hover {
    border-color: #00D4FF;
    box-shadow: 0 0 20px rgba(0, 212, 255, 0.15);
    transform: translateY(-2px);
  }
}

.device-image {
  flex: 1;
  min-height: 140px;
  background: #21262D;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #484F58;
  margin-bottom: 16px;
  overflow: hidden;
}

.device-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  image-rendering: auto;
  transform: translateZ(0);
  backface-visibility: hidden;
}

.device-image-upload-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.device-img-uploader {
  :deep(.el-upload) {
    border: 1px dashed #30363d;
    border-radius: 8px;
    cursor: pointer;
    width: 120px;
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #161b22;
    overflow: hidden;
    transition: border-color 0.2s;
  }
  :deep(.el-upload:hover) {
    border-color: #00d4ff;
  }
}

.device-img-preview {
  width: 120px;
  height: 120px;
  object-fit: contain;
  object-position: center;
  background: #0d1117;
  display: block;
}

.device-img-placeholder {
  font-size: 36px;
  color: #8b949e;
}

.device-upload-side {
  flex: 1;
  min-width: 160px;
}

.upload-hint {
  font-size: 12px;
  color: #8b949e;
  line-height: 1.5;
  margin-bottom: 8px;
}

.device-info {
  margin-bottom: 16px;
}

.device-name {
  font-size: 16px;
  font-weight: 600;
  color: #E6EDF3;
  margin-bottom: 8px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-no {
  font-size: 12px;
  color: #8B949E;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.device-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.device-location {
  font-size: 12px;
  color: #8B949E;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.device-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
  justify-content: flex-start;
}

.device-actions .el-button {
  flex: none;
}

.device-actions .el-select {
  flex: none;
}

.recommend-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.recommend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #1A1F35;
  border-radius: 6px;
  border: 1px solid #30363D;
}
.recommend-item .rec-name {
  color: #E6EDF3;
  font-weight: 500;
}
.recommend-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #8B949E;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}

/* 智能推荐深色主题样式 */
:deep(.el-collapse) {
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 8px;
  margin-bottom: 20px;
}

:deep(.el-collapse-item__header) {
  background: #21262D;
  color: #E6EDF3;
  border-bottom-color: #30363D;
  padding: 12px 16px;
  font-weight: 600;
}

:deep(.el-collapse-item__arrow) {
  color: #8B949E;
}

:deep(.el-collapse-item__content) {
  background: #161B22;
  color: #E6EDF3;
  padding: 16px;
}

:deep(.el-collapse-item__wrap) {
  background: #161B22;
  border-bottom-color: #30363D;
}

/* 搜索区域输入框深色主题 */
:deep(.el-input__wrapper) {
  background: #161B22 !important;
  box-shadow: 0 0 0 1px #30363D inset !important;
}

:deep(.el-input__inner) {
  color: #E6EDF3 !important;
  background: transparent !important;
}

:deep(.el-input__inner::placeholder) {
  color: #8B949E !important;
}

:deep(.el-input__prefix-inner) {
  color: #8B949E !important;
}

/* 选择器深色主题 */
:deep(.el-select .el-input__wrapper) {
  background: #161B22 !important;
  box-shadow: 0 0 0 1px #30363D inset !important;
}

:deep(.el-select .el-input__inner) {
  color: #E6EDF3 !important;
}

:deep(.el-select .el-select__placeholder) {
  color: #8B949E !important;
}

/* 下拉菜单深色主题 */
:deep(.el-select-dropdown) {
  background: #161B22 !important;
  border: 1px solid #30363D !important;
}

:deep(.el-select-dropdown__item) {
  color: #E6EDF3 !important;
}

:deep(.el-select-dropdown__item.hover) {
  background: #21262D !important;
}

:deep(.el-select-dropdown__item.selected) {
  background: #1A1F35 !important;
  color: #00D4FF !important;
}

/* el-empty 深色主题 */
:deep(.el-empty__description) {
  color: #8B949E !important;
}

/* 设备状态选择器 */
:deep(.el-select .el-select__tags) {
  background: transparent !important;
}

:deep(.el-select .el-select__tags .el-tag) {
  background: #21262D !important;
  border-color: #30363D !important;
  color: #E6EDF3 !important;
}

/* 推荐时段样式 */
.recommended-slots {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.slot-tag {
  cursor: pointer;
  user-select: none;
}
</style>
