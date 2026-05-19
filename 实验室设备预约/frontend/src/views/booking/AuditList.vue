<template>
  <div class="audit-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>{{ isTeacher ? '学生预约审核' : '预约审核' }}</h2>
      <div class="header-actions">
        <el-button
          v-if="canBookingAudit && selectedRows.length > 0"
          type="success"
          :loading="batchLoading"
          @click="handleBatchAudit(1)"
        >
          批量通过({{ selectedRows.length }})
        </el-button>
        <el-button
          v-if="canBookingAudit && selectedRows.length > 0"
          type="danger"
          :loading="batchLoading"
          @click="handleBatchAudit(2)"
        >
          批量拒绝({{ selectedRows.length }})
        </el-button>
      </div>
    </div>

    <!-- 搜索筛选区 -->
    <el-alert
      v-if="scopeLoaded && !canBookingAudit"
      type="warning"
      :closable="false"
      show-icon
      class="audit-scope-alert"
      title="当前角色未勾选「审核预约申请」数据权限，仅可查看列表，无法进行通过/拒绝、批量审核与调度操作。"
    />
    <div class="search-bar">
      <el-select
        v-model="searchForm.status"
        placeholder="全部状态"
        clearable
        style="width: 140px"
        @change="resetAndLoad"
      >
        <el-option label="全部状态" :value="null" />
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
        <el-option label="已拒绝" :value="2" />
        <el-option label="已完成" :value="3" />
        <el-option label="已取消" :value="4" />
      </el-select>
      <el-input
        v-model="searchForm.keyword"
        placeholder="设备名称 / 申请人 / 预约单号"
        clearable
        style="width: 260px"
        @keyup.enter="resetAndLoad"
      />
      <el-button type="primary" @click="resetAndLoad">搜索</el-button>
      <el-button @click="resetSearch">重置</el-button>
      <el-button
        v-if="canBookingExport"
        type="success"
        plain
        :loading="exportLoading"
        @click="handleExportBookings"
      >
        <el-icon><Download /></el-icon>
        导出预约数据
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card" :class="{ active: searchForm.status === 0 }" @click="setStatus(0)">
        <div class="stat-num" style="color: #ff9500">{{ stats.pending }}</div>
        <div class="stat-label">待审核</div>
      </div>
      <div class="stat-card" :class="{ active: searchForm.status === 1 }" @click="setStatus(1)">
        <div class="stat-num" style="color: #00ff88">{{ stats.approved }}</div>
        <div class="stat-label">已通过</div>
      </div>
      <div class="stat-card" :class="{ active: searchForm.status === 2 }" @click="setStatus(2)">
        <div class="stat-num" style="color: #ff4757">{{ stats.rejected }}</div>
        <div class="stat-label">已拒绝</div>
      </div>
      <div class="stat-card" :class="{ active: searchForm.status === 3 }" @click="setStatus(3)">
        <div class="stat-num" style="color: #00d4ff">{{ stats.completed }}</div>
        <div class="stat-label">已完成</div>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table
      :data="tableData"
      border
      stripe
      @selection-change="onSelectionChange"
      class="audit-table"
    >
      <el-table-column
        v-if="canBookingAudit"
        type="selection"
        width="50"
        :selectable="row => row.status === 0"
      />
      <el-table-column prop="orderNo" label="预约单号" width="140" />
      <el-table-column label="设备名称" min-width="150">
        <template #default="{ row }">
          <span>{{ row.deviceName || '-' }}</span>
          <span v-if="row.replaceDeviceName" class="replace-hint"
            >（拟换：{{ row.replaceDeviceName }}）</span
          >
        </template>
      </el-table-column>
      <el-table-column label="申请人" width="100">
        <template #default="{ row }">{{ row.userName || '-' }}</template>
      </el-table-column>
      <el-table-column label="预约日期" width="110">
        <template #default="{ row }">{{ formatBookingDateOnly(row.bookingDate) || '-' }}</template>
      </el-table-column>
      <el-table-column label="时段" width="160">
        <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
      </el-table-column>
      <el-table-column
        prop="experimentProject"
        label="实验项目"
        min-width="120"
        show-overflow-tooltip
      />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="getStatusType(row.status)">{{
            getStatusText(row.status)
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="400" fixed="right" align="left">
        <template #default="{ row }">
          <div class="audit-ops">
            <!-- 待审核：教师仅可审核通过/拒绝；调度类（调整/替换）仅实验室管理员等 -->
            <template v-if="row.status === 0">
              <el-button
                v-if="canBookingAudit"
                size="small"
                type="success"
                @click="handleAudit(row, 1)"
                >通过</el-button
              >
              <el-button
                v-if="canBookingAudit"
                size="small"
                type="danger"
                plain
                @click="handleAudit(row, 2)"
                >拒绝</el-button
              >
              <template v-if="canBookingAudit && !isTeacher">
                <el-button size="small" type="warning" plain @click="openAdjust(row)">调整</el-button>
                <el-button size="small" type="primary" plain @click="openReplace(row)">替换</el-button>
              </template>
              <el-button
                v-if="canBookingCancel && !isTeacher"
                size="small"
                type="danger"
                plain
                @click="openCancel(row)"
                >取消预约</el-button
              >
              <el-button size="small" type="info" plain @click="handleView(row)">详情</el-button>
            </template>
            <!-- 已通过操作（调度管理） -->
            <template v-else-if="row.status === 1 && !isTeacher">
              <template v-if="canBookingAudit">
                <el-button size="small" type="warning" plain @click="openAdjust(row)">调整</el-button>
                <el-button size="small" type="primary" plain @click="openReplace(row)">替换</el-button>
              </template>
              <el-button
                v-if="canBookingCancel"
                size="small"
                type="danger"
                plain
                @click="openCancel(row)"
                >取消预约</el-button
              >
              <el-button size="small" type="info" plain @click="handleView(row)">详情</el-button>
            </template>
            <!-- 其他状态 -->
            <el-button v-else size="small" @click="handleView(row)">详情</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @current-change="fetchData"
      @size-change="fetchData"
    />

    <!-- 预约详情弹窗 -->
    <el-dialog v-model="detailVisible" title="预约详情" width="580px">
      <el-descriptions v-if="detailData" :column="1" border>
        <el-descriptions-item label="预约单号">{{ detailData.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="设备名称">{{ detailData.deviceName }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.replaceDeviceName" label="拟替换为">{{
          detailData.replaceDeviceName
        }}</el-descriptions-item>
        <el-descriptions-item v-if="detailData.replaceReason" label="替换原因">{{
          detailData.replaceReason
        }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detailData.userName }}</el-descriptions-item>
        <el-descriptions-item label="预约日期">{{ detailData.bookingDate }}</el-descriptions-item>
        <el-descriptions-item label="使用时段"
          >{{ detailData.startTime }} - {{ detailData.endTime }}</el-descriptions-item
        >
        <el-descriptions-item label="参与人数">{{
          detailData.participantCount
        }}</el-descriptions-item>
        <el-descriptions-item label="实验项目">{{
          detailData.experimentProject || '-'
        }}</el-descriptions-item>
        <el-descriptions-item label="预约事由">{{ detailData.reason || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag size="small" :type="getStatusType(detailData.status)">{{
            getStatusText(detailData.status)
          }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailData.auditOpinion" label="审核意见">{{
          detailData.auditOpinion
        }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditDialogVisible" title="审核预约" width="500px">
      <el-form :model="auditForm" label-width="100px">
        <el-form-item label="审核结论">
          <el-radio-group v-model="auditForm.status">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">拒绝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核意见">
          <el-input
            v-model="auditForm.auditOpinion"
            type="textarea"
            rows="4"
            :placeholder="auditForm.status === 2 ? '拒绝必填' : '选填'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAuditSubmit"
          >确定</el-button
        >
      </template>
    </el-dialog>

    <!-- 调整预约弹窗 -->
    <el-dialog v-model="adjustDialogVisible" title="调整预约" width="540px">
      <el-form :model="adjustForm" label-width="110px">
        <el-form-item label="原设备">{{ adjustForm.deviceName || '—' }}</el-form-item>
        <el-form-item label="原时段"
          >{{ formatBookingDateOnly(adjustForm.bookingDate) }} {{ adjustForm.startTime }} -
          {{ adjustForm.endTime }}</el-form-item
        >
        <el-form-item label="新预约日期">
          <el-date-picker
            v-model="adjustForm.newDate"
            type="date"
            placeholder="选择日期"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="新开始时间">
          <el-time-select
            v-model="adjustForm.newStartTime"
            start="06:00"
            step="00:30"
            end="23:30"
            style="width: 100%"
            placeholder="开始时间"
          />
        </el-form-item>
        <el-form-item label="新结束时间">
          <el-time-select
            v-model="adjustForm.newEndTime"
            start="06:00"
            step="00:30"
            end="23:30"
            style="width: 100%"
            placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="adjustForm.reason"
            type="textarea"
            rows="3"
            placeholder="必填，说明调整原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdjustSubmit"
          >确定调整</el-button
        >
      </template>
    </el-dialog>

    <!-- 取消预约弹窗 -->
    <el-dialog v-model="cancelDialogVisible" title="取消预约" width="480px">
      <el-form :model="cancelForm" label-width="100px">
        <el-form-item label="预约信息">
          {{ cancelForm.deviceName }} {{ cancelForm.bookingDate }} {{ cancelForm.startTime }} -
          {{ cancelForm.endTime }}
        </el-form-item>
        <el-form-item label="取消原因">
          <el-input
            v-model="cancelForm.reason"
            type="textarea"
            rows="3"
            placeholder="必填，说明取消原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitLoading" @click="handleCancelSubmit"
          >确认取消</el-button
        >
      </template>
    </el-dialog>

    <el-dialog v-model="replaceDialogVisible" title="设备替换" width="520px">
      <el-form :model="replaceForm" label-width="110px">
        <el-form-item label="原设备">
          {{ replaceForm.currentDeviceName }}
        </el-form-item>
        <el-form-item label="可替换设备">
          <el-select
            v-model="replaceForm.newDeviceId"
            placeholder="请选择可替换设备"
            style="width: 100%"
          >
            <el-option
              v-for="d in replaceDeviceOptions"
              :key="d.id"
              :label="`${d.deviceName}（${d.deviceNo || '无编号'}）`"
              :value="d.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="替换原因">
          <el-input
            v-model="replaceForm.reason"
            type="textarea"
            rows="3"
            placeholder="请填写设备替换原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replaceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleReplaceSubmit"
          >确定替换</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  auditBooking,
  getBookingDetail,
  getBookingList,
  adjustBooking,
  adminCancelBooking,
  batchAuditBooking,
  applyDeviceReplace,
  getReplaceableDevices,
  exportBookingList
} from '@/api/booking';
import { getMyDataScope } from '@/api/user';
import { useUserStore } from '@/stores/user';
import { Download } from '@element-plus/icons-vue';

const userStore = useUserStore();
const isTeacher = computed(() => userStore.userInfo?.userType === 'TEACHER');

/** 与「权限分配」右侧「预约数据可见性」勾选一致（合并后的有效权限） */
const bookingScope = ref([]);
const scopeLoaded = ref(false);
const scopeHasFlag = flag =>
  Array.isArray(bookingScope.value) &&
  bookingScope.value.some(s => String(s).trim().toUpperCase() === String(flag).toUpperCase());

const canBookingAudit = computed(() => scopeHasFlag('AUDIT') || isTeacher.value);
const canBookingCancel = computed(() => scopeHasFlag('CANCEL'));
/** 数据权限「导出预约数据」或系统管理员 */
const canBookingExport = computed(() => {
  if (userStore.userInfo?.userType === 'SYSTEM_ADMIN') return true;
  return scopeHasFlag('EXPORT');
});

const exportLoading = ref(false);

const loadBookingDataScope = async () => {
  try {
    const res = await getMyDataScope();
    bookingScope.value = res?.bookingScope || [];
  } catch (e) {
    console.error(e);
    bookingScope.value = [];
  } finally {
    scopeLoaded.value = true;
  }
};

// ========== 数据 ==========
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const selectedRows = ref([]);
const batchLoading = ref(false);
const submitLoading = ref(false);

const stats = reactive({ pending: 0, approved: 0, rejected: 0, completed: 0 });

const searchForm = reactive({
  status: null,
  keyword: ''
});

// ========== 详情 ==========
const detailVisible = ref(false);
const detailData = ref(null);

// ========== 审核 ==========
const auditDialogVisible = ref(false);
const auditForm = reactive({ status: 1, auditOpinion: '' });
const auditCurrentRow = ref(null);

// ========== 调整 ==========
const adjustDialogVisible = ref(false);
const adjustForm = reactive({
  id: null,
  deviceName: '',
  bookingDate: '',
  startTime: '',
  endTime: '',
  newDate: '',
  newStartTime: '',
  newEndTime: '',
  reason: ''
});

// ========== 取消 ==========
const cancelDialogVisible = ref(false);
const cancelForm = reactive({
  id: null,
  deviceName: '',
  bookingDate: '',
  startTime: '',
  endTime: '',
  reason: ''
});

const replaceDialogVisible = ref(false);
const replaceDeviceOptions = ref([]);
const replaceForm = reactive({
  id: null,
  currentDeviceName: '',
  newDeviceId: null,
  reason: ''
});

// ========== 工具 ==========
/** 后端 bookingDate 常为 ISO 字符串（如 2026-03-29T00:00:00），统一为 yyyy-MM-dd 供展示与提交 */
const formatBookingDateOnly = v => {
  if (v == null || v === '') return ''
  const s = String(v).trim()
  const m = s.match(/^(\d{4}-\d{2}-\d{2})/)
  return m ? m[1] : ''
}

const getStatusType = s =>
  ({ 0: 'warning', 1: 'success', 2: 'danger', 3: 'info', 4: 'info', 5: 'primary' })[s] || 'info';
const getStatusText = s =>
  ({ 0: '待审核', 1: '已通过', 2: '已拒绝', 3: '已完成', 4: '已取消', 5: '已签到' })[s] || '未知';

const resetSearch = () => {
  searchForm.status = null;
  searchForm.keyword = '';
  resetAndLoad();
};

const resetAndLoad = () => {
  pageNum.value = 1;
  fetchData();
};

const setStatus = val => {
  searchForm.status = val;
  resetAndLoad();
};

const onSelectionChange = rows => {
  selectedRows.value = rows;
};

// ========== 数据加载 ==========
const fetchData = async () => {
  try {
    const res = await getBookingList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchForm
    });
    tableData.value = res.list || [];
    total.value = res.total || 0;

    // 加载统计数据
    if (searchForm.status === 0) {
      const allRes = await getBookingList({ pageNum: 1, pageSize: 1 });
      // 简单统计：如果当前只显示待审核，则分别拉取各状态数量（实际应后端提供，这里前端计算）
    }
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败');
  }
};

const fetchStats = async () => {
  try {
    const [pendingRes, approvedRes, rejectedRes, completedRes] = await Promise.all([
      getBookingList({ pageNum: 1, pageSize: 1, status: 0 }),
      getBookingList({ pageNum: 1, pageSize: 1, status: 1 }),
      getBookingList({ pageNum: 1, pageSize: 1, status: 2 }),
      getBookingList({ pageNum: 1, pageSize: 1, status: 3 })
    ]);
    stats.pending = pendingRes.total || 0;
    stats.approved = approvedRes.total || 0;
    stats.rejected = rejectedRes.total || 0;
    stats.completed = completedRes.total || 0;
  } catch (e) {
    console.error(e);
  }
};

/** 导出当前数据权限范围内的预约 CSV（与后端 /booking/export 一致） */
const handleExportBookings = async () => {
  const kw = searchForm.keyword?.trim();
  const params = {
    status: searchForm.status === null || searchForm.status === undefined ? undefined : searchForm.status,
    // 后端 global-list 为「单号 / 设备名 / 申请人」分列 AND；导出时关键词仅按单号模糊匹配，避免三列同值导致几乎无数据
    orderNo: kw || undefined
  };
  try {
    exportLoading.value = true;
    const res = await exportBookingList(params);
    if (!res) return;
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `预约数据_${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error(e?.message || '导出失败');
  } finally {
    exportLoading.value = false;
  }
};

// ========== 详情 ==========
const handleView = async row => {
  try {
    detailData.value = await getBookingDetail(row.id);
    detailVisible.value = true;
  } catch (e) {
    ElMessage.error(e.message || '获取详情失败');
  }
};

// ========== 审核 ==========
const handleAudit = (row, status) => {
  auditCurrentRow.value = row;
  auditForm.status = status;
  auditForm.auditOpinion = '';
  auditDialogVisible.value = true;
};

const handleAuditSubmit = async () => {
  if (auditForm.status === 2 && !auditForm.auditOpinion?.trim()) {
    ElMessage.warning('拒绝时请填写审核意见');
    return;
  }
  submitLoading.value = true;
  try {
    await auditBooking(auditCurrentRow.value.id, {
      status: auditForm.status,
      opinion: auditForm.auditOpinion
    });
    ElMessage.success('审核成功');
    auditDialogVisible.value = false;
    fetchData();
    fetchStats();
  } catch (e) {
    ElMessage.error(e.message || '审核失败');
  } finally {
    submitLoading.value = false;
  }
};

// ========== 批量审核 ==========
const handleBatchAudit = async status => {
  const pending = selectedRows.value.filter(r => r.status === 0);
  if (pending.length === 0) {
    ElMessage.warning('请选择待审核的预约');
    return;
  }
  const action = status === 1 ? '通过' : '拒绝';
  try {
    await ElMessageBox.confirm(
      `确定批量${action}选中的 ${pending.length} 条预约申请吗？`,
      `批量${action}`,
      { type: 'warning', confirmButtonText: `确定${action}`, cancelButtonText: '取消' }
    );
    batchLoading.value = true;
    const ids = pending.map(r => r.id);
    await batchAuditBooking({ ids, status });
    ElMessage.success(`批量${action}成功`);
    selectedRows.value = [];
    fetchData();
    fetchStats();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || `批量${action}失败`);
  } finally {
    batchLoading.value = false;
  }
};

// ========== 调整预约 ==========
const openAdjust = async row => {
  let merged = { ...row };
  if (!merged.deviceName && !merged.device_name) {
    try {
      const d = await getBookingDetail(row.id);
      if (d) merged = { ...merged, ...d };
    } catch (e) {
      console.error(e);
    }
  }
  const dateOnly = formatBookingDateOnly(merged.bookingDate);
  Object.assign(adjustForm, {
    id: merged.id,
    deviceName: merged.deviceName || merged.device_name || '',
    bookingDate: merged.bookingDate,
    startTime: merged.startTime,
    endTime: merged.endTime,
    newDate: dateOnly,
    newStartTime: merged.startTime,
    newEndTime: merged.endTime,
    reason: ''
  });
  adjustDialogVisible.value = true;
};

const handleAdjustSubmit = async () => {
  const newDate = formatBookingDateOnly(adjustForm.newDate);
  if (!newDate || !adjustForm.newStartTime || !adjustForm.newEndTime) {
    ElMessage.warning('请填写完整的调整信息');
    return;
  }
  if (!adjustForm.reason?.trim()) {
    ElMessage.warning('请填写调整原因');
    return;
  }
  submitLoading.value = true;
  try {
    await adjustBooking(adjustForm.id, {
      newDate,
      newStartTime: adjustForm.newStartTime,
      newEndTime: adjustForm.newEndTime,
      reason: adjustForm.reason
    });
    ElMessage.success('预约调整成功');
    adjustDialogVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(e.message || '调整失败');
  } finally {
    submitLoading.value = false;
  }
};

// ========== 取消预约 ==========
const openCancel = row => {
  Object.assign(cancelForm, {
    id: row.id,
    deviceName: row.deviceName,
    bookingDate: row.bookingDate,
    startTime: row.startTime,
    endTime: row.endTime,
    reason: ''
  });
  cancelDialogVisible.value = true;
};

const handleCancelSubmit = async () => {
  if (!cancelForm.reason?.trim()) {
    ElMessage.warning('请填写取消原因');
    return;
  }
  submitLoading.value = true;
  try {
    await adminCancelBooking(cancelForm.id, { reason: cancelForm.reason });
    ElMessage.success('预约已取消');
    cancelDialogVisible.value = false;
    fetchData();
    fetchStats();
  } catch (e) {
    ElMessage.error(e.message || '取消失败');
  } finally {
    submitLoading.value = false;
  }
};

const openReplace = async row => {
  replaceForm.id = row.id;
  replaceForm.currentDeviceName = row.deviceName || row.device_name || '-';
  replaceForm.newDeviceId = null;
  replaceForm.reason = '';
  replaceDeviceOptions.value = [];
  replaceDialogVisible.value = true;
  try {
    // 若设备名为空，先拉详情补全
    if (!replaceForm.currentDeviceName || replaceForm.currentDeviceName === '-') {
      const detail = await getBookingDetail(row.id);
      if (detail) {
        replaceForm.currentDeviceName = detail.deviceName || detail.device_name || '-';
      }
    }
    const list = await getReplaceableDevices(row.id);
    replaceDeviceOptions.value = (list && list.devices) ? list.devices : (Array.isArray(list) ? list : []);
    if (replaceDeviceOptions.value.length === 0) {
      ElMessage.warning('暂无可替换的设备（同实验室暂无其他空闲设备）');
    }
  } catch (e) {
    ElMessage.error(e.message || '获取可替换设备失败');
  }
};

const handleReplaceSubmit = async () => {
  if (!replaceForm.newDeviceId) {
    ElMessage.warning('请选择替换设备');
    return;
  }
  if (!replaceForm.reason?.trim()) {
    ElMessage.warning('请填写替换原因');
    return;
  }
  submitLoading.value = true;
  try {
    await applyDeviceReplace(replaceForm.id, {
      newDeviceId: replaceForm.newDeviceId,
      reason: replaceForm.reason
    });
    ElMessage.success('设备替换申请已提交');
    replaceDialogVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(e.message || '设备替换失败');
  } finally {
    submitLoading.value = false;
  }
};

// ========== 审核调整申请 ==========
onMounted(async () => {
  await loadBookingDataScope();
  fetchData();
  fetchStats();
});
</script>

<style scoped>
.audit-container {
  color: #e6edf3;
}
.audit-scope-alert {
  margin-bottom: 12px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  color: #e6edf3;
  font-size: 18px;
}
.header-actions {
  display: flex;
  gap: 10px;
}

.search-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
}
.stat-card:hover,
.stat-card.active {
  border-color: #00d4ff;
  background: rgba(0, 212, 255, 0.05);
}
.stat-num {
  font-size: 28px;
  font-weight: bold;
  color: #e6edf3;
}
.stat-label {
  font-size: 13px;
  color: #8b949e;
  margin-top: 4px;
}

.audit-table {
  margin-bottom: 12px;
}

.audit-ops {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  align-items: center;
  white-space: nowrap;
  min-width: 0;
}

.replace-hint {
  color: #8b949e;
  font-size: 12px;
  margin-left: 4px;
}

:deep(.el-table) {
  background: #161b22;
  color: #e6edf3;
}
:deep(.el-table th) {
  background: #1a1f35 !important;
  color: #8b949e !important;
}
:deep(.el-table td) {
  border-color: #21262d !important;
}
:deep(.el-table__row--striped td) {
  background: #1a1f35 !important;
}
:deep(.el-table__row:hover > td) {
  background: #1f2937 !important;
}

:deep(.el-pagination) {
  margin-top: 12px;
  justify-content: flex-end;
}

:deep(.el-dialog) {
  background: #161b22;
  border: 1px solid #30363d;
}
:deep(.el-dialog__header) {
  background: #21262d;
  border-bottom: 1px solid #30363d;
  margin-right: 0;
}
:deep(.el-dialog__title) {
  color: #e6edf3;
}
:deep(.el-dialog__body) {
  background: #0d1117;
  color: #e6edf3;
}
:deep(.el-dialog__footer) {
  background: #161b22;
  border-top: 1px solid #30363d;
}
:deep(.el-descriptions__body) {
  background: #161b22;
}
:deep(.el-descriptions__table) {
  border-color: #30363d !important;
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
:deep(.el-textarea__inner) {
  background: #161b22 !important;
  color: #e6edf3 !important;
  box-shadow: 0 0 0 1px #30363d inset !important;
}
:deep(.el-textarea__inner::placeholder) {
  color: #8b949e !important;
}
:deep(.el-input__wrapper) {
  background: #161b22 !important;
  box-shadow: 0 0 0 1px #30363d inset !important;
}
:deep(.el-input__inner) {
  color: #e6edf3 !important;
}
:deep(.el-input__inner::placeholder) {
  color: #8b949e !important;
}
:deep(.el-select__wrapper) {
  background: #161b22 !important;
  box-shadow: 0 0 0 1px #30363d inset !important;
}
:deep(.el-select__selected-item) {
  color: #e6edf3 !important;
}
:deep(.el-select__placeholder) {
  color: #8b949e !important;
}
:deep(.el-form-item__label) {
  color: #8b949e !important;
}
:deep(.el-radio__label) {
  color: #e6edf3 !important;
}
</style>
