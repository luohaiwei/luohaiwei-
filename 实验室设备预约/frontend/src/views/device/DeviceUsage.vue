<template>
  <div class="device-usage-container">
    <!-- 页面头部 -->
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>设备使用记录</h2>
        <span class="header-sub">所属实验室设备使用流水记录查询、统计与导出</span>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">使用总次数</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color: #00ff88">{{ stats.todayCount }}</div>
        <div class="stat-label">今日使用</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color: #00d4ff">{{ stats.weekCount }}</div>
        <div class="stat-label">本周使用</div>
      </div>
      <div class="stat-card">
        <div class="stat-num" style="color: #ff9500">{{ stats.totalHours }}</div>
        <div class="stat-label">累计使用时长(h)</div>
      </div>
    </div>

    <!-- 搜索区 -->
    <div class="page-card search-card">
      <div class="search-row">
        <div class="search-field">
          <label>设备名称</label>
          <el-input
            v-model="searchForm.deviceName"
            placeholder="设备名称"
            clearable
            @keyup.enter="resetAndLoad"
          />
        </div>
        <div class="search-field">
          <label>使用人</label>
          <el-input
            v-model="searchForm.userName"
            placeholder="使用人"
            clearable
            @keyup.enter="resetAndLoad"
          />
        </div>
        <div class="search-field">
          <label>日期范围</label>
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            @change="resetAndLoad"
          />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="resetAndLoad">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button @click="handleExport" :loading="exportLoading">导出</el-button>
        </div>
      </div>
    </div>

    <!-- 使用记录列表 -->
    <div class="page-card table-card">
      <el-table :data="tableData" border stripe>
        <el-table-column prop="deviceName" label="设备名称" min-width="160" />
        <el-table-column prop="deviceNo" label="设备编号" width="120" />
        <el-table-column prop="userName" label="使用人" width="100" />
        <el-table-column prop="department" label="部门/班级" width="140" show-overflow-tooltip />
        <el-table-column label="使用日期" width="140">
          <template #default="{ row }">{{ formatUsageDate(row.usageDate) }}</template>
        </el-table-column>
        <el-table-column label="使用时段" width="150">
          <template #default="{ row }">{{ formatTimeRange(row.startTime) }} - {{ formatTimeRange(row.endTime) }}</template>
        </el-table-column>
        <el-table-column label="使用时长(h)" width="110" align="center">
          <template #default="{ row }">{{ row.durationHours || '-' }}</template>
        </el-table-column>
        <el-table-column
          prop="experimentProject"
          label="实验项目"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column prop="usageResult" label="使用结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.usageResult === '正常' ? 'success' : 'warning'">{{
              row.usageResult || '正常'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { getDeviceUsageRecords, exportDeviceUsageRecords } from '@/api/device';

const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const exportLoading = ref(false);
const stats = reactive({ total: 0, todayCount: 0, weekCount: 0, totalHours: 0 });

const searchForm = reactive({
  deviceName: '',
  userName: '',
  dateRange: null
});

const getParams = () => {
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    deviceName: searchForm.deviceName,
    userName: searchForm.userName
  };
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    params.startDate = searchForm.dateRange[0];
    params.endDate = searchForm.dateRange[1];
  }
  return params;
};

const resetSearch = () => {
  searchForm.deviceName = '';
  searchForm.userName = '';
  searchForm.dateRange = null;
  resetAndLoad();
};

const resetAndLoad = () => {
  pageNum.value = 1;
  loadData();
};

const loadData = async () => {
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      deviceName: searchForm.deviceName,
      userName: searchForm.userName
    };
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0];
      params.endDate = searchForm.dateRange[1];
    }
    const res = await getDeviceUsageRecords(params);
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败');
  }
};

