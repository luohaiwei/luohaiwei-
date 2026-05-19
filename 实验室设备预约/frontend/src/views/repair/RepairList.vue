<template>
  <div class="repair-container">
    <div class="page-header">
      <h2>维修工单管理</h2>
    </div>

    <div v-if="isMaintainer" class="stats-cards">
      <div class="stat-card">
        <div class="stat-value">{{ maintStats.total }}</div>
        <div class="stat-label">个人工单总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ maintStats.pending }}</div>
        <div class="stat-label">待处理工单</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ maintStats.processing }}</div>
        <div class="stat-label">处理中工单</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ maintStats.completionRate }}%</div>
        <div class="stat-label">工单完成率</div>
      </div>
    </div>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="orderNo" label="工单编号" width="150" />
      <el-table-column prop="deviceName" label="设备名称" />
      <el-table-column prop="reporterName" label="报修人" width="100" />
      <el-table-column prop="faultDescription" label="故障描述" show-overflow-tooltip />
      <el-table-column prop="reportTime" label="报修时间" width="160" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.status === 0" type="warning">待处理</el-tag>
          <el-tag v-else-if="row.status === 1" type="primary">处理中</el-tag>
          <el-tag v-else-if="row.status === 2" type="success">已完成</el-tag>
          <el-tag v-else-if="row.status === 3" type="info">已关闭</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template #default="{ row }">
          <el-button size="small" @click="handleView(row)">详情</el-button>
          <el-button
            v-if="row.status === 0 && canAssign"
            size="small"
            type="primary"
            @click="openAssignDialog(row)"
            >分配</el-button
          >
          <el-button
            v-if="row.status === 0 && canAccept(row)"
            size="small"
            type="success"
            @click="handleAccept(row)"
            >接单</el-button
          >
          <el-button
            v-if="row.status === 1 && canComplete(row)"
            size="small"
            type="success"
            @click="handleProcess(row)"
            >完成维修</el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchData"
    />

    <el-dialog v-model="assignDialogVisible" title="分配维护人员" width="400px">
      <el-form label-width="100px">
        <el-form-item label="维护人员">
          <el-select v-model="assignForm.handlerId" placeholder="选择维护人员" style="width: 100%">
            <el-option
              v-for="u in maintainerList"
              :key="u.id"
              :label="u.realName || u.username"
              :value="u.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAssign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 工单详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      title="维修工单详情"
      width="700px"
      class="repair-detail-dialog"
    >
      <div v-if="detailData.orderNo" class="detail-content">
        <div class="detail-section">
          <h4>基本信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="工单编号">{{ detailData.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="报修时间">{{
              detailData.reportTime
            }}</el-descriptions-item>
            <el-descriptions-item label="设备名称">{{
              detailData.deviceName
            }}</el-descriptions-item>
            <el-descriptions-item label="设备编号">{{
              detailData.deviceNo || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="报修人">{{
              detailData.reporterName
            }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="detailData.status === 0" type="warning">待处理</el-tag>
              <el-tag v-else-if="detailData.status === 1" type="primary">处理中</el-tag>
              <el-tag v-else-if="detailData.status === 2" type="success">已完成</el-tag>
              <el-tag v-else-if="detailData.status === 3" type="info">已关闭</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <h4>故障信息</h4>
          <div class="detail-item">
            <span class="detail-label">故障描述：</span>
            <span class="detail-value">{{ detailData.faultDescription }}</span>
          </div>
          <div v-if="detailData.imagePath" class="detail-item repair-fault-img-wrap">
            <span class="detail-label">故障图片：</span>
            <img
              class="repair-fault-img"
              :src="repairImageUrl(detailData.imagePath)"
              alt="故障图片"
            />
          </div>
        </div>

        <div v-if="detailData.status >= 1" class="detail-section">
          <h4>处理信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="处理人">{{
              detailData.handlerName || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{
              detailData.handleStartTime || '-'
            }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div v-if="detailData.status === 2" class="detail-section">
          <h4>维修结果</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="故障原因">{{
              detailData.faultCause || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="维修方案">{{
              detailData.repairSolution || '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="维修费用">{{
              detailData.repairCost != null ? '¥' + detailData.repairCost : '-'
            }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{
              detailData.handleEndTime || '-'
            }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="完成维修" width="600px">
      <el-form ref="formRef" :model="form" label-width="100px">
        <el-form-item label="故障原因">
          <el-input v-model="form.faultCause" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="维修方案">
          <el-input v-model="form.repairSolution" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="维修费用">
          <el-input-number v-model="form.cost" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">完成维修</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getRepairList, completeRepair, handleRepair, acceptRepair } from '@/api/repair';
import { getUsersByType } from '@/api/user';
import { useUserStore } from '@/stores/user';

const userStore = useUserStore();
const isMaintainer = computed(() => userStore.userInfo?.userType === 'MAINTAINER');

const perms = computed(() => userStore.userInfo?.permissions || []);
const hasPerm = (codes) => codes.some((c) => perms.value.includes(c));

/** 与后端 PUT /repair/{id}/handle 一致 */
const canAssign = computed(() => {
  const t = userStore.userInfo?.userType;
  if (t === 'LAB_ADMIN' || t === 'SYSTEM_ADMIN') return true;
  return hasPerm(['repair', 'repair-list', 'repair-global-monitor']);
});

/** 与后端 PUT /repair/{id}/complete 一致（维护员仅本人处理中单） */
const canComplete = (row) => {
  if (row.status !== 1) return false;
  const t = userStore.userInfo?.userType;
  const uid = userStore.userInfo?.id ?? userStore.userInfo?.userId;
  if (t === 'MAINTAINER') return row.handlerId === uid;
  if (t === 'LAB_ADMIN' || t === 'SYSTEM_ADMIN') return true;
  return hasPerm(['repair', 'repair-list', 'repair:complete']);
};

/** 与后端 PUT /repair/{id}/accept 一致 */
const canAccept = (row) => {
  if (row.status !== 0 || row.handlerId != null) return false;
  return hasPerm(['repair:handle']);
};

const repairImageUrl = p => {
  if (!p) return '';
  if (p.startsWith('http://') || p.startsWith('https://')) return p;
  const s = p.startsWith('/') ? p : `/${p}`;
  return `/api${s}`;
};

const handleAccept = async row => {
  try {
    await acceptRepair(row.id);
    ElMessage.success('接单成功');
    fetchData();
  } catch (e) {
    ElMessage.error(e.message || '接单失败');
  }
};

const tableData = ref([]);
const maintStats = ref({
  total: 0,
  pending: 0,
  processing: 0,
  completionRate: 0
});
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);
const formRef = ref();
const currentRow = ref(null);
const form = ref({
  faultCause: '',
  repairSolution: '',
  cost: 0
});

const assignDialogVisible = ref(false);
const assignForm = ref({ handlerId: null });
const assignCurrentRow = ref(null);
const maintainerList = ref([]);

const openAssignDialog = async row => {
  assignCurrentRow.value = row;
  assignForm.value = { handlerId: null };
  try {
    maintainerList.value = (await getUsersByType('MAINTAINER')) || [];
  } catch (e) {
    ElMessage.error('获取维护人员列表失败');
    return;
  }
  assignDialogVisible.value = true;
};

const submitAssign = async () => {
  if (!assignForm.value.handlerId) {
    ElMessage.warning('请选择维护人员');
    return;
  }
  try {
    await handleRepair(assignCurrentRow.value.id, assignForm.value.handlerId);
    ElMessage.success('分配成功');
    assignDialogVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(e.message || '分配失败');
  }
};

const fetchData = async () => {
  try {
    const res = await getRepairList({ pageNum: pageNum.value, pageSize: pageSize.value });
    tableData.value = res.list || [];
    total.value = res.total || 0;
    calcMaintStats();
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败');
  }
};

const calcMaintStats = () => {
  const list = tableData.value || [];
  const totalCount = list.length;
  const pendingCount = list.filter(i => i.status === 0).length;
  const processingCount = list.filter(i => i.status === 1).length;
  const completedCount = list.filter(i => i.status === 2).length;
  const completionRate = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;
  maintStats.value = {
    total: totalCount,
    pending: pendingCount,
    processing: processingCount,
    completionRate
  };
};

const detailVisible = ref(false);
const detailData = ref({});

const handleView = row => {
  detailData.value = row;
  detailVisible.value = true;
};

const handleProcess = row => {
  currentRow.value = row;
  form.value = { faultCause: '', repairSolution: '', cost: 0 };
  dialogVisible.value = true;
};

const handleSubmit = async () => {
  try {
    await completeRepair(currentRow.value.id, {
      faultCause: form.value.faultCause,
      repairSolution: form.value.repairSolution,
      cost: form.value.cost
    });
    ElMessage.success('工单已完成');
    dialogVisible.value = false;
    fetchData();
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.repair-container {
  color: #e6edf3;
}
.page-header {
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  color: #e6edf3;
}
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}
.stat-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 8px;
  padding: 14px;
  text-align: center;
}
.stat-value {
  color: #00d4ff;
  font-size: 24px;
  font-weight: 700;
}
.stat-label {
  color: #8b949e;
  font-size: 12px;
  margin-top: 4px;
}
:deep(.el-table) {
  background: #161b22;
  color: #e6edf3;
}
:deep(.el-table__header) {
  background: #21262d;
}
:deep(.el-pagination) {
  margin-top: 20px;
  justify-content: flex-end;
}

/* 详情弹窗样式 - 深色主题 */
:deep(.repair-detail-dialog) {
  .el-dialog {
    background: #161b22;
    border: 1px solid #30363d;
  }

  .el-dialog__header {
    background: #21262d;
    border-bottom: 1px solid #30363d;
    padding: 16px 20px;
    margin-right: 0;
  }

  .el-dialog__title {
    color: #e6edf3;
    font-weight: 600;
  }

  .el-dialog__headerbtn .el-dialog__close {
    color: #8b949e;
  }

  .el-dialog__headerbtn:hover .el-dialog__close {
    color: #00d4ff;
  }

  .el-dialog__body {
    background: #0d1117;
    padding: 20px;
  }

  .el-dialog__footer {
    background: #161b22;
    border-top: 1px solid #30363d;
    padding: 16px 20px;
  }
}

.detail-content {
  color: #e6edf3;
}

.detail-section {
  margin-bottom: 24px;
}

.detail-section h4 {
  margin: 0 0 12px 0;
  color: #00d4ff;
  font-size: 16px;
  border-left: 3px solid #00d4ff;
  padding-left: 10px;
  font-weight: 600;
}

.detail-item {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 6px;
  padding: 12px;
}

.detail-label {
  color: #8b949e;
  font-weight: 500;
}

.detail-value {
  color: #e6edf3;
  line-height: 1.6;
}

/* el-descriptions 深色主题 */
:deep(.el-descriptions) {
  background: #161b22;
}

:deep(.el-descriptions__header) {
  background: #161b22;
}

:deep(.el-descriptions__title) {
  color: #e6edf3;
}

:deep(.el-descriptions__body) {
  background: #161b22;
}

:deep(.el-descriptions__table) {
  background: #161b22;
  border-color: #30363d;
}

:deep(.el-descriptions__cell) {
  background: #161b22;
  border-color: #30363d !important;
}

:deep(.el-descriptions__label) {
  background: #21262d !important;
  color: #8b949e !important;
  border-color: #30363d !important;
}

:deep(.el-descriptions__content) {
  background: #161b22 !important;
  color: #e6edf3 !important;
  border-color: #30363d !important;
}

:deep(.el-descriptions--bordered .el-descriptions__cell) {
  border: 1px solid #30363d !important;
}

:deep(.el-descriptions--bordered .el-descriptions__label) {
  background: #21262d !important;
}

:deep(.el-descriptions--bordered .el-descriptions__content) {
  background: #161b22 !important;
}

/* 分配弹窗和完成弹窗样式 */
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

:deep(.el-form-item__label) {
  color: #8b949e;
}

.repair-fault-img-wrap {
  margin-top: 12px;
}
.repair-fault-img {
  max-width: 100%;
  max-height: 240px;
  margin-top: 8px;
  border-radius: 8px;
  border: 1px solid #30363d;
}
</style>
