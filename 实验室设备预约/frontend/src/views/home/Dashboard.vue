<template>
  <div class="dashboard">
    <!-- ==================== 系统管理员 / 自定义角色（数据权限全开时与全局看板一致） ==================== -->
    <div v-if="showAdminDashboard">
      <div v-if="isCustomRole" class="custom-role-banner">
        <p class="sub">
          当前为自定义角色（{{ userStore.userInfo.userType }}），以下为核心运营数据概览；详细功能请从左侧菜单进入。
        </p>
      </div>
      <!-- 第一行：运营核心指标（4张，均可点击跳转） -->
      <div class="stats-grid admin-stats-grid">
        <div class="stat-card" @click="$router.push('/user')" title="点击查看用户管理">
          <div class="stat-icon" style="background: linear-gradient(135deg, #00D4FF, #7B61FF)">
            <el-icon :size="24"><User /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[0].value }}</div>
            <div class="stat-title">系统用户总数</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="stat-card" @click="$router.push('/device')" title="点击查看设备列表">
          <div class="stat-icon" style="background: linear-gradient(135deg, #FF9500, #FF4757)">
            <el-icon :size="24"><Monitor /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[1].value }}</div>
            <div class="stat-title">设备总数</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #00FF88, #00D4FF)">
            <el-icon :size="24"><Calendar /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[2].value }}</div>
            <div class="stat-title">预约总数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: linear-gradient(135deg, #7B61FF, #FF4757)">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[3].value }}</div>
            <div class="stat-title">今日预约</div>
          </div>
        </div>
      </div>
      <!-- 第二行：运维与待办指标（4张，均可点击跳转） -->
      <div class="stats-grid admin-stats-grid" style="margin-bottom: 24px">
        <div class="stat-card" @click="$router.push(isAdmin ? '/booking-global' : '/audit')" title="系统管理员查看全局预约；自定义角色进入审核">
          <div class="stat-icon" style="background: linear-gradient(135deg, #FF9500, #FF4757)">
            <el-icon :size="24"><Clock /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[4].value }}</div>
            <div class="stat-title">待审核预约</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="stat-card" @click="$router.push('/repair-global-monitor')" title="点击处理报修">
          <div class="stat-icon" style="background: linear-gradient(135deg, #FF4757, #FF9500)">
            <el-icon :size="24"><Tools /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[5].value }}</div>
            <div class="stat-title">待处理报修</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="stat-card" @click="$router.push('/repair-global-monitor')" title="点击查看工单">
          <div class="stat-icon" style="background: linear-gradient(135deg, #FF9500, #7B61FF)">
            <el-icon :size="24"><Setting /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[6].value }}</div>
            <div class="stat-title">处理中工单</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
        <div class="stat-card" @click="$router.push('/calibration')" title="点击查看校准">
          <div class="stat-icon" style="background: linear-gradient(135deg, #7B61FF, #00D4FF)">
            <el-icon :size="24"><TrendCharts /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ adminStats[7].value }}</div>
            <div class="stat-title">校准到期提醒</div>
          </div>
          <el-icon class="stat-arrow"><ArrowRight /></el-icon>
        </div>
      </div>
      <!-- 图表区域 -->
      <div class="charts-grid">
        <div class="chart-card">
          <div class="chart-title">预约趋势</div>
          <div class="chart-container" ref="trendChartRef"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">设备状态分布</div>
          <div class="chart-container" ref="statusChartRef"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">设备类型分布</div>
          <div class="chart-container" ref="typeChartRef"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">预约高峰时段</div>
          <div class="chart-container" ref="peakChartRef"></div>
        </div>
      </div>
      <div class="notice-card">
        <div class="notice-title">
          <el-icon><Bell /></el-icon> 系统公告
        </div>
        <div class="notice-content">
          <p v-if="isAdmin">
            欢迎使用实验室设备预约系统，当前登录身份：<strong>系统管理员</strong>。系统全局配置、数据备份与恢复、用户权限管理均在系统配置中完成。
          </p>
          <p v-else>
            欢迎使用实验室设备预约系统。自定义角色用户请通过左侧已授权菜单进入各业务模块；本页展示与系统一致的汇总指标（具体业务操作仍以菜单权限为准）。
          </p>
        </div>
      </div>
    </div>

    <!-- ==================== 实验室管理员首页 ==================== -->
    <div v-else-if="isLabAdmin">
      <div class="stats-grid">
        <div class="stat-card" v-for="stat in labAdminStats" :key="stat.title">
          <div class="stat-icon" :style="{ background: stat.gradient }">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </div>
      <!-- 待办事项 -->
      <div class="todo-grid">
        <div class="todo-card" @click="$router.push('/audit')">
          <el-icon :size="28" color="#FF9500"><Clock /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ labAdminStats.find(s => s.title === '待审核预约')?.value || 0 }}
            </div>
            <div class="todo-label">待审核预约</div>
          </div>
        </div>
        <div class="todo-card" @click="$router.push('/repair')">
          <el-icon :size="28" color="#FF4757"><Tools /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ labAdminStats.find(s => s.title === '待处理故障')?.value || 0 }}
            </div>
            <div class="todo-label">待处理故障</div>
          </div>
        </div>
        <div class="todo-card" @click="$router.push('/calibration')">
          <el-icon :size="28" color="#7B61FF"><Setting /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ labAdminStats.find(s => s.title === '待校准设备')?.value || 0 }}
            </div>
            <div class="todo-label">待校准设备</div>
          </div>
        </div>
        <div class="todo-card" @click="$router.push('/device-usage')">
          <el-icon :size="28" color="#00D4FF"><Document /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ labAdminStats.find(s => s.title === '今日使用记录')?.value || 0 }}
            </div>
            <div class="todo-label">设备使用记录</div>
          </div>
        </div>
      </div>
      <div class="charts-grid">
        <div class="chart-card">
          <div class="chart-title">实验室设备使用情况</div>
          <div class="chart-container" ref="labStatusChartRef"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">本周预约趋势</div>
          <div class="chart-container" ref="labTrendChartRef"></div>
        </div>
      </div>
    </div>

    <!-- ==================== 教师首页 ==================== -->
    <div v-else-if="isTeacher">
      <div class="stats-grid">
        <div class="stat-card" v-for="stat in teacherStats" :key="stat.title" @click="stat.route && $router.push(stat.route)">
          <div class="stat-icon" :style="{ background: stat.gradient }">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </div>
      <!-- 待办 -->
      <div class="todo-grid">
        <div class="todo-card" @click="$router.push('/audit')">
          <el-icon :size="28" color="#FF9500"><Clock /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ teacherStats.find(s => s.title === '待审核学生预约')?.value || 0 }}
            </div>
            <div class="todo-label">待审核学生预约</div>
          </div>
        </div>
        <div class="todo-card" @click="$router.push('/booking')">
          <el-icon :size="28" color="#00D4FF"><Calendar /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ teacherStats.find(s => s.title === '我的预约')?.value || 0 }}
            </div>
            <div class="todo-label">我的预约</div>
          </div>
        </div>
      </div>
      <div class="charts-grid">
        <div class="chart-card full">
          <div class="chart-title">本周预约趋势</div>
          <div class="chart-container" ref="teacherTrendChartRef"></div>
        </div>
      </div>
    </div>

    <!-- ==================== 学生首页 ==================== -->
    <div v-else-if="isStudent">
      <div class="stats-grid">
        <div class="stat-card" v-for="stat in studentStats" :key="stat.title" @click="stat.route && $router.push(stat.route)">
          <div class="stat-icon" :style="{ background: stat.gradient }">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </div>
      <div class="quick-links">
        <div class="quick-link-card" @click="$router.push('/device')">
          <el-icon :size="28"><Monitor /></el-icon>
          <span>浏览设备</span>
        </div>
        <div class="quick-link-card" @click="$router.push('/booking')">
          <el-icon :size="28"><Calendar /></el-icon>
          <span>我的预约</span>
        </div>
        <div class="quick-link-card" @click="$router.push('/ai-assistant')">
          <el-icon :size="28"><ChatDotRound /></el-icon>
          <span>AI助手</span>
        </div>
      </div>
      <div class="charts-grid">
        <div class="chart-card full">
          <div class="chart-title">本周预约趋势</div>
          <div class="chart-container" ref="trendChartRef"></div>
        </div>
      </div>
      <div class="recent-bookings">
        <h3 class="section-title">最近预约</h3>
        <el-table :data="recentBookings" stripe>
          <el-table-column prop="deviceName" label="设备" />
          <el-table-column prop="bookingDate" label="预约日期" width="120" />
          <el-table-column label="时段" width="120">
            <template #default="{ row }">{{ row.startTime }} - {{ row.endTime }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="getBookingStatusType(row.status)">{{
                getBookingStatusText(row.status)
              }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- ==================== 设备维护人员首页 ==================== -->
    <div v-else-if="isMaintainer">
      <div class="stats-grid">
        <div class="stat-card" v-for="stat in maintainerStats" :key="stat.title">
          <div class="stat-icon" :style="{ background: stat.gradient }">
            <el-icon :size="24"><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-title">{{ stat.title }}</div>
          </div>
        </div>
      </div>
      <!-- 待办 -->
      <div class="todo-grid">
        <div class="todo-card" @click="$router.push('/repair')">
          <el-icon :size="28" color="#FF9500"><Clock /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ maintainerStats.find(s => s.title === '待处理工单')?.value || 0 }}
            </div>
            <div class="todo-label">待处理工单</div>
          </div>
        </div>
        <div class="todo-card" @click="$router.push('/calibration')">
          <el-icon :size="28" color="#7B61FF"><Setting /></el-icon>
          <div class="todo-info">
            <div class="todo-num">
              {{ maintainerStats.find(s => s.title === '待校准任务')?.value || 0 }}
            </div>
            <div class="todo-label">待校准任务</div>
          </div>
        </div>
      </div>
      <div class="charts-grid">
        <div class="chart-card full">
          <div class="chart-title">设备状态分布</div>
          <div class="chart-container" ref="faultTypeChartRef"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue';