const loadStats = async () => {
  // 统计基于全部已完成预约数据
  try {
    const res = await getDeviceUsageRecords({ pageNum: 1, pageSize: 10000 });
    const all = res.list || [];
    const today = new Date().toISOString().slice(0, 10);
    const weekAgo = new Date(Date.now() - 7 * 86400000).toISOString().slice(0, 10);
    const todayItems = all.filter(r => r.usageDate && String(r.usageDate).slice(0, 10) === today);
    const weekItems = all.filter(r => r.usageDate && r.usageDate >= weekAgo);
    const totalHours = all.reduce((sum, r) => sum + (parseFloat(r.durationHours) || 0), 0);
    Object.assign(stats, {
      total: all.length,
      todayCount: todayItems.length,
      weekCount: weekItems.length,
      totalHours: Math.round(totalHours * 10) / 10
    });
  } catch (e) {
    console.error(e);
  }
};

const handleExport = async () => {
  exportLoading.value = true;
  try {
    const params = {
      deviceName: searchForm.deviceName,
      userName: searchForm.userName
    };
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0];
      params.endDate = searchForm.dateRange[1];
    }
    const raw = await exportDeviceUsageRecords(params);
    const blob =
      raw instanceof Blob
        ? raw
        : new Blob([raw], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `设备使用记录_${new Date().toISOString().slice(0, 10)}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    ElMessage.error(e.message || '导出失败');
  } finally {
    exportLoading.value = false;
  }
};

/** 格式化使用日期：将ISO格式转为 YYYY-MM-DD */
const formatUsageDate = (dateStr) => {
  if (!dateStr) return '-';
  // 处理 ISO 格式: 2026-04-13T00:00:00 → 2026-04-13
  if (dateStr.includes('T')) {
    return dateStr.split('T')[0];
  }
  return dateStr;
};

/** 格式化时间：将完整时间转为 HH:mm */
const formatTimeRange = (timeStr) => {
  if (!timeStr) return '--';
  // 处理格式: 09:00:00 或 09:00 或 2026-04-13T09:00:00
  let time = timeStr;
  if (time.includes('T')) {
    time = time.split('T')[1];
  }
  // 只取时:分部分
  const parts = time.split(':');
  if (parts.length >= 2) {
    return `${parts[0]}:${parts[1]}`;
  }
  return time;
};

onMounted(() => {
  loadData();
  loadStats();
});
</script>

<style scoped>
.device-usage-container {
  color: #e6edf3;
}
.page-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.page-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #161b22 0%, #1a1f35 100%);
  border-left: 4px solid #00d4ff;
  padding: 18px 24px;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.page-header-card h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #e6edf3;
}
.header-sub {
  font-size: 12px;
  color: #8b949e;
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
.search-card {
  padding-bottom: 16px;
}
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
  min-width: 160px;
  max-width: 240px;
}
.search-field label {
  font-size: 12px;
  font-weight: 600;
  color: #8b949e;
}
.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 2px;
}
.table-card {
  padding: 0;
  overflow: hidden;
}
.pagination-bar {
  padding: 14px 16px 10px;
  border-top: 1px solid #30363d;
  display: flex;
  justify-content: flex-end;
}
:deep(.el-table) {
  background: #161b22;
  color: #e6edf3;
  border: none;
}
:deep(.el-table th.el-table__cell) {
  background: #1a1f35 !important;
  color: #8b949e !important;
  font-size: 12px;
  padding: 12px 8px;
  border-bottom: 1px solid #30363d !important;
}
:deep(.el-table td.el-table__cell) {
  padding: 12px 8px;
  border-bottom: 1px solid #21262d !important;
}
:deep(.el-table__row--striped td) {
  background: #1a1f35 !important;
}
:deep(.el-table__row:hover > td) {
  background: #1f2937 !important;
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
:deep(.el-date-editor.el-input__wrapper) {
  background: #161b22 !important;
  box-shadow: 0 0 0 1px #30363d inset !important;
}
:deep(.el-range-input) {
  color: #e6edf3 !important;
}
:deep(.el-range-separator) {
  color: #8b949e !important;
}
</style>
