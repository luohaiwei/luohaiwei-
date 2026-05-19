<template>
  <div class="statistics-container">
    <div class="page-header">
      <h2>{{ pageTitle }}</h2>
      <div class="export-btns">
        <el-button type="success" :loading="exportLoading" @click="handleExportExcel">
          <el-icon><Download /></el-icon> 导出Excel
        </el-button>
        <el-button type="warning" :loading="exportLoading" @click="handleExportPdf">
          <el-icon><Download /></el-icon> 导出PDF
        </el-button>
      </div>
    </div>

    <!-- ==================== 系统管理员/实验室管理员：多标签页统计 ==================== -->
    <div v-if="isAdmin || isLabAdmin">
      <el-tabs v-model="activeTab" class="admin-tabs" lazy @tab-change="onAdminTabChange">
        <!-- 用户统计分析（仅系统管理员） -->
        <el-tab-pane v-if="isAdmin" label="用户统计" name="user">
          <div class="stats-grid-4">
            <div class="stat-card">
              <div class="stat-title">用户总数</div>
              <div class="stat-value">{{ userStats.total }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">实验室管理员</div>
              <div class="stat-value">{{ userStats.labAdmin }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">教师</div>
              <div class="stat-value">{{ userStats.teacher }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">学生</div>
              <div class="stat-value">{{ userStats.student }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">设备维护人员</div>
              <div class="stat-value">{{ userStats.maintainer }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">系统管理员</div>
              <div class="stat-value">{{ userStats.systemAdmin }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本周活跃用户</div>
              <div class="stat-value">{{ userStats.activeWeek }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本月活跃用户</div>
              <div class="stat-value">{{ userStats.activeMonth }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">新增用户(本月)</div>
              <div class="stat-value">{{ userStats.newMonth }}</div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>用户活跃度趋势（本周）</h3>
              <div ref="userTrendChartRef" style="height: 280px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>各角色用户数量分布</h3>
              <div ref="userRoleChartRef" style="height: 280px"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 用户画像分析（任务书第31-33行） -->
        <el-tab-pane label="用户画像" name="user-profile">
          <div class="stats-grid-4">
            <div class="stat-card">
              <div class="stat-title">用户总数</div>
              <div class="stat-value">{{ userProfile.summary.totalUsers }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">活跃用户</div>
              <div class="stat-value" style="color: #00ff88">{{ userProfile.summary.activeUsers }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">平均技能等级</div>
              <div class="stat-value">{{ userProfile.summary.avgSkillLevel }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">最受欢迎实验类型</div>
              <div class="stat-value" style="color: #00d4ff; font-size: 14px">{{ userProfile.summary.mostPopularExpType }}</div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card">
              <h3>实验类型偏好分布（饼图）</h3>
              <div ref="expTypeChartRef" style="height: 300px"></div>
            </div>
            <div class="chart-card">
              <h3>用户技能等级分布（饼图）</h3>
              <div ref="skillLevelChartRef" style="height: 300px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card">
              <h3>用户能力雷达图</h3>
              <div ref="userAbilityRadarChartRef" style="height: 350px"></div>
            </div>
            <div class="chart-card">
              <h3>用户使用频率排行TOP10</h3>
              <div ref="userUsageRankChartRef" style="height: 350px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>用户使用详情列表</h3>
              <el-table :data="userProfile.userUsageRanking" stripe size="small" max-height="300">
                <el-table-column prop="rank" label="排行" width="60" align="center" />
                <el-table-column prop="realName" label="姓名" width="100" />
                <el-table-column prop="userType" label="用户类型" width="100" />
                <el-table-column prop="experimentType" label="实验类型" width="120" />
                <el-table-column prop="bookingCount" label="预约次数" width="100" align="center" />
                <el-table-column prop="skillLevel" label="技能等级" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag v-if="row.skillLevel === 3" type="success" size="small">熟练</el-tag>
                    <el-tag v-else-if="row.skillLevel === 2" type="warning" size="small">一般</el-tag>
                    <el-tag v-else type="info" size="small">初学</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <!-- 设备使用分析 -->
        <el-tab-pane label="设备使用分析" name="device">
          <div class="stats-grid-4">
            <div class="stat-card">
              <div class="stat-title">设备总数</div>
              <div class="stat-value">{{ deviceStats.total }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">空闲设备</div>
              <div class="stat-value" style="color: #00ff88">{{ deviceStats.idle }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">使用中</div>
              <div class="stat-value" style="color: #00d4ff">{{ deviceStats.using }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">维修中</div>
              <div class="stat-value" style="color: #ff9500">{{ deviceStats.maintaining }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">校准中</div>
              <div class="stat-value" style="color: #7b61ff">{{ deviceStats.calibrating }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">已报废</div>
              <div class="stat-value" style="color: #ff4757">{{ deviceStats.scrapped }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">设备利用率</div>
              <div class="stat-value">{{ deviceStats.usageRate }}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">平均利用率</div>
              <div class="stat-value">{{ deviceStats.avgUsageRate }}%</div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card">
              <h3>设备状态分布</h3>
              <div ref="deviceStatusChartRef" style="height: 300px"></div>
            </div>
            <div class="chart-card">
              <h3>设备类型分布</h3>
              <div ref="deviceTypeChartRef" style="height: 300px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>热门设备TOP10（按预约次数）</h3>
              <div ref="hotDeviceChartRef" style="height: 300px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>闲置设备分析（本周未使用）</h3>
              <el-table :data="idleDevices" stripe size="small" max-height="260">
                <el-table-column prop="deviceName" label="设备名称" min-width="160" />
                <el-table-column prop="deviceNo" label="设备编号" width="120" />
                <el-table-column prop="category" label="分类" width="120" />
                <el-table-column prop="lastUsedDate" label="最后使用日期" width="140" />
                <el-table-column prop="idleDays" label="闲置天数" width="100" align="center" />
              </el-table>
            </div>
          </div>
        </el-tab-pane>

        <!-- 预约全量分析 -->
        <el-tab-pane label="预约全量分析" name="booking">
          <div class="stats-grid-4">
            <div class="stat-card">
              <div class="stat-title">本周预约总量</div>
              <div class="stat-value">{{ bookingStats.weekTotal }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">今日预约</div>
              <div class="stat-value">{{ bookingStats.todayTotal }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">预约成功率</div>
              <div class="stat-value" style="color: #00ff88">{{ bookingStats.successRate }}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">预约取消率</div>
              <div class="stat-value" style="color: #ff9500">{{ bookingStats.cancelRate }}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">待审核</div>
              <div class="stat-value" style="color: #7b61ff">{{ bookingStats.pending }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">平均审核时长</div>
              <div class="stat-value">{{ bookingStats.avgAuditHours }}h</div>
            </div>
            <div class="stat-card highlight">
              <div class="stat-title">平均等待时长</div>
              <div class="stat-value">{{ bookingStats.avgWaitHours }}h</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本月预约总量</div>
              <div class="stat-value">{{ bookingStats.monthTotal }}</div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>预约趋势</h3>
              <div ref="bookingTrendChartRef" style="height: 280px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card">
              <h3>预约状态分布</h3>
              <div ref="bookingStatusChartRef" style="height: 280px"></div>
            </div>
            <div class="chart-card">
              <h3>预约高峰时段</h3>
              <div ref="peakHoursChartRef" style="height: 280px"></div>
            </div>
          </div>
          <!-- 智能预测：预约趋势预测图表 -->
          <div class="chart-row">
            <div class="chart-card full">
              <PredictionChart title="预约趋势预测" />
            </div>
          </div>
        </el-tab-pane>

        <!-- 维护统计分析 -->
        <el-tab-pane label="维护统计" name="maintenance">
          <div class="stats-grid-4">
            <div class="stat-card">
              <div class="stat-title">本周维护次数</div>
              <div class="stat-value">{{ maintStats.weekCount }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本月维护次数</div>
              <div class="stat-value">{{ maintStats.monthCount }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">待处理工单</div>
              <div class="stat-value" style="color: #ff4757">{{ maintStats.pending }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">处理中工单</div>
              <div class="stat-value" style="color: #ff9500">{{ maintStats.processing }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">平均维修时长</div>
              <div class="stat-value">{{ maintStats.avgRepairHours }}h</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本月维修成本</div>
              <div class="stat-value">¥{{ maintStats.monthCost }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本周校准次数</div>
              <div class="stat-value">{{ maintStats.weekCalibration }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">本月校准次数</div>
              <div class="stat-value">{{ maintStats.monthCalibration }}</div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card">
              <h3>故障类型分布</h3>
              <div ref="faultTypeChartRef" style="height: 280px"></div>
            </div>
            <div class="chart-card">
              <h3>维修时长趋势（本周）</h3>
              <div ref="repairTrendChartRef" style="height: 280px"></div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>维修成本统计（本月）</h3>
              <div ref="costChartRef" style="height: 280px"></div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 校准达标率统计（任务书：设备数据分析 → 校准达标率） -->
        <el-tab-pane label="校准达标率" name="calibration">
          <div class="stats-grid-4">
            <div class="stat-card">
              <div class="stat-title">校准总次数</div>
              <div class="stat-value">{{ calibrationRate.total }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">达标次数</div>
              <div class="stat-value" style="color: #67C23A">{{ calibrationRate.passed }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">当前达标率</div>
              <div class="stat-value" style="color: #409EFF">{{ calibrationRate.rate }}%</div>
            </div>
            <div class="stat-card">
              <div class="stat-title">统计周期</div>
              <div class="stat-value" style="color: #909399">{{ calibrationRate.months }}个月</div>
            </div>
          </div>
          <div class="chart-row">
            <div class="chart-card full">
              <h3>校准达标率趋势（近{{ calibrationRate.months }}个月）</h3>
              <div ref="calibrationRateChartRef" style="height: 300px"></div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- ==================== 设备维护人员：故障统计视图 ==================== -->
    <div v-else-if="isMaintainer">
      <div class="stats-grid-4">
        <div class="stat-card">
          <div class="stat-title">处理中工单</div>
          <div class="stat-value" style="color:#ff9500">{{ maintStats.processing }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">本周完成</div>
          <div class="stat-value" style="color:#00ff88">{{ maintStats.weekCompleted }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">本月完成</div>
          <div class="stat-value" style="color:#00d4ff">{{ maintStats.monthCompleted }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">平均维修时长</div>
          <div class="stat-value">{{ maintStats.avgRepairHours }}h</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">本月维修成本</div>
          <div class="stat-value">¥{{ maintStats.monthCost }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-title">本月校准次数</div>
          <div class="stat-value" style="color:#7b61ff">{{ maintStats.monthCalibration }}</div>
        </div>
      </div>
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-card">
            <h3>故障类型分布（近30天）</h3>
            <div ref="faultTypeChartRef" style="height: 300px"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <h3>校准达标率趋势（近6个月）</h3>
            <div ref="maintCalibrationChartRef" style="height: 300px"></div>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- ==================== 其他角色：通用统计 ==================== -->
    <div v-else>
      <!-- 教师：顶部指标与审核/本人预约一致，避免与全系统看板混淆 -->
      <template v-if="isTeacher">

        <el-row :gutter="20" class="stats-row">
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">我的预约（总数）</div>
              <div class="stat-value">{{ teacherScopeStats.myBookings }}</div>
            </div></el-col
          >
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">待审核学生预约</div>
              <div class="stat-value">{{ teacherScopeStats.pendingStudent }}</div>
            </div></el-col
          >
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">我已完成预约</div>
              <div class="stat-value">{{ teacherScopeStats.myCompleted }}</div>
            </div></el-col
          >
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">今日全系统预约</div>
              <div class="stat-value">{{ stats.todayBookings }}</div>
            </div></el-col
          >
        </el-row>
      </template>
      <template v-else>
        <el-row :gutter="20" class="stats-row">
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">设备总数</div>
              <div class="stat-value">{{ stats.totalDevices }}</div>
            </div></el-col
          >
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">空闲设备</div>
              <div class="stat-value">{{ stats.idleDevices }}</div>
            </div></el-col
          >
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">使用中设备</div>
              <div class="stat-value">{{ stats.usingDevices }}</div>
            </div></el-col
          >
          <el-col :span="6"
            ><div class="stat-card">
              <div class="stat-title">今日预约</div>
              <div class="stat-value">{{ stats.todayBookings }}</div>
            </div></el-col
          >
        </el-row>
      </template>

      <el-row :gutter="20" class="stats-row">
        <el-col :span="8">
          <div class="stat-card highlight">
            <div class="stat-title">平均预约等待时长（本周）</div>
            <div class="stat-value wait-time">
              {{ waitStats.avgWaitTime }} <span class="stat-unit">小时</span>
            </div>
            <div class="period-selector">
              <el-radio-group v-model="waitStats.period" size="small" @change="handlePeriodChange">
                <el-radio-button label="day">今日</el-radio-button>
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <div class="chart-card">
            <h3>设备状态分布</h3>
            <div ref="deviceStatusChart" style="height: 300px"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="chart-card">
            <h3>设备类型分布</h3>
            <div ref="deviceTypeChart" style="height: 300px"></div>
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24"
          ><div class="chart-card">
            <h3>预约趋势</h3>
            <div ref="bookingTrendChart" style="height: 300px"></div></div
        ></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24"
          ><div class="chart-card">
            <h3>预约高峰时段分析</h3>
            <div ref="peakHoursChart" style="height: 300px"></div></div
        ></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24"
          ><div class="chart-card">
            <h3>预约等待时长趋势</h3>
            <div ref="waitTimeTrendChart" style="height: 300px"></div></div
        ></el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24"
          ><div class="chart-card">
            <h3>用户活跃度统计</h3>
            <div ref="userActivityChart" style="height: 300px"></div></div
        ></el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  getDashboard,
  getDeviceStatus,
  getDeviceType,
  getBookingTrend,
  getPeakHours,
  getAvgWaitTime,
  getWaitTimeTrend,
  getUserActivity,
  getUserStatistics,
  getDeviceUsageAnalysis,
  getBookingAnalysis,
  getMaintenanceStatistics,
  getMaintainerStats,
  exportExcel,
  exportPdf,
  exportReport,
  getUserProfile,
  getCalibrationRate,
  getBookingPrediction
} from '../../api/statistics';
import { Download } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';
import { getMyBookings, getBookingList } from '@/api/booking';
import PredictionChart from '../../components/PredictionChart.vue';

const userStore = useUserStore();
const getUserType = () => userStore.userInfo?.userType || userStore.userType || '';
const userType = ref(getUserType());

// 监听用户信息变化，更新 userType
watch(() => userStore.userInfo?.userType, (newType) => {
  userType.value = newType || '';
});

const isAdmin = computed(() => getUserType() === 'SYSTEM_ADMIN');
const isTeacher = computed(() => getUserType() === 'TEACHER');
const isMaintainer = computed(() => getUserType() === 'MAINTAINER');
const isLabAdmin = computed(() => getUserType() === 'LAB_ADMIN');
const isStudent = computed(() => getUserType() === 'STUDENT');

// 确保页面初始化时角色检测正确
const initRoleData = async () => {
  // 如果 userType 为空，尝试从 store 获取
  if (!getUserType() && userStore.userInfo?.userType) {
    userType.value = userStore.userInfo.userType;
  }
  // 根据角色加载数据
  if (isAdmin.value) {
    activeTab.value = 'user';
    await loadAdminData();
  } else if (isMaintainer.value) {
    await loadMaintainerStats();
    await fetchDashboard();
    await fetchAvgWaitTime();
    await loadCalibrationRate();
    await nextTick();
    await initCommonCharts();
    setTimeout(() => initMaintainerCharts(), 50);
  } else if (isLabAdmin.value) {
    // 实验室管理员仅加载与其职责匹配的分析视图（不加载系统管理员专属用户统计）
    activeTab.value = 'device';
    await loadLabAdminData();
    await fetchDashboard();
    await fetchAvgWaitTime();
    await loadCalibrationRate();
    await nextTick();
    await initCommonCharts();
  } else if (isTeacher.value) {
    await loadTeacherScopeStats();
    await fetchDashboard();
    await fetchAvgWaitTime();
    await nextTick();
    await initCommonCharts();
  } else {
    // 普通用户（学生等）只加载基础数据
    await fetchDashboard();
    await fetchAvgWaitTime();
    await nextTick();
    await initCommonCharts();
  }
};

// ========== 维护人员数据加载 ==========
const loadMaintainerStats = async () => {
  try {
    const res = await getMaintainerStats();
    maintStats.processing = res.processing ?? 0;
    maintStats.weekCompleted = res.weekCompleted ?? 0;
    maintStats.monthCompleted = res.monthCompleted ?? 0;
    maintStats.avgRepairHours = res.avgRepairHours ?? 0;
    maintStats.monthCost = res.monthCost ?? 0;
    maintStats.monthCalibration = res.monthCalibration ?? 0;
    maintStats.faultTypes = res.faultTypes ?? [];
    // 校准趋势图依赖 loadCalibrationRate 先完成，不在此处 initMaintainerCharts（避免竞态）
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || e?.response?.data?.message || '维护统计接口异常，请检查登录身份与后端日志');
  }
};

const initMaintainerCharts = () => {
  if (faultTypeChartRef.value) {
    disposeChartDom(faultTypeChartRef.value);
    const c = echarts.init(faultTypeChartRef.value);
    const ft = maintStats.faultTypes || [];
    c.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '70%'], center: ['50%', '50%'],
        data: ft.map((f, i) => ({
          name: f.name, value: f.count,
          itemStyle: { color: ['#FF4757','#FF9500','#7B61FF','#00D4FF','#00FF88','#F5A623'][i % 6] }
        })),
        label: { color: '#8B949E' }
      }]
    });
    c.resize();
  }
  if (maintCalibrationChartRef.value) {
    const res = calibrationRate;
    disposeChartDom(maintCalibrationChartRef.value);
    const c = echarts.init(maintCalibrationChartRef.value);
    c.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
      legend: { data: ['校准达标率'], bottom: '0%', textStyle: { color: '#8B949E' } },
      grid: { left: '3%', right: '4%', top: '10%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: res.monthsList, axisLabel: { color: '#8B949E' }, axisLine: { lineStyle: { color: '#30363D' } } },
      yAxis: { type: 'value', name: '达标率(%)', min: 0, max: 100, axisLabel: { color: '#8B949E', formatter: '{value}%' }, axisLine: { lineStyle: { color: '#30363D' } }, splitLine: { lineStyle: { color: '#21262D' } } },
      series: [{
        name: '校准达标率', type: 'line',
        data: res.rateList,
        itemStyle: { color: '#67C23A' }, lineStyle: { width: 3 },
        areaStyle: { color: 'rgba(103,194,58,0.15)' },
        label: { show: true, formatter: '{c}%', color: '#67C23A', position: 'top' }
      }]
    });
    c.resize();
  }
};

const pageTitle = computed(() => {
  if (isAdmin.value) return '数据分析报表';
  if (isTeacher.value) return '实验数据分析（教师）';
  if (isMaintainer.value) return '故障统计分析';
  if (isLabAdmin.value) return '数据分析报表';
  return '数据分析报表';
});

/** 教师页顶部指标：与「学生预约审核」列表范围一致，避免误用全系统看板数字 */
const teacherScopeStats = reactive({
  myBookings: 0,
  pendingStudent: 0,
  myCompleted: 0
});

const loadTeacherScopeStats = async () => {
  try {
    const [myRes, pendingRes, completedRes] = await Promise.all([
      getMyBookings({ pageNum: 1, pageSize: 1 }),
      getBookingList({ pageNum: 1, pageSize: 1, status: 0 }),
      getMyBookings({ pageNum: 1, pageSize: 1, status: 3 })
    ]);
    teacherScopeStats.myBookings = myRes.total ?? 0;
    teacherScopeStats.pendingStudent = pendingRes.total ?? 0;
    teacherScopeStats.myCompleted = completedRes.total ?? 0;
  } catch (e) {
    console.error(e);
  }
};

const activeTab = ref('user');
const exportLoading = ref(false);

// ========== 系统管理员专用统计数据 ==========
const userStats = reactive({
  total: 0, systemAdmin: 0, labAdmin: 0, teacher: 0, student: 0,
  maintainer: 0, activeWeek: 0, activeMonth: 0, newMonth: 0
});
/** 最近一次各 Tab 统计接口完整结果（隐藏 Tab 内图表首屏宽度为 0，需在切换 Tab 后重绘） */
const lastUserStatsPayload = ref(null);
const lastDeviceStatsPayload = ref(null);
const lastBookingStatsPayload = ref(null);
const lastMaintStatsPayload = ref(null);
const deviceStats = reactive({
  total: 0, idle: 0, using: 0, maintaining: 0,
  calibrating: 0, scrapped: 0, usageRate: 0, avgUsageRate: 0
});
const bookingStats = reactive({
  weekTotal: 0, todayTotal: 0, successRate: 0, cancelRate: 0,
  pending: 0, avgAuditHours: 0, avgWaitHours: 0, monthTotal: 0
});
const maintStats = reactive({
  weekCount: 0, monthCount: 0, pending: 0, processing: 0,
  avgRepairHours: 0, monthCost: 0, weekCalibration: 0, monthCalibration: 0,
  weekCompleted: 0, monthCompleted: 0, faultTypes: []
});
const idleDevices = ref([]);

// ========== 其他角色统计数据 ==========
const stats = reactive({ totalDevices: 0, idleDevices: 0, usingDevices: 0, todayBookings: 0 });
const waitStats = reactive({ avgWaitTime: '0.00', period: 'week' });

// ========== 用户画像数据 ==========
const userProfile = reactive({
  summary: {
    totalUsers: 0,
    activeUsers: 0,
    avgSkillLevel: 0,
    mostPopularExpType: '无数据'
  },
  experimentType: [],
  skillLevel: [],
  userUsageRanking: [],
  radarData: []
});

// 校准达标率数据
const calibrationRate = reactive({
  total: 0,
  passed: 0,
  rate: 0,
  months: 6,
  monthsList: [],
  totalList: [],
  passedList: [],
  rateList: []
});

const scopedBookingTrend = ref(null);
const scopedPeakHours = ref(null);

// ========== 图表 ref ==========
const deviceStatusChartRef = ref(null);
const deviceTypeChartRef = ref(null);
const hotDeviceChartRef = ref(null);
const bookingTrendChartRef = ref(null);
const bookingStatusChartRef = ref(null);
const peakHoursChartRef = ref(null);
const faultTypeChartRef = ref(null);
const repairTrendChartRef = ref(null);
const costChartRef = ref(null);
const userTrendChartRef = ref(null);
const userRoleChartRef = ref(null);

// 用户画像图表 ref
const expTypeChartRef = ref(null);
const skillLevelChartRef = ref(null);
const userAbilityRadarChartRef = ref(null);
const userUsageRankChartRef = ref(null);

// 校准达标率图表 ref
const calibrationRateChartRef = ref(null);
const calibrationRateChart = ref(null);
const maintCalibrationChartRef = ref(null);

const deviceStatusChart = ref(null);
const deviceTypeChart = ref(null);
const bookingTrendChart = ref(null);
const peakHoursChart = ref(null);
const waitTimeTrendChart = ref(null);
const userActivityChart = ref(null);

// ========== 通用导出 ==========
const downloadBlob = (blob, filename) => {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = filename; a.click();
  window.URL.revokeObjectURL(url);
};

const handleExportExcel = async () => {
  exportLoading.value = true;
  try {
    let res;
    if (isAdmin.value) {
      res = await exportReport({ type: activeTab.value, format: 'excel' });
    } else if (isMaintainer.value) {
      console.log('维护人员导出Excel，参数：', { type: 'maintenance', format: 'excel' });
      res = await exportReport({ type: 'maintenance', format: 'excel' });
      console.log('导出响应：', res);
    } else {
      res = await exportExcel();
    }
    
    if (!res || (res instanceof Blob && res.size === 0)) {
      throw new Error('导出的文件为空');
    }
    
    downloadBlob(res, `数据分析_${activeTab.value}_${new Date().toISOString().slice(0, 10)}.xlsx`);
    ElMessage.success('Excel导出成功');
  } catch (e) {
    console.error('导出失败详情：', e);
    ElMessage.error('导出失败：' + (e.message || '未知错误'));
  } finally {
    exportLoading.value = false;
  }
};

const handleExportPdf = async () => {
  exportLoading.value = true;
  try {
    let res;
    if (isAdmin.value) {
      res = await exportReport({ type: activeTab.value, format: 'pdf' });
    } else if (isMaintainer.value) {
      console.log('维护人员导出PDF，参数：', { type: 'maintenance', format: 'pdf' });
      res = await exportReport({ type: 'maintenance', format: 'pdf' });
      console.log('导出响应：', res);
    } else {
      res = await exportPdf();
    }
    
    if (!res || (res instanceof Blob && res.size === 0)) {
      throw new Error('导出的文件为空');
    }
    
    downloadBlob(res, `数据分析_${activeTab.value}_${new Date().toISOString().slice(0, 10)}.pdf`);
    ElMessage.success('PDF导出成功');
  } catch (e) {
    console.error('导出失败详情：', e);
    ElMessage.error('导出失败：' + (e.message || '未知错误'));
  } finally {
    exportLoading.value = false;
  }
};

const handlePeriodChange = () => fetchAvgWaitTime();

// ========== 通用数据加载 ==========
const fetchDashboard = async () => {
  try {
    const res = await getDashboard();
    Object.assign(stats, res);
  } catch (e) { console.error(e); }
};

const fetchAvgWaitTime = async () => {
  try {
    const res = await getAvgWaitTime(waitStats.period);
    waitStats.avgWaitTime = res.avgWaitTime || '0.00';
  } catch (e) { console.error(e); }
};

const normalizeDateKey = (v) => {
  if (!v) return '';
  const s = String(v);
  if (s.length >= 10 && s[4] === '-' && s[7] === '-') return s.slice(0, 10);
  const d = new Date(s);
  if (Number.isNaN(d.getTime())) return '';
  return d.toISOString().slice(0, 10);
};

const normalizeHourKey = (v) => {
  if (!v) return '';
  const s = String(v).trim();
  const h = s.split(':')[0];
  const n = Number(h);
  if (!Number.isFinite(n) || n < 0 || n > 23) return '';
  return `${String(n).padStart(2, '0')}:00`;
};

const buildScopedBookingCharts = async () => {
  scopedBookingTrend.value = null;
  scopedPeakHours.value = null;
  let rows = [];
  try {
    if (isTeacher.value) {
      const res = await getBookingList({ pageNum: 1, pageSize: 1000 });
      rows = res?.list || [];
    } else if (isStudent.value) {
      const res = await getMyBookings({ pageNum: 1, pageSize: 1000 });
      rows = res?.list || [];
    } else {
      return;
    }
  } catch (e) {
    console.error(e);
    return;
  }
  const dateMap = new Map();
  const hourMap = new Map();
  const allHours = ['08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00'];
  allHours.forEach((h) => hourMap.set(h, 0));

  rows.forEach((r) => {
    const d = normalizeDateKey(r.bookingDate);
    if (d) dateMap.set(d, (dateMap.get(d) || 0) + 1);
    const hk = normalizeHourKey(r.startTime);
    if (hk && hourMap.has(hk)) hourMap.set(hk, (hourMap.get(hk) || 0) + 1);
  });
  const dates = [];
  const counts = [];
  for (let i = 6; i >= 0; i--) {
    const d = new Date();
    d.setDate(d.getDate() - i);
    const key = d.toISOString().slice(0, 10);
    dates.push(key);
    counts.push(dateMap.get(key) || 0);
  }
  scopedBookingTrend.value = { dates, counts };
  scopedPeakHours.value = { hours: allHours, counts: allHours.map((h) => hourMap.get(h) || 0) };
};

const initCommonCharts = async () => {
  try {
    await buildScopedBookingCharts();
    const [statusRes, typeRes, trendResRaw, peakResRaw] = await Promise.all([
      getDeviceStatus(), getDeviceType(), getBookingTrend(), getPeakHours()
    ]);
    const trendRes = scopedBookingTrend.value || trendResRaw;
    const peakRes = scopedPeakHours.value || peakResRaw;

    if (deviceStatusChart.value) {
      const c = echarts.init(deviceStatusChart.value);
      c.setOption({
        tooltip: { trigger: 'item' },
        series: [{ type: 'pie', radius: '50%', data: [
          { value: statusRes.idle||0, name:'空闲', itemStyle:{color:'#00FF88'} },
          { value: statusRes.using||0, name:'使用中', itemStyle:{color:'#00D4FF'} },
          { value: statusRes.maintaining||0, name:'维修中', itemStyle:{color:'#FF9500'} },
          { value: statusRes.calibrating||0, name:'校准中', itemStyle:{color:'#7B61FF'} },
          { value: statusRes.scrapped||0, name:'报废', itemStyle:{color:'#FF4757'} }
        ], label:{ color:'#8B949E' } }]
      });
    }

    if (deviceTypeChart.value) {
      const c = echarts.init(deviceTypeChart.value);
      c.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: typeRes.categories||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
        yAxis: { type: 'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
        series: [{ type: 'bar', data: typeRes.counts||[], itemStyle:{ color: new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#00D4FF'},{offset:1,color:'#7B61FF'}]) }, barWidth:15, borderRadius:[0,4,4,0] }]
      });
    }

    if (bookingTrendChart.value) {
      const c = echarts.init(bookingTrendChart.value);
      c.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: trendRes.dates||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
        yAxis: { type: 'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
        series: [{ type: 'line', data: trendRes.counts||[], smooth:true, lineStyle:{ color:'#00D4FF', width:3 }, areaStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,212,255,0.3)'},{offset:1,color:'rgba(0,212,255,0)'}])} }]
      });
    }

    if (peakHoursChart.value) {
      const c = echarts.init(peakHoursChart.value);
      c.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: peakRes.hours||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
        yAxis: { type: 'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
        series: [{ type: 'bar', data: peakRes.counts||[], itemStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#FF9500'},{offset:1,color:'#FF4757'}]) }, barWidth:15, borderRadius:[4,4,0,0] }]
      });
    }

    if (waitTimeTrendChart.value) {
      const waitRes = await getWaitTimeTrend(7);
      const c = echarts.init(waitTimeTrendChart.value);
      c.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: waitRes.dates||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
        yAxis: { type: 'value', name:'小时', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
        series: [{ type: 'line', data: waitRes.waitTimes||[], smooth:true, areaStyle:{ opacity:0.3 }, itemStyle:{ color:'#67C23A' } }]
      });
    }

    if (userActivityChart.value) {
      const actRes = await getUserActivity('week',7,10);
      const actData = actRes.activities||[];
      const c = echarts.init(userActivityChart.value);
      c.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data:['登录次数','预约次数'], textStyle:{ color:'#8B949E' } },
        xAxis: { type:'category', data: actData.map(i=>i.realName||i.username), axisLabel:{ color:'#8B949E', rotate:30 } },
        yAxis: { type:'value', axisLabel:{ color:'#8B949E' } },
        series: [
          { name:'登录次数', type:'bar', data: actData.map(i=>i.loginCount||0), itemStyle:{ color:'#409EFF' } },
          { name:'预约次数', type:'bar', data: actData.map(i=>i.bookingCount||0), itemStyle:{ color:'#67C23A' } }
        ]
      });
    }
  } catch (e) { console.error(e); }
};

// ========== 系统管理员数据加载 ==========
const loadUserStats = async () => {
  if (!isAdmin.value) return;
  try {
    const res = await getUserStatistics();
    lastUserStatsPayload.value = res;
    userStats.total = res.total ?? 0;
    userStats.systemAdmin = res.systemAdmin ?? 0;
    userStats.labAdmin = res.labAdmin ?? 0;
    userStats.teacher = res.teacher ?? 0;
    userStats.student = res.student ?? 0;
    userStats.maintainer = res.maintainer ?? 0;
    userStats.activeWeek = res.activeWeek ?? 0;
    userStats.activeMonth = res.activeMonth ?? 0;
    userStats.newMonth = res.newMonth ?? 0;
    await nextTick();
    // Tab 内图表在部分布局下首帧宽度为 0，延迟一帧再初始化
    setTimeout(() => initUserCharts(res), 0);
  } catch (e) {
    console.error(e);
    const msg = e?.message || (e?.response?.data?.message) || '加载用户统计失败，请检查是否以系统管理员登录或接口是否正常';
    ElMessage.error(msg);
  }
};

function disposeChartDom (dom) {
  if (!dom) return;
  const inst = echarts.getInstanceByDom(dom);
  if (inst) inst.dispose();
}

const initUserCharts = (data) => {
  if (!data) return;
  if (userTrendChartRef.value) {
    disposeChartDom(userTrendChartRef.value);
    const c = echarts.init(userTrendChartRef.value);
    c.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type:'category', data: data.trendDates||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [
        { name:'活跃用户', type:'line', data: data.trendCounts||[], smooth:true, lineStyle:{ color:'#00D4FF', width:3 }, areaStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,212,255,0.3)'},{offset:1,color:'rgba(0,212,255,0)'}]) } }
      ]
    });
    c.resize();
  }
  if (userRoleChartRef.value) {
    disposeChartDom(userRoleChartRef.value);
    const c = echarts.init(userRoleChartRef.value);
    c.setOption({
      tooltip: { trigger:'item' },
      series: [{ type:'pie', radius:['45%','70%'], center:['50%','50%'], data:[
        { value: data.systemAdmin||0, name:'系统管理员', itemStyle:{ color:'#FF4757' } },
        { value: data.labAdmin||0, name:'实验室管理员', itemStyle:{ color:'#FF9500' } },
        { value: data.teacher||0, name:'教师', itemStyle:{ color:'#7B61FF' } },
        { value: data.student||0, name:'学生', itemStyle:{ color:'#00FF88' } },
        { value: data.maintainer||0, name:'设备维护人员', itemStyle:{ color:'#00D4FF' } }
      ], label:{ color:'#8B949E' } }]
    });
    c.resize();
  }
};

const onAdminTabChange = (name) => {
  // 非当前 Tab 内 DOM 常为 display:none，echarts 在 0 宽初始化会空白；切换后再绘并略延迟等布局完成
  if (name === 'user' && !isAdmin.value) {
    activeTab.value = 'device';
    return;
  }
  nextTick(() => {
    setTimeout(() => {
      if (name === 'user' && lastUserStatsPayload.value) initUserCharts(lastUserStatsPayload.value);
      if (name === 'device' && lastDeviceStatsPayload.value) initDeviceCharts(lastDeviceStatsPayload.value);
      if (name === 'booking' && lastBookingStatsPayload.value) initBookingCharts(lastBookingStatsPayload.value);
      if (name === 'maintenance' && lastMaintStatsPayload.value) initMaintCharts(lastMaintStatsPayload.value);
      if (name === 'user-profile') loadUserProfile();
      if (name === 'calibration') loadCalibrationRate();
      // 预约全量分析内的「预约趋势预测」需等 Tab 可见后再 resize
      if (name === 'booking') {
        setTimeout(() => window.dispatchEvent(new Event('resize')), 120);
      }
    }, 50);
  });
};

// ========== 用户画像数据加载 ==========
const loadUserProfile = async () => {
  try {
    const res = await getUserProfile();
    // 更新摘要数据
    if (res.summary) {
      userProfile.summary.totalUsers = res.summary.totalUsers || 0;
      userProfile.summary.activeUsers = res.summary.activeUsers || 0;
      userProfile.summary.avgSkillLevel = res.summary.avgSkillLevel || 0;
      userProfile.summary.mostPopularExpType = res.summary.mostPopularExpType || '无数据';
    }
    // 更新实验类型数据
    userProfile.experimentType = res.experimentType || [];
    // 更新技能等级数据
    userProfile.skillLevel = res.skillLevel || [];
    // 更新用户使用排行
    userProfile.userUsageRanking = res.userUsageRanking || [];
    // 更新雷达图数据
    userProfile.radarData = res.radarData || [];

    await nextTick();
    setTimeout(() => initUserProfileCharts(res), 0);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || e?.response?.data?.message || '加载用户画像数据失败');
  }
};

// 初始化用户画像图表
const initUserProfileCharts = (data) => {
  if (!data) return;

  // 实验类型饼图
  if (expTypeChartRef.value) {
    disposeChartDom(expTypeChartRef.value);
    const chart = echarts.init(expTypeChartRef.value);
    const expData = data.experimentType || [];
    const colorList = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#00d4ff'];
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { color: '#8B949E' } },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        label: { color: '#8B949E', fontSize: 12 },
        data: expData.map((item, index) => ({
          name: item.name,
          value: item.value,
          itemStyle: { color: colorList[index % colorList.length] }
        }))
      }]
    });
    chart.resize();
  }

  // 技能等级饼图
  if (skillLevelChartRef.value) {
    disposeChartDom(skillLevelChartRef.value);
    const chart = echarts.init(skillLevelChartRef.value);
    const skillData = data.skillLevel || [];
    chart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', right: '5%', top: 'center', textStyle: { color: '#8B949E' } },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['40%', '50%'],
        avoidLabelOverlap: false,
        label: { color: '#8B949E', fontSize: 12 },
        data: [
          { name: '初学(1级)', value: skillData.find(s => s.name === '初学(1级)')?.value || 0, itemStyle: { color: '#909399' } },
          { name: '一般(2级)', value: skillData.find(s => s.name === '一般(2级)')?.value || 0, itemStyle: { color: '#E6A23C' } },
          { name: '熟练(3级)', value: skillData.find(s => s.name === '熟练(3级)')?.value || 0, itemStyle: { color: '#67C23A' } }
        ]
      }]
    });
    chart.resize();
  }

  // 用户能力雷达图
  if (userAbilityRadarChartRef.value) {
    disposeChartDom(userAbilityRadarChartRef.value);
    const chart = echarts.init(userAbilityRadarChartRef.value);
    const radarData = data.radarData || [];
    chart.setOption({
      tooltip: {},
      legend: {
        bottom: '5%',
        textStyle: { color: '#8B949E' },
        data: radarData.map(item => item.name)
      },
      radar: {
        indicator: [
          { name: '使用频次', max: 100 },
          { name: '技能等级', max: 3 },
          { name: '活跃度', max: 100 }
        ],
        axisName: { color: '#8B949E' },
        splitLine: { lineStyle: { color: '#30363D' } },
        splitArea: { areaStyle: { color: ['#1a1f25', '#21262D'] } },
        axisLine: { lineStyle: { color: '#30363D' } }
      },
      series: [{
        type: 'radar',
        data: radarData.map((item, index) => ({
          value: item.value || [0, 0, 0],
          name: item.name,
          lineStyle: { color: ['#00D4FF', '#FF9500', '#7B61FF', '#00FF88', '#FF4757'][index % 5] },
          areaStyle: { color: ['#00D4FF', '#FF9500', '#7B61FF', '#00FF88', '#FF4757'][index % 5] + '40' }
        }))
      }]
    });
    chart.resize();
  }

  // 用户使用频率排行柱状图
  if (userUsageRankChartRef.value) {
    disposeChartDom(userUsageRankChartRef.value);
    const chart = echarts.init(userUsageRankChartRef.value);
    const ranking = data.userUsageRanking || [];
    chart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: {
        type: 'value',
        axisLabel: { color: '#8B949E' },
        axisLine: { lineStyle: { color: '#30363D' } },
        splitLine: { lineStyle: { color: '#21262D' } }
      },
      yAxis: {
        type: 'category',
        data: ranking.map(item => item.realName || item.username).reverse(),
        axisLabel: { color: '#8B949E' },
        axisLine: { lineStyle: { color: '#30363D' } }
      },
      series: [{
        type: 'bar',
        data: ranking.map(item => item.bookingCount || 0).reverse(),
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#00D4FF' },
            { offset: 1, color: '#7B61FF' }
          ]),
          borderRadius: [0, 4, 4, 0]
        },
        label: { show: true, position: 'right', color: '#8B949E', fontSize: 12 }
      }]
    });
    chart.resize();
  }
};

// ========== 校准达标率数据加载 ==========
const loadCalibrationRate = async () => {
  try {
    const res = await getCalibrationRate(6);
    calibrationRate.total = res.total || 0;
    calibrationRate.passed = res.passed || 0;
    calibrationRate.rate = res.rate || 0;
    calibrationRate.monthsList = res.months || [];
    calibrationRate.totalList = res.totalList || [];
    calibrationRate.passedList = res.passedList || [];
    calibrationRate.rateList = res.rateList || [];
    await nextTick();
    setTimeout(() => initCalibrationRateChart(), 0);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || e?.response?.data?.message || '加载校准达标率数据失败');
  }
};

// ========== 初始化校准达标率图表 ==========
const initCalibrationRateChart = () => {
  if (calibrationRateChartRef.value) {
    disposeChartDom(calibrationRateChartRef.value);
    const chart = echarts.init(calibrationRateChartRef.value);
    chart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' }
      },
      legend: {
        data: ['总校准次数', '达标次数', '达标率'],
        bottom: '0%',
        textStyle: { color: '#8B949E' }
      },
      grid: {
        left: '3%',
        right: '4%',
        top: '10%',
        bottom: '15%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: calibrationRate.monthsList,
        axisLabel: { color: '#8B949E' },
        axisLine: { lineStyle: { color: '#30363D' } }
      },
      yAxis: [
        {
          type: 'value',
          name: '次数',
          axisLabel: { color: '#8B949E' },
          axisLine: { lineStyle: { color: '#30363D' } },
          splitLine: { lineStyle: { color: '#21262D' } }
        },
        {
          type: 'value',
          name: '达标率(%)',
          min: 0,
          max: 100,
          axisLabel: {
            color: '#8B949E',
            formatter: '{value}%'
          },
          axisLine: { lineStyle: { color: '#30363D' } },
          splitLine: { show: false }
        }
      ],
      series: [
        {
          name: '总校准次数',
          type: 'bar',
          data: calibrationRate.totalList,
          itemStyle: { color: '#409EFF' },
          barWidth: '30%'
        },
        {
          name: '达标次数',
          type: 'bar',
          data: calibrationRate.passedList,
          itemStyle: { color: '#67C23A' },
          barWidth: '30%'
        },
        {
          name: '达标率',
          type: 'line',
          yAxisIndex: 1,
          data: calibrationRate.rateList,
          itemStyle: { color: '#E6A23C' },
          lineStyle: { width: 3 },
          label: {
            show: true,
            formatter: '{c}%',
            color: '#E6A23C',
            position: 'top'
          }
        }
      ]
    });
    chart.resize();
    calibrationRateChart.value = chart;
  }
};

const loadDeviceStats = async () => {
  try {
    const res = await getDeviceUsageAnalysis();
    lastDeviceStatsPayload.value = res;
    deviceStats.total = res.total || 0;
    deviceStats.idle = res.idle || 0;
    deviceStats.using = res.using || 0;
    deviceStats.maintaining = res.maintaining || 0;
    deviceStats.calibrating = res.calibrating || 0;
    deviceStats.scrapped = res.scrapped || 0;
    deviceStats.usageRate = res.usageRate || 0;
    deviceStats.avgUsageRate = res.avgUsageRate || 0;
    idleDevices.value = res.idleDevices || [];
    await nextTick();
    setTimeout(() => {
      if (activeTab.value === 'device') initDeviceCharts(res);
    }, 0);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || e?.response?.data?.message || '加载设备统计失败');
  }
};

const initDeviceCharts = (data) => {
  if (!data) return;
  if (deviceStatusChartRef.value) {
    disposeChartDom(deviceStatusChartRef.value);
    const c = echarts.init(deviceStatusChartRef.value);
    c.setOption({
      tooltip: { trigger:'item' },
      series: [{ type:'pie', radius:'55%', center:['50%','50%'], data:[
        { value: data.idle||0, name:'空闲', itemStyle:{ color:'#00FF88' } },
        { value: data.using||0, name:'使用中', itemStyle:{ color:'#00D4FF' } },
        { value: data.maintaining||0, name:'维修中', itemStyle:{ color:'#FF9500' } },
        { value: data.calibrating||0, name:'校准中', itemStyle:{ color:'#7B61FF' } },
        { value: data.scrapped||0, name:'已报废', itemStyle:{ color:'#FF4757' } }
      ], label:{ color:'#8B949E' } }]
    });
    c.resize();
  }
  if (deviceTypeChartRef.value) {
    const cats = data.categories||[], counts = data.categoryCounts||[];
    disposeChartDom(deviceTypeChartRef.value);
    const c = echarts.init(deviceTypeChartRef.value);
    c.setOption({
      tooltip: { trigger:'axis' },
      xAxis: { type:'category', data:cats, axisLabel:{ color:'#8B949E', rotate:30 }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [{ type:'bar', data:counts, itemStyle:{ color: new echarts.graphic.LinearGradient(0,0,1,0,[{offset:0,color:'#00D4FF'},{offset:1,color:'#7B61FF'}]) }, barWidth:15, borderRadius:[0,4,4,0] }]
    });
    c.resize();
  }
  if (hotDeviceChartRef.value) {
    const top = data.hotDevices||[];
    disposeChartDom(hotDeviceChartRef.value);
    const c = echarts.init(hotDeviceChartRef.value);
    c.setOption({
      tooltip: { trigger:'axis' },
      xAxis: { type:'category', data: top.map(i=>i.deviceName || i.device_name || '-'), axisLabel:{ color:'#8B949E', rotate:30 }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [{ type:'bar', data: top.map(i=>i.bookingCount ?? i.booking_count ?? 0), itemStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#FF9500'},{offset:1,color:'#FF4757'}]) }, barWidth:20, borderRadius:[4,4,0,0] }]
    });
    c.resize();
  }
};

const loadBookingStats = async () => {
  try {
    const res = await getBookingAnalysis();
    lastBookingStatsPayload.value = res;
    bookingStats.weekTotal = res.weekTotal || 0;
    bookingStats.todayTotal = res.todayTotal || 0;
    bookingStats.successRate = res.successRate || 0;
    bookingStats.cancelRate = res.cancelRate || 0;
    bookingStats.pending = res.pending || 0;
    bookingStats.avgAuditHours = res.avgAuditHours || 0;
    bookingStats.avgWaitHours = res.avgWaitHours || 0;
    bookingStats.monthTotal = res.monthTotal || 0;
    await nextTick();
    setTimeout(() => {
      if (activeTab.value === 'booking') initBookingCharts(res);
    }, 0);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || e?.response?.data?.message || '加载预约统计失败');
  }
};

const initBookingCharts = (data) => {
  if (!data) return;
  if (bookingTrendChartRef.value) {
    disposeChartDom(bookingTrendChartRef.value);
    const c = echarts.init(bookingTrendChartRef.value);
    c.setOption({
      tooltip: { trigger:'axis' },
      xAxis: { type:'category', data: data.dates||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [{ type:'line', data: data.counts||[], smooth:true, lineStyle:{ color:'#00D4FF', width:3 }, areaStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(0,212,255,0.3)'},{offset:1,color:'rgba(0,212,255,0)'}]) } }]
    });
    c.resize();
  }
  if (bookingStatusChartRef.value) {
    disposeChartDom(bookingStatusChartRef.value);
    const c = echarts.init(bookingStatusChartRef.value);
    c.setOption({
      tooltip: { trigger:'item' },
      series: [{ type:'pie', radius:'55%', data:[
        { value: data.pending||0, name:'待审核', itemStyle:{ color:'#7B61FF' } },
        { value: data.approved||0, name:'已通过', itemStyle:{ color:'#00FF88' } },
        { value: data.rejected||0, name:'已拒绝', itemStyle:{ color:'#FF4757' } },
        { value: data.completed||0, name:'已完成', itemStyle:{ color:'#00D4FF' } },
        { value: data.cancelled||0, name:'已取消', itemStyle:{ color:'#FF9500' } }
      ], label:{ color:'#8B949E' } }]
    });
    c.resize();
  }
  if (peakHoursChartRef.value) {
    disposeChartDom(peakHoursChartRef.value);
    const c = echarts.init(peakHoursChartRef.value);
    c.setOption({
      tooltip: { trigger:'axis' },
      xAxis: { type:'category', data: data.peakHours||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [{ type:'bar', data: data.peakCounts||[], itemStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#FF9500'},{offset:1,color:'#FF4757'}]) }, barWidth:15, borderRadius:[4,4,0,0] }]
    });
    c.resize();
  }
};

const loadMaintStats = async () => {
  try {
    const res = await getMaintenanceStatistics();
    lastMaintStatsPayload.value = res;
    maintStats.weekCount = res.weekCount || 0;
    maintStats.monthCount = res.monthCount || 0;
    maintStats.pending = res.pending || 0;
    maintStats.processing = res.processing || 0;
    maintStats.avgRepairHours = res.avgRepairHours || 0;
    maintStats.monthCost = res.monthCost || 0;
    maintStats.weekCalibration = res.weekCalibration || 0;
    maintStats.monthCalibration = res.monthCalibration || 0;
    await nextTick();
    setTimeout(() => {
      if (activeTab.value === 'maintenance') initMaintCharts(res);
    }, 0);
  } catch (e) {
    console.error(e);
    ElMessage.error(e?.message || e?.response?.data?.message || '加载维护统计失败');
  }
};

const initMaintCharts = (data) => {
  if (!data) return;
  if (faultTypeChartRef.value) {
    disposeChartDom(faultTypeChartRef.value);
    const c = echarts.init(faultTypeChartRef.value);
    c.setOption({
      tooltip: { trigger:'item' },
      series: [{ type:'pie', radius:['40%','70%'], center:['50%','50%'], data: (data.faultTypes||[]).map((f,i)=>({ name:f.name, value:f.count, itemStyle:{ color: ['#FF4757','#FF9500','#7B61FF','#00D4FF','#00FF88'][i%5] } })), label:{ color:'#8B949E' } }]
    });
    c.resize();
  }
  if (repairTrendChartRef.value) {
    disposeChartDom(repairTrendChartRef.value);
    const c = echarts.init(repairTrendChartRef.value);
    c.setOption({
      tooltip: { trigger:'axis' },
      xAxis: { type:'category', data: data.repairDates||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', name:'小时', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [{ type:'line', data: data.repairDurations||[], smooth:true, lineStyle:{ color:'#FF4757', width:3 }, areaStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'rgba(255,71,87,0.3)'},{offset:1,color:'rgba(255,71,87,0)'}]) } }]
    });
    c.resize();
  }
  if (costChartRef.value) {
    disposeChartDom(costChartRef.value);
    const c = echarts.init(costChartRef.value);
    c.setOption({
      tooltip: { trigger:'axis' },
      xAxis: { type:'category', data: data.costDates||[], axisLabel:{ color:'#8B949E' }, axisLine:{ lineStyle:{ color:'#30363D' } } },
      yAxis: { type:'value', name:'元', axisLabel:{ color:'#8B949E' }, splitLine:{ lineStyle:{ color:'#21262D' } } },
      series: [{ type:'bar', data: data.costAmounts||[], itemStyle:{ color: new echarts.graphic.LinearGradient(0,0,0,1,[{offset:0,color:'#FF9500'},{offset:1,color:'#FF4757'}]) }, barWidth:20, borderRadius:[4,4,0,0] }]
    });
    c.resize();
  }
};

const loadAdminData = () => {
  loadUserStats();
  loadDeviceStats();
  loadBookingStats();
  loadMaintStats();
};

const loadLabAdminData = () => {
  // 不请求系统管理员专属接口 /statistics/user，避免403
  loadDeviceStats();
  loadBookingStats();
  loadMaintStats();
};

// 用户信息来自 localStorage 时首屏即可判定；若后续刷新用户信息也可重新拉取统计
watch(isAdmin, (v) => {
  if (v) loadAdminData();
}, { immediate: true });

onMounted(async () => {
  await initRoleData();
});

// 监听实验室管理员状态变化，加载数据
watch(isLabAdmin, async (val) => {
  if (val) {
    activeTab.value = 'device';
    await loadLabAdminData();
    await fetchDashboard();
    await fetchAvgWaitTime();
    await loadCalibrationRate();
    await nextTick();
    await initCommonCharts();
  }
});

// 监听维护人员状态变化，加载数据
watch(isMaintainer, async (val) => {
  if (val) {
    await loadMaintainerStats();
    await fetchDashboard();
    await fetchAvgWaitTime();
    await loadCalibrationRate();
    await nextTick();
    await initCommonCharts();
    setTimeout(() => initMaintainerCharts(), 50);
  }
});

// 监听教师状态变化，加载数据
watch(isTeacher, async (val) => {
  if (val) {
    await loadTeacherScopeStats();
    await fetchDashboard();
    await fetchAvgWaitTime();
    await nextTick();
    await initCommonCharts();
  }
});
</script>

<style scoped>
.statistics-container {
  color: #e6edf3;
}
.teacher-stats-hint {
  margin-bottom: 16px;
}
.page-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.page-header h2 {
  margin: 0;
  color: #e6edf3;
}
.export-btns {
  display: flex;
  gap: 10px;
}

.stats-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  background: #161b22;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  border: 1px solid #30363d;
}
.stat-card.highlight {
  border-color: #67c23a;
  background: linear-gradient(135deg, #1a2f1a 0%, #162016 100%);
}
.stat-title {
  color: #8b949e;
  font-size: 14px;
  margin-bottom: 10px;
}
.stat-value {
  color: #00d4ff;
  font-size: 32px;
  font-weight: bold;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.chart-row .full {
  grid-column: 1 / -1;
}
.chart-card {
  background: #161b22;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #30363d;
}
.chart-card h3 {
  color: #e6edf3;
  margin: 0 0 15px 0;
  font-size: 15px;
}

.admin-tabs :deep(.el-tabs__header) {
  background: #161b22;
  border-radius: 8px 8px 0 0;
  padding: 0 16px;
  border: 1px solid #30363d;
  border-bottom: none;
}
.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}
.admin-tabs :deep(.el-tabs__item) {
  color: #8b949e;
}
.admin-tabs :deep(.el-tabs__item.is-active) {
  color: #00d4ff;
}
.admin-tabs :deep(.el-tabs__active-bar) {
  background-color: #00d4ff;
}
.admin-tabs :deep(.el-tabs__content) {
  background: transparent;
  padding: 20px 0 0;
}

.stat-card .stat-unit {
  font-size: 16px;
  color: #8b949e;
}
.stat-card .wait-time {
  color: #67c23a;
  font-size: 36px;
  margin: 10px 0;
}
.period-selector {
  margin-top: 10px;
}
.period-selector :deep(.el-radio-button__inner) {
  background: #0d1117;
  border-color: #30363d;
  color: #8b949e;
}
.period-selector :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #67c23a;
  border-color: #67c23a;
  color: #fff;
}
</style>