import * as echarts from 'echarts';
import { useUserStore } from '@/stores/user';
import { isBuiltinUserType } from '@/constants/userRoles';
import {
  getDashboard,
  getDeviceStatus,
  getBookingTrend,
  getDeviceType,
  getPeakHours,
  getMaintainerStats
} from '@/api/statistics';
import { getMyBookings, getBookingList } from '@/api/booking';
import {
  Monitor,
  Calendar,
  ChatDotRound,
  ChatLineSquare,
  TrendCharts,
  Clock,
  PieChart,
  Bell,
  Tools,
  Setting,
  User,
  Document,
  ArrowRight
} from '@element-plus/icons-vue';

const userStore = useUserStore();
const userType = computed(() => userStore.userInfo?.userType || '');
const isAdmin = computed(() => userType.value === 'SYSTEM_ADMIN');
const isCustomRole = computed(() => !!userType.value && !isBuiltinUserType(userType.value));
const showAdminDashboard = computed(() => isAdmin.value || isCustomRole.value);
const isLabAdmin = computed(() => userType.value === 'LAB_ADMIN');
const isTeacher = computed(() => userType.value === 'TEACHER');
const isStudent = computed(() => userType.value === 'STUDENT');
const isMaintainer = computed(() => userType.value === 'MAINTAINER');

