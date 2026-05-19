<template>
  <div class="device-monitor-tab">
    <div class="status-cards">
      <div class="status-card" v-for="s in statusCards" :key="s.status">
        <div class="status-value">{{ s.count }}</div>
        <div class="status-label">{{ s.label }}</div>
      </div>
    </div>

    <el-table :data="tableData" border>
      <el-table-column prop="deviceNo" label="设备编号" width="120" />
      <el-table-column prop="deviceName" label="设备名称" min-width="150" />
      <el-table-column prop="laboratory" label="实验室" width="120" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column
        prop="nextCalibrationDate"
        label="下次校准"
        width="120"
        :formatter="formatDate"
      />
      <el-table-column v-if="canUpdateStatus" label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-select
            v-model="row._editStatus"
            size="small"
            placeholder="更新状态"
            style="width: 120px"
            @change="v => updateStatus(row, v)"
          >
            <el-option label="空闲" :value="0" />
            <el-option label="使用中" :value="1" />
            <el-option label="维修中" :value="2" />
            <el-option label="校准中" :value="3" />
            <el-option label="报废" :value="4" />
          </el-select>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { getDeviceList } from '@/api/device';
import { updateDeviceStatus } from '@/api/device';
import { getDeviceStatus } from '@/api/statistics';
import { useUserStore } from '@/stores/user';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';

const userStore = useUserStore();
const canUpdateStatus = computed(() => {
  const t = userStore.userInfo?.userType;
  return t === 'SYSTEM_ADMIN' || t === 'LAB_ADMIN' || t === 'MAINTAINER';
});

const statusCards = reactive([
  { status: 0, label: '空闲', count: 0 },
  { status: 1, label: '使用中', count: 0 },
  { status: 2, label: '维修中', count: 0 },
  { status: 3, label: '校准中', count: 0 },
  { status: 4, label: '报废', count: 0 }
]);
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);

const getStatusType = s =>
  ({ 0: 'success', 1: 'primary', 2: 'warning', 3: 'info', 4: 'danger' })[s] || 'info';
const getStatusText = s =>
  ({ 0: '空闲', 1: '使用中', 2: '维修中', 3: '校准中', 4: '报废' })[s] || '未知';
const formatDate = (r, c, v) => (v ? dayjs(v).format('YYYY-MM-DD') : '-');

const loadStatusCounts = async () => {
  try {
    const res = await getDeviceStatus();
    statusCards[0].count = res.idle || 0;
    statusCards[1].count = res.using || 0;
    statusCards[2].count = res.maintaining || 0;
    statusCards[3].count = res.calibrating || 0;
    statusCards[4].count = res.scrapped || 0;
  } catch (e) {
    console.error(e);
  }
};

const loadData = async () => {
  try {
    const res = await getDeviceList({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = (res.list || []).map(d => ({ ...d, _editStatus: d.status }));
    total.value = res.total || 0;
    loadStatusCounts();
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败');
  }
};

const updateStatus = async (row, status) => {
  try {
    await updateDeviceStatus(row.id, status);
    ElMessage.success('状态更新成功');
    loadData();
  } catch (e) {
    ElMessage.error(e.message || '更新失败');
  }
};

onMounted(() => {
  loadData();
});
</script>

<style lang="scss" scoped>
.device-monitor-tab {
  color: #e6edf3;
}
.status-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}
.status-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 16px 24px;
  min-width: 100px;
  text-align: center;
}
.status-value {
  font-size: 28px;
  font-weight: 600;
  color: #00d4ff;
}
.status-label {
  font-size: 12px;
  color: #8b949e;
  margin-top: 4px;
}

/* 深色主题表格：统一深色背景和浅色文字，避免 stripe 导致的白色行看不清 */
.device-monitor-tab :deep(.el-table) {
  --el-table-bg-color: #161b22 !important;
  --el-table-tr-bg-color: #161b22 !important;
  --el-table-row-hover-bg-color: #21262d !important;
  background: #161b22 !important;
  color: #e6edf3 !important;
}
.device-monitor-tab :deep(.el-table__body tr),
.device-monitor-tab :deep(.el-table__body tr td) {
  background: #161b22 !important;
  color: #e6edf3 !important;
}
.device-monitor-tab :deep(.el-table__body tr:nth-child(even)),
.device-monitor-tab :deep(.el-table__body tr:nth-child(even) td) {
  background: #21262d !important;
  color: #e6edf3 !important;
}
.device-monitor-tab :deep(.el-table th.el-table__cell),
.device-monitor-tab :deep(.el-table td.el-table__cell) {
  color: #e6edf3 !important;
  border-color: #30363d !important;
}
.device-monitor-tab :deep(.el-table__header) {
  background: #21262d !important;
}
.device-monitor-tab :deep(.el-table__header th) {
  background: #21262d !important;
  color: #e6edf3 !important;
}
</style>