// 图表 refs
const trendChartRef = ref(null);
const statusChartRef = ref(null);
const typeChartRef = ref(null);
const peakChartRef = ref(null);
const labStatusChartRef = ref(null);
const labTrendChartRef = ref(null);
const teacherTrendChartRef = ref(null);
const faultTypeChartRef = ref(null);

// ==================== 各角色统计数据 ====================
const adminStats = reactive([
  { title: '系统用户总数', value: 0 },
  { title: '设备总数', value: 0 },
  { title: '预约总数', value: 0 },
  { title: '今日预约', value: 0 },
  { title: '待审核预约', value: 0 },
  { title: '待处理报修', value: 0 },
  { title: '处理中工单', value: 0 },
  { title: '校准到期提醒', value: 0 }
]);

const labAdminStats = reactive([
  {
    title: '设备总数',
    value: 0,
    icon: 'Monitor',
    gradient: 'linear-gradient(135deg, #00D4FF, #7B61FF)'
  },
  {
    title: '今日预约',
    value: 0,
    icon: 'Calendar',
    gradient: 'linear-gradient(135deg, #00FF88, #00D4FF)'
  },
  {
    title: '待审核预约',
    value: 0,
    icon: 'Clock',
    gradient: 'linear-gradient(135deg, #FF9500, #FF4757)'
  },
  {
    title: '待处理故障',
    value: 0,
    icon: 'Tools',
    gradient: 'linear-gradient(135deg, #7B61FF, #FF4757)'
  },
  {
    title: '待校准设备',
    value: 0,
    icon: 'Setting',
    gradient: 'linear-gradient(135deg, #FF4757, #7B61FF)'
  },
  {
    title: '今日使用记录',
    value: 0,
    icon: 'Document',
    gradient: 'linear-gradient(135deg, #00D4FF, #00FF88)'
  }
]);

const teacherStats = reactive([
  {
    title: '我的预约',
    value: 0,
    icon: 'Calendar',
    gradient: 'linear-gradient(135deg, #00D4FF, #7B61FF)',
    route: '/booking'
  },
  {
    title: '待审核学生预约',
    value: 0,
    icon: 'Clock',
    gradient: 'linear-gradient(135deg, #FF9500, #FF4757)',
    route: '/audit'
  },
  {
    title: '已完成',
    value: 0,
    icon: 'TrendCharts',
    gradient: 'linear-gradient(135deg, #00FF88, #00D4FF)',
    route: '/booking'
  },
  {
    title: 'AI助手',
    value: '→',
    icon: 'ChatDotRound',
    gradient: 'linear-gradient(135deg, #7B61FF, #FF4757)',
    route: '/ai-assistant'
  }
]);

const studentStats = reactive([
  {
    title: '我的预约',
    value: 0,
    icon: 'Calendar',
    gradient: 'linear-gradient(135deg, #00D4FF, #7B61FF)',
    route: '/booking'
  },
  {
    title: '待审核',
    value: 0,
    icon: 'Clock',
    gradient: 'linear-gradient(135deg, #FF9500, #FF4757)',
    route: '/booking'
  },
  {
    title: '已完成',
    value: 0,
    icon: 'TrendCharts',
    gradient: 'linear-gradient(135deg, #00FF88, #00D4FF)',
    route: '/booking'
  },
  {
    title: 'AI助手',
    value: '→',
    icon: 'ChatDotRound',
    gradient: 'linear-gradient(135deg, #7B61FF, #FF4757)',
    route: '/ai-assistant'
  }
]);

const maintainerStats = reactive([
  {
    title: '待处理工单',
    value: 0,
    icon: 'Tools',
    gradient: 'linear-gradient(135deg, #FF9500, #FF4757)'
  },
  {
    title: '处理中',
    value: 0,
    icon: 'Clock',
    gradient: 'linear-gradient(135deg, #00D4FF, #7B61FF)'
  },
  {
    title: '待校准任务',
    value: 0,
    icon: 'Setting',
    gradient: 'linear-gradient(135deg, #7B61FF, #FF4757)'
  },
  {
    title: '本月完成',
    value: 0,
    icon: 'TrendCharts',
    gradient: 'linear-gradient(135deg, #00FF88, #00D4FF)'
  }
]);

const recentBookings = ref([]);

// ==================== 数据加载 ====================
const loadAdminDashboard = async () => {
  try {
    const [dashboard, statusRes, trendRes, typeRes, peakRes] = await Promise.all([
      getDashboard(),
      getDeviceStatus(),
      getBookingTrend(),
      getDeviceType(),
      getPeakHours()
    ]);
    // 第一行：系统用户总数、设备总数、预约总数、今日预约
    adminStats[0].value = dashboard.totalUsers || 0;
    adminStats[1].value = dashboard.totalDevices || 0;
    adminStats[2].value = dashboard.totalBookings || 0;
    adminStats[3].value = dashboard.todayBookings || 0;
    // 第二行：待审核预约、待处理报修、处理中工单、校准到期提醒
    adminStats[4].value = dashboard.pendingAudits || 0;
    adminStats[5].value = dashboard.pendingRepairs || 0;
    adminStats[6].value = dashboard.processingRepairs || 0;
    adminStats[7].value = dashboard.calibrationDueCount || 0;
    nextTick(() => initAdminCharts(statusRes, trendRes, typeRes, peakRes));
  } catch (e) {
    console.error(e);
  }
};

const loadLabAdminDashboard = async () => {
  try {
    const [dashboard, statusRes, trendRes] = await Promise.all([
      getDashboard(),
      getDeviceStatus(),
      getBookingTrend()
    ]);
    labAdminStats[0].value = dashboard.totalDevices || 0;
    labAdminStats[1].value = dashboard.todayBookings || 0;
    labAdminStats[2].value = dashboard.pendingAudits || 0;
    labAdminStats[3].value = dashboard.pendingRepairs || 0;
    // 待校准设备：优先使用pendingCalibrations，兼容calibrationDueCount
    labAdminStats[4].value = dashboard.pendingCalibrations || dashboard.calibrationDueCount || 0;
    // 今日使用记录：从todayUsage获取，如果没有则显示已完成预约数作为参考
    labAdminStats[5].value = dashboard.todayUsage || dashboard.completedTodayBookings || 0;
    nextTick(() => initLabAdminCharts(statusRes, trendRes));
  } catch (e) {
    console.error(e);
  }
};

const loadTeacherDashboard = async () => {
  try {
    const [myRes, pendingStudentRes, myCompletedRes] = await Promise.all([
      getMyBookings({ pageNum: 1, pageSize: 20 }),
      getBookingList({ pageNum: 1, pageSize: 1, status: 0 }),
      getMyBookings({ pageNum: 1, pageSize: 1, status: 3 })
    ]);
    const bookings = myRes.list || [];
    recentBookings.value = bookings.slice(0, 5);
    teacherStats[0].value = myRes.total ?? bookings.length;
    teacherStats[1].value = pendingStudentRes.total ?? 0;
    teacherStats[2].value = myCompletedRes.total ?? 0;
    const trendRes = await getBookingTrend();
    nextTick(() => initTeacherChart(trendRes));
  } catch (e) {
    console.error(e);
  }
};

const loadStudentDashboard = async () => {
  try {
    const [listRes, pendingRes, completedRes] = await Promise.all([
      getMyBookings({ pageNum: 1, pageSize: 20 }),
      getMyBookings({ pageNum: 1, pageSize: 1, status: 0 }),
      getMyBookings({ pageNum: 1, pageSize: 1, status: 3 })
    ]);
    const bookings = listRes.list || [];
    recentBookings.value = bookings.slice(0, 5);
    studentStats[0].value = listRes.total ?? bookings.length;
    studentStats[1].value = pendingRes.total ?? 0;
    studentStats[2].value = completedRes.total ?? 0;
    const trendRes = await getBookingTrend();
    nextTick(() => initStudentChart(trendRes));
  } catch (e) {
    console.error(e);
  }
};

const loadMaintainerDashboard = async () => {
  try {
    const [statsRes, dashboardRes, statusRes] = await Promise.all([
      getMaintainerStats(),
      getDashboard(),
      getDeviceStatus()
    ]);
    maintainerStats[0].value = dashboardRes.pendingRepairs || 0;
    maintainerStats[1].value = statsRes.processing || 0;
    maintainerStats[2].value = dashboardRes.pendingCalibrations || dashboardRes.calibrationDueCount || 0;
    maintainerStats[3].value = statsRes.monthCompleted || 0;
    nextTick(() => initFaultChart(statusRes));
  } catch (e) {
    console.error(e);
  }
};

// ==================== 图表初始化 ====================
const initStudentChart = (trendRes = {}) => {
  if (!trendChartRef.value) return;
  const chart = echarts.init(trendChartRef.value);
  chart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 40, right: 20, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: trendRes.dates || [],
      axisLine: { lineStyle: { color: '#30363D' } },
      axisLabel: { color: '#8B949E' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#30363D' } },
      axisLabel: { color: '#8B949E' },
      splitLine: { lineStyle: { color: '#21262D' } }
    },
    series: [
      {
        data: trendRes.counts || [],
        type: 'line',
        smooth: true,
        lineStyle: { color: '#00D4FF', width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0)' }
          ])
        },
        itemStyle: { color: '#00D4FF' }
      }
    ]
  });
};

const initAdminCharts = (statusRes = {}, trendRes = {}, typeRes = {}, peakRes = {}) => {
  if (!trendChartRef.value) return;
  const trendChart = echarts.init(trendChartRef.value);
  trendChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 40, right: 20, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: trendRes.dates || [],
      axisLine: { lineStyle: { color: '#30363D' } },
      axisLabel: { color: '#8B949E' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#30363D' } },
      axisLabel: { color: '#8B949E' },
      splitLine: { lineStyle: { color: '#21262D' } }
    },
    series: [
      {
        data: trendRes.counts || [],
        type: 'line',
        smooth: true,
        lineStyle: { color: '#00D4FF', width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0)' }
          ])
        },
        itemStyle: { color: '#00D4FF' }
      }
    ]
  });

  if (statusChartRef.value) {
    const statusChart = echarts.init(statusChartRef.value);
    statusChart.setOption({
      backgroundColor: 'transparent',
      series: [
        {
          type: 'pie',
          radius: ['50%', '70%'],
          center: ['50%', '50%'],
          data: [
            { value: statusRes.idle || 0, name: '空闲', itemStyle: { color: '#00FF88' } },
            { value: statusRes.using || 0, name: '使用中', itemStyle: { color: '#00D4FF' } },
            { value: statusRes.maintaining || 0, name: '维修中', itemStyle: { color: '#FF9500' } },
            { value: statusRes.calibrating || 0, name: '校准中', itemStyle: { color: '#7B61FF' } },
            { value: statusRes.scrapped || 0, name: '报废', itemStyle: { color: '#FF4757' } }
          ],
          label: { color: '#8B949E' }
        }
      ]
    });
  }

  if (typeChartRef.value) {
    const typeChart = echarts.init(typeChartRef.value);
    const cats = typeRes.categories || [],
      counts = typeRes.counts || [];
    typeChart.setOption({
      backgroundColor: 'transparent',
      grid: { top: 20, right: 20, bottom: 30, left: 80 },
      xAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#30363D' } },
        axisLabel: { color: '#8B949E' },
        splitLine: { lineStyle: { color: '#21262D' } }
      },
      yAxis: {
        type: 'category',
        data: cats,
        axisLine: { lineStyle: { color: '#30363D' } },
        axisLabel: { color: '#8B949E' }
      },
      series: [
        {
          type: 'bar',
          data: counts,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: '#00D4FF' },
              { offset: 1, color: '#7B61FF' }
            ])
          },
          barWidth: 15,
          borderRadius: [0, 4, 4, 0]
        }
      ]
    });
  }

  if (peakChartRef.value) {
    const peakChart = echarts.init(peakChartRef.value);
    const hours = (peakRes.hours || []).map(h => (h ? h.split(':')[0] + '时' : h));
    peakChart.setOption({
      backgroundColor: 'transparent',
      grid: { top: 20, right: 20, bottom: 30, left: 50 },
      xAxis: {
        type: 'category',
        data: hours,
        axisLine: { lineStyle: { color: '#30363D' } },
        axisLabel: { color: '#8B949E' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#30363D' } },
        axisLabel: { color: '#8B949E' },
        splitLine: { lineStyle: { color: '#21262D' } }
      },
      series: [
        {
          type: 'bar',
          data: peakRes.counts || [],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#FF9500' },
              { offset: 1, color: '#FF4757' }
            ])
          },
          barWidth: 15,
          borderRadius: [4, 4, 0, 0]
        }
      ]
    });
  }
};

const initLabAdminCharts = (statusRes = {}, trendRes = {}) => {
  if (labStatusChartRef.value) {
    const sc = echarts.init(labStatusChartRef.value);
    sc.setOption({
      backgroundColor: 'transparent',
      series: [
        {
          type: 'pie',
          radius: ['50%', '70%'],
          center: ['50%', '50%'],
          data: [
            { value: statusRes.idle || 0, name: '空闲', itemStyle: { color: '#00FF88' } },
            { value: statusRes.using || 0, name: '使用中', itemStyle: { color: '#00D4FF' } },
            { value: statusRes.maintaining || 0, name: '维修中', itemStyle: { color: '#FF9500' } },
            { value: statusRes.calibrating || 0, name: '校准中', itemStyle: { color: '#7B61FF' } }
          ],
          label: { color: '#8B949E' }
        }
      ]
    });
  }
  if (labTrendChartRef.value) {
    const tc = echarts.init(labTrendChartRef.value);
    tc.setOption({
      backgroundColor: 'transparent',
      grid: { top: 40, right: 20, bottom: 30, left: 50 },
      xAxis: {
        type: 'category',
        data: trendRes.dates || [],
        axisLine: { lineStyle: { color: '#30363D' } },
        axisLabel: { color: '#8B949E' }
      },
      yAxis: {
        type: 'value',
        axisLine: { lineStyle: { color: '#30363D' } },
        axisLabel: { color: '#8B949E' },
        splitLine: { lineStyle: { color: '#21262D' } }
      },
      series: [
        {
          data: trendRes.counts || [],
          type: 'line',
          smooth: true,
          lineStyle: { color: '#00D4FF', width: 3 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
              { offset: 1, color: 'rgba(0, 212, 255, 0)' }
            ])
          },
          itemStyle: { color: '#00D4FF' }
        }
      ]
    });
  }
};

const initTeacherChart = (trendRes = {}) => {
  if (!teacherTrendChartRef.value) return;
  const chart = echarts.init(teacherTrendChartRef.value);
  chart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 40, right: 20, bottom: 30, left: 50 },
    xAxis: {
      type: 'category',
      data: trendRes.dates || [],
      axisLine: { lineStyle: { color: '#30363D' } },
      axisLabel: { color: '#8B949E' }
    },
    yAxis: {
      type: 'value',
      axisLine: { lineStyle: { color: '#30363D' } },
      axisLabel: { color: '#8B949E' },
      splitLine: { lineStyle: { color: '#21262D' } }
    },
    series: [
      {
        data: trendRes.counts || [],
        type: 'line',
        smooth: true,
        lineStyle: { color: '#00D4FF', width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0)' }
          ])
        },
        itemStyle: { color: '#00D4FF' }
      }
    ]
  });
};

const initFaultChart = (statusRes = {}) => {
  if (!faultTypeChartRef.value) return;
  const chart = echarts.init(faultTypeChartRef.value);
  chart.setOption({
    backgroundColor: 'transparent',
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['50%', '50%'],
        data: [
          { value: statusRes.maintaining || 0, name: '维修中', itemStyle: { color: '#FF9500' } },
          { value: statusRes.idle || 0, name: '正常', itemStyle: { color: '#00FF88' } },
          { value: statusRes.using || 0, name: '使用中', itemStyle: { color: '#00D4FF' } }
        ],
        label: { color: '#8B949E' }
      }
    ]
  });
};

const getBookingStatusText = s => ['', '已通过', '已驳回', '已取消', '已完成'][s] || '待审核';
const getBookingStatusType = s => ['warning', 'success', 'danger', 'info', 'primary'][s] || 'info';

const resizeCharts = () => {
  echarts.getInstanceByDom(trendChartRef.value)?.resize();
  echarts.getInstanceByDom(statusChartRef.value)?.resize();
  echarts.getInstanceByDom(typeChartRef.value)?.resize();
  echarts.getInstanceByDom(peakChartRef.value)?.resize();
  echarts.getInstanceByDom(labStatusChartRef.value)?.resize();
  echarts.getInstanceByDom(labTrendChartRef.value)?.resize();
  echarts.getInstanceByDom(teacherTrendChartRef.value)?.resize();
  echarts.getInstanceByDom(faultTypeChartRef.value)?.resize();
};

onMounted(() => {
  if (showAdminDashboard.value) loadAdminDashboard();
  else if (isLabAdmin.value) loadLabAdminDashboard();
  else if (isTeacher.value) loadTeacherDashboard();
  else if (isStudent.value) loadStudentDashboard();
  else if (isMaintainer.value) loadMaintainerDashboard();
  window.addEventListener('resize', resizeCharts);
});
</script>

<style lang="scss" scoped>
.dashboard {
  width: 100%;
}
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}
.admin-stats-grid {
  grid-template-columns: repeat(4, 1fr);
}
.stat-card {
  background: #161b22;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #30363d;
  transition: all 0.3s;
  cursor: default;
  &:hover {
    border-color: #00d4ff;
    box-shadow: 0 0 20px rgba(0, 212, 255, 0.15);
  }
  &[style*="cursor: pointer"]:hover,
  &[title] {
    cursor: pointer;
    .stat-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}
.stat-content {
  flex: 1;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #e6edf3;
}
.stat-title {
  font-size: 14px;
  color: #8b949e;
  margin-top: 4px;
}
.stat-arrow {
  color: #8b949e;
  font-size: 16px;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.2s;
  flex-shrink: 0;
}

.quick-links {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.quick-link-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  color: #8b949e;
  font-size: 14px;
  transition: all 0.2s;
  &:hover {
    border-color: #00d4ff;
    color: #00d4ff;
    background: rgba(0, 212, 255, 0.05);
  }
}

.todo-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.todo-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.2s;
  &:hover {
    border-color: #00d4ff;
    box-shadow: 0 0 15px rgba(0, 212, 255, 0.1);
  }
}
.todo-info {
  flex: 1;
}
.todo-num {
  font-size: 24px;
  font-weight: 700;
  color: #e6edf3;
}
.todo-label {
  font-size: 13px;
  color: #8b949e;
  margin-top: 4px;
}

.charts-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}
.chart-card {
  background: #161b22;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363d;
  &.full {
    grid-column: 1 / -1;
  }
}
.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #e6edf3;
  margin-bottom: 16px;
}
.chart-container {
  height: 280px;
}

.notice-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 12px;
  padding: 20px;
  border-left: 4px solid #00d4ff;
}
.notice-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #e6edf3;
  font-weight: 600;
  margin-bottom: 12px;
  .el-icon {
    color: #00d4ff;
  }
}
.notice-content {
  p {
    margin: 0;
    color: #8b949e;
    font-size: 14px;
    line-height: 1.6;
    strong {
      color: #00d4ff;
    }
  }
}

.recent-bookings {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 12px;
  padding: 20px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #e6edf3;
  margin-bottom: 16px;
}

.custom-role-banner {
  margin-bottom: 20px;
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 12px;
  padding: 16px 20px;
  border-left: 4px solid #00d4ff;
  .sub {
    margin: 0;
    color: #8b949e;
    font-size: 14px;
    line-height: 1.6;
  }
}
</style>
