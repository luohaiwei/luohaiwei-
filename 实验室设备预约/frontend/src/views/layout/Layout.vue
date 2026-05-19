<template>
  <div class="layout-container">
    <!-- 移动端顶部导航栏 -->
    <header class="mobile-header">
      <div class="mobile-header-left">
        <el-icon :size="24" @click="toggleMobileSidebar" class="menu-toggle">
          <Fold v-if="!isMobileSidebarOpen" />
          <Expand v-else />
        </el-icon>
        <span class="mobile-logo-text">实验室预约</span>
      </div>
      <div class="mobile-header-right">
        <div class="notification-bell" @click="goToNotification">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" type="danger">
            <el-icon :size="20"><Bell /></el-icon>
          </el-badge>
        </div>
        <el-dropdown @command="handleCommand" trigger="click">
          <el-avatar :size="28" class="user-avatar">
            {{ userStore.userInfo.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 移动端遮罩层 -->
    <div v-if="isMobileSidebarOpen" class="mobile-overlay" @click="toggleMobileSidebar"></div>

    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': isCollapsed, 'mobile-sidebar': isMobileSidebarOpen }">
      <div class="logo">
        <el-icon :size="24"><Monitor /></el-icon>
        <span class="logo-text" v-show="!isCollapsed">实验室设备预约系统</span>
      </div>

      <!-- 桌面端折叠按钮 -->
      <div class="collapse-btn desktop-only" @click="toggleCollapse">
        <el-icon :size="18">
          <Fold v-if="!isCollapsed" />
          <Expand v-else />
        </el-icon>
      </div>

      <!-- ==================== 系统管理员菜单 ==================== -->
      <div v-if="isAdmin" class="menu-section">
        <div class="menu-section-title">系统管理</div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#161B22"
          text-color="#B1BAC4"
          active-text-color="#00D4FF"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>首页</span>
          </el-menu-item>
          <el-menu-item index="/user">
            <el-icon><User /></el-icon><span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/sys-role">
            <el-icon><Key /></el-icon><span>角色管理</span>
          </el-menu-item>
          <el-menu-item index="/sys-permission">
            <el-icon><Grid /></el-icon><span>权限分配</span>
          </el-menu-item>
          <el-menu-item index="/sys-config">
            <el-icon><Setting /></el-icon><span>系统全局配置</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">设备管理</div>
          <el-menu-item index="/device">
            <el-icon><Monitor /></el-icon><span>设备全局列表</span>
          </el-menu-item>
          <el-menu-item index="/category">
            <el-icon><Folder /></el-icon><span>设备分类管理</span>
          </el-menu-item>
          <el-menu-item index="/laboratory">
            <el-icon><OfficeBuilding /></el-icon><span>实验室信息管理</span>
          </el-menu-item>
          <el-menu-item index="/device-status-track">
            <el-icon><Monitor /></el-icon><span>设备状态跟踪</span>
          </el-menu-item>
          <el-menu-item index="/device-scrap">
            <el-icon><Delete /></el-icon><span>设备报废管理</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">预约管理</div>
          <el-menu-item index="/booking-global">
            <el-icon><Calendar /></el-icon><span>预约全局列表</span>
          </el-menu-item>
          <el-menu-item index="/audit">
            <el-icon><Checked /></el-icon><span>预约审核</span>
          </el-menu-item>
          <el-menu-item index="/booking-rule-global">
            <el-icon><Setting /></el-icon><span>预约全局规则</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">维护管理</div>
          <el-menu-item index="/repair">
            <el-icon><Tools /></el-icon><span>维修工单</span>
          </el-menu-item>
          <el-menu-item index="/repair-records">
            <el-icon><Document /></el-icon><span>维修记录</span>
          </el-menu-item>
          <el-menu-item index="/repair-global-monitor">
            <el-icon><Tools /></el-icon><span>维修工单全局监控</span>
          </el-menu-item>
          <el-menu-item index="/calibration">
            <el-icon><Setting /></el-icon><span>校准管理</span>
          </el-menu-item>
          <el-menu-item index="/health-record">
            <el-icon><Document /></el-icon><span>健康档案</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">日志审计</div>
          <el-menu-item index="/sys-log">
            <el-icon><Document /></el-icon><span>日志审计管理</span>
          </el-menu-item>
          <el-menu-item index="/ai-log-audit">
            <el-icon><ChatDotRound /></el-icon><span>AI日志审计</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">数据安全</div>
          <el-menu-item index="/backup-restore">
            <el-icon><FolderOpened /></el-icon><span>数据备份与恢复</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">数据分析</div>
          <el-menu-item index="/statistics">
            <el-icon><PieChart /></el-icon><span>数据分析报表</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">AI 助手</div>
          <el-menu-item index="/ai-assistant">
            <el-icon><ChatDotRound /></el-icon><span>AI 实验助手</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph">
            <el-icon><Connection /></el-icon><span>知识图谱</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-manage">
            <el-icon><Collection /></el-icon><span>知识库管理</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph-manage">
            <el-icon><Connection /></el-icon><span>图谱管理</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">个人中心</div>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- ==================== 实验室管理员菜单 ==================== -->
      <div v-else-if="isLabAdmin" class="menu-section">
        <div class="menu-section-title">日常管理</div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#161B22"
          text-color="#B1BAC4"
          active-text-color="#00D4FF"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>首页</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">设备管理</div>
          <el-menu-item index="/device-entry">
            <el-icon><Plus /></el-icon><span>设备入库管理</span>
          </el-menu-item>
          <el-menu-item index="/device">
            <el-icon><Monitor /></el-icon><span>设备列表</span>
          </el-menu-item>
          <el-menu-item index="/category">
            <el-icon><Folder /></el-icon><span>设备分类管理</span>
          </el-menu-item>
          <el-menu-item index="/device-status-track">
            <el-icon><Monitor /></el-icon><span>设备状态中心</span>
          </el-menu-item>
          <el-menu-item index="/device-scrap">
            <el-icon><Delete /></el-icon><span>设备报废管理</span>
          </el-menu-item>
          <el-menu-item index="/device-usage">
            <el-icon><Document /></el-icon><span>设备使用记录</span>
          </el-menu-item>
          <el-menu-item index="/laboratory">
            <el-icon><OfficeBuilding /></el-icon><span>实验室信息管理</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">预约管理</div>
          <el-menu-item index="/booking">
            <el-icon><Calendar /></el-icon><span>我的预约</span>
          </el-menu-item>
          <el-menu-item index="/audit">
            <el-icon><Checked /></el-icon><span>预约审核</span>
          </el-menu-item>
          <el-menu-item index="/booking-global">
            <el-icon><List /></el-icon><span>预约列表</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">维护管理</div>
          <el-menu-item index="/repair">
            <el-icon><Tools /></el-icon><span>维修工单</span>
          </el-menu-item>
          <el-menu-item index="/repair-records">
            <el-icon><Document /></el-icon><span>维修记录</span>
          </el-menu-item>
          <el-menu-item index="/calibration">
            <el-icon><Setting /></el-icon><span>校准管理</span>
          </el-menu-item>
          <el-menu-item index="/health-record">
            <el-icon><Document /></el-icon><span>健康档案</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">数据分析</div>
          <el-menu-item index="/statistics">
            <el-icon><PieChart /></el-icon><span>数据分析报表</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">AI 助手</div>
          <el-menu-item index="/ai-assistant">
            <el-icon><ChatDotRound /></el-icon><span>AI 实验助手</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph">
            <el-icon><Connection /></el-icon><span>知识图谱</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-manage">
            <el-icon><Collection /></el-icon><span>知识库管理</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph-manage">
            <el-icon><Connection /></el-icon><span>图谱管理</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">个人中心</div>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- ==================== 教师菜单 ==================== -->
      <div v-else-if="isTeacher" class="menu-section">
        <div class="menu-section-title">教学管理</div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#161B22"
          text-color="#B1BAC4"
          active-text-color="#00D4FF"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>首页</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">设备与预约</div>
          <el-menu-item index="/device">
            <el-icon><Monitor /></el-icon><span>可预约设备</span>
          </el-menu-item>
          <el-menu-item index="/booking">
            <el-icon><Calendar /></el-icon><span>我的预约</span>
          </el-menu-item>
          <el-menu-item index="/audit">
            <el-icon><Checked /></el-icon><span>学生预约审核</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">数据分析</div>
          <el-menu-item index="/statistics">
            <el-icon><PieChart /></el-icon><span>实验数据分析</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">AI 助手</div>
          <el-menu-item index="/ai-assistant">
            <el-icon><ChatDotRound /></el-icon><span>AI 实验助手</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph">
            <el-icon><Connection /></el-icon><span>知识图谱</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">个人中心</div>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- ==================== 自定义角色：按权限表动态菜单 ==================== -->
      <div v-else-if="isCustomRole" class="menu-section">
        <div class="menu-section-title">功能菜单</div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#161B22"
          text-color="#B1BAC4"
          active-text-color="#00D4FF"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>首页</span>
          </el-menu-item>
          <el-menu-item
            v-for="item in userStore.dynamicMenus"
            :key="item.route"
            :index="item.route"
          >
            <el-icon><component :is="menuIcon(item.icon)" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">个人中心</div>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- ==================== 学生菜单 ==================== -->
      <div v-else-if="isStudent" class="menu-section">
        <div class="menu-section-title">学习工具</div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#161B22"
          text-color="#B1BAC4"
          active-text-color="#00D4FF"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>首页</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">设备预约</div>
          <el-menu-item index="/device">
            <el-icon><Monitor /></el-icon><span>可预约设备</span>
          </el-menu-item>
          <el-menu-item index="/booking">
            <el-icon><Calendar /></el-icon><span>我的预约</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">AI 助手</div>
          <el-menu-item index="/ai-assistant">
            <el-icon><ChatDotRound /></el-icon><span>AI 实验助手</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph">
            <el-icon><Connection /></el-icon><span>知识图谱</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">个人中心</div>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </div>

      <!-- ==================== 设备维护人员菜单 ==================== -->
      <div v-else-if="isMaintainer" class="menu-section">
        <div class="menu-section-title">维护任务</div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          background-color="#161B22"
          text-color="#B1BAC4"
          active-text-color="#00D4FF"
          router
        >
          <el-menu-item index="/dashboard">
            <el-icon><DataAnalysis /></el-icon><span>首页</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">设备与预约</div>
          <el-menu-item index="/device">
            <el-icon><Monitor /></el-icon><span>可预约设备</span>
          </el-menu-item>
          <el-menu-item index="/booking">
            <el-icon><Calendar /></el-icon><span>我的预约</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">维护管理</div>
          <el-menu-item index="/repair">
            <el-icon><Tools /></el-icon><span>维修工单</span>
          </el-menu-item>
          <el-menu-item index="/repair-records">
            <el-icon><Document /></el-icon><span>维修记录</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">设备维护</div>
          <el-menu-item index="/device-status-track">
            <el-icon><Monitor /></el-icon><span>设备状态中心</span>
          </el-menu-item>
          <el-menu-item index="/calibration">
            <el-icon><Setting /></el-icon><span>校准管理</span>
          </el-menu-item>
          <el-menu-item index="/health-record">
            <el-icon><Document /></el-icon><span>健康档案</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">数据分析</div>
          <el-menu-item index="/statistics">
            <el-icon><PieChart /></el-icon><span>故障统计分析</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">AI 助手</div>
          <el-menu-item index="/ai-assistant">
            <el-icon><ChatDotRound /></el-icon><span>AI 实验助手</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph">
            <el-icon><Connection /></el-icon><span>知识图谱</span>
          </el-menu-item>

          <div class="menu-section-title" style="margin-top: 8px">个人中心</div>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon><span>个人中心</span>
          </el-menu-item>
        </el-menu>
      </div>
    </aside>

    <main class="main-content" :class="{ 'main-content-expanded': isCollapsed }">
      <!-- 桌面端头部 -->
      <header class="header desktop-only">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentMenu }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 通知铃铛 -->
          <div class="notification-bell" @click="goToNotification">
            <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" type="danger">
              <el-icon :size="20"><Bell /></el-icon>
            </el-badge>
          </div>

          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo.realName?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-text">
                <span class="user-name">{{
                  userStore.userInfo.realName || userStore.userInfo.username
                }}</span>
                <span class="user-role">{{ getRoleText(userStore.userInfo?.userType) }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, watch, ref, onMounted, onUnmounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '../../stores/user';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import {
  Connection,
  Monitor,
  DataAnalysis,
  Calendar,
  ChatDotRound,
  User,
  Folder,
  Tools,
  PieChart,
  Checked,
  Document,
  Setting,
  Key,
  Grid,
  Delete,
  FolderOpened,
  Plus,
  Collection,
  Bell,
  List,
  Timer,
  Fold,
  Expand
} from '@element-plus/icons-vue';
import { getUnreadCount } from '../../api/notification';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

// 响应式布局相关
const isCollapsed = ref(false);
const isMobileSidebarOpen = ref(false);
const isMobile = ref(false);

// 响应式检测
function checkMobile() {
  isMobile.value = window.innerWidth < 768;
  if (isMobile.value) {
    isCollapsed.value = true;
  }
}

// 切换侧边栏折叠状态（桌面端）
function toggleCollapse() {
  isCollapsed.value = !isCollapsed.value;
}

// 切换移动端侧边栏
function toggleMobileSidebar() {
  isMobileSidebarOpen.value = !isMobileSidebarOpen.value;
}

const userType = computed(() => userStore.userInfo?.userType || '');

const BUILTIN_USER_TYPES = ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER'];
const isCustomRole = computed(
  () => !!userType.value && !BUILTIN_USER_TYPES.includes(userType.value)
);

function menuIcon(name) {
  const n = name && String(name).trim();
  return (n && ElementPlusIconsVue[n]) || ElementPlusIconsVue['Menu'];
}

// 角色判断（自定义角色使用权限表动态侧栏 + 路由守卫按 allowedPaths）
const isAdmin = computed(() => userType.value === 'SYSTEM_ADMIN');
const isLabAdmin = computed(() => userType.value === 'LAB_ADMIN');
const isTeacher = computed(() => userType.value === 'TEACHER');
const isStudent = computed(() => userType.value === 'STUDENT');
const isMaintainer = computed(() => userType.value === 'MAINTAINER');

// 通知未读数
const unreadCount = ref(0);
let unreadTimer = null;
let unreadFailCount = 0;
let unreadSuspendUntil = 0;
const UNREAD_POLL_MS = 30000;
const UNREAD_MAX_FAIL = 3;
const UNREAD_COOLDOWN_MS = 180000;

async function loadUnreadCount() {
  if (!userStore.token) return;
  if (unreadSuspendUntil > Date.now()) return;
  try {
    const data = await getUnreadCount();
    const n =
      typeof data === 'number'
        ? data
        : data != null && data.unreadCount != null
          ? Number(data.unreadCount)
          : 0;
    unreadCount.value = Number.isFinite(n) ? n : 0;
    unreadFailCount = 0;
    unreadSuspendUntil = 0;
  } catch (e) {
    unreadFailCount += 1;
    if (unreadFailCount >= UNREAD_MAX_FAIL) {
      unreadSuspendUntil = Date.now() + UNREAD_COOLDOWN_MS;
      unreadFailCount = 0;
    }
  }
}

function goToNotification() {
  router.push('/notification');
}

function startUnreadPolling() {
  if (unreadTimer) clearInterval(unreadTimer);
  if (userStore.token) {
    loadUnreadCount();
    unreadTimer = setInterval(loadUnreadCount, UNREAD_POLL_MS);
  }
}

function stopUnreadPolling() {
  if (unreadTimer) {
    clearInterval(unreadTimer);
    unreadTimer = null;
  }
}

onMounted(() => {
  checkMobile();
  window.addEventListener('resize', checkMobile);
  startUnreadPolling();
});

// 监听登录状态变化，刷新通知数
watch(() => userStore.token, (newVal) => {
  if (newVal) {
    unreadSuspendUntil = 0;
    unreadFailCount = 0;
    startUnreadPolling();
  } else {
    unreadCount.value = 0;
    stopUnreadPolling();
  }
});

// 监听路由变化，关闭移动端侧边栏
watch(() => route.path, () => {
  if (isMobile.value && isMobileSidebarOpen.value) {
    isMobileSidebarOpen.value = false;
  }
});

onUnmounted(() => {
  stopUnreadPolling();
  window.removeEventListener('resize', checkMobile);
});

const activeMenu = computed(() => route.path);

const menuMap = {
  '/dashboard': '首页',
  '/user': '用户管理',
  '/sys-role': '角色管理',
  '/sys-permission': '权限分配',
  '/sys-config': '系统全局配置',
  '/device': '设备全局列表',
  '/device-entry': '设备入库管理',
  '/device-usage': '设备使用记录',
  '/laboratory': '实验室信息管理',
  '/device-status-track': '设备状态中心',
  '/device-scrap': '设备报废管理',
  '/category': '设备分类管理',
  '/booking': '我的预约',
  '/booking-global': '预约全局列表',
  '/booking-rule-global': '预约全局规则',
  '/audit': '预约审核',
  '/repair': '维修工单',
  '/repair-records': '维修记录',
  '/repair-global-monitor': '维修工单全局监控',
  '/calibration': '校准管理',
  '/health-record': '健康档案',
  '/statistics': '数据分析',
  '/ai-assistant': 'AI 实验助手',
  '/knowledge-graph': '知识图谱',
  '/knowledge-manage': '知识库管理',
  '/ai-log-audit': 'AI日志审计',
  '/sys-log': '日志审计管理',
  '/backup-restore': '数据备份与恢复',
  '/profile': '个人中心'
};
const currentMenu = computed(() => {
  if (route.path.startsWith('/device/') && route.path !== '/device') return '设备详情';
  if (route.path === '/booking-global' && userType.value === 'LAB_ADMIN') return '预约列表';
  const dyn = userStore.dynamicMenus?.find(m => m.route === route.path);
  if (dyn) return dyn.title;
  return menuMap[route.path] || '首页';
});

const roleMap = {
  SYSTEM_ADMIN: '系统管理员',
  LAB_ADMIN: '实验室管理员',
  TEACHER: '教师',
  STUDENT: '学生',
  MAINTAINER: '设备维护人员'
};
const getRoleText = t => roleMap[t] || t || '用户';

const handleCommand = command => {
  if (command === 'logout') {
    userStore.logout();
    router.push('/login');
  } else if (command === 'profile') {
    router.push('/profile');
  }
};
</script>

<style lang="scss" scoped>
// 响应式布局断点
$mobile-breakpoint: 768px;
$tablet-breakpoint: 1024px;

.layout-container {
  display: flex;
  height: 100vh;
  background: #0d1117;
}

.sidebar {
  width: 200px;
  background: #161b22;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #30363d;
  overflow-y: auto;
  transition: width 0.3s ease, transform 0.3s ease;
  flex-shrink: 0;

  // 折叠状态
  &.sidebar-collapsed {
    width: 60px;

    .logo-text,
    .menu-section-title,
    .el-menu-item span {
      display: none;
    }

    .el-menu-item {
      padding-left: 20px !important;
      justify-content: center;
    }
  }

  // 移动端侧边栏
  @media (max-width: #{$mobile-breakpoint - 1}) {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1001;
    transform: translateX(-100%);

    &.mobile-sidebar {
      transform: translateX(0);
    }
  }
}

// 移动端遮罩层
.mobile-overlay {
  display: none;

  @media (max-width: #{$mobile-breakpoint - 1}) {
    display: block;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 1000;
  }
}

// 移动端顶部导航栏
.mobile-header {
  display: none;

  @media (max-width: #{$mobile-breakpoint - 1}) {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 50px;
    background: #161b22;
    padding: 0 16px;
    border-bottom: 1px solid #30363d;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    z-index: 999;

    .mobile-header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .menu-toggle {
        cursor: pointer;
        color: #B1BAC4;
        padding: 4px;
        &:hover {
          color: #00D4FF;
        }
      }

      .mobile-logo-text {
        color: #00D4FF;
        font-size: 14px;
        font-weight: 600;
      }
    }

    .mobile-header-right {
      display: flex;
      align-items: center;
      gap: 12px;

      .user-avatar {
        background: linear-gradient(135deg, #00d4ff, #7b61ff);
        color: #fff;
        font-weight: 600;
        cursor: pointer;
      }

      .notification-bell {
        display: flex;
        align-items: center;
        cursor: pointer;
        padding: 4px;
        color: #8b949e;
      }
    }
  }
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #00d4ff;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid #30363d;
  padding: 0 12px;
  flex-shrink: 0;
}

.logo-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 140px;
  text-shadow: 0 0 10px rgba(0, 212, 255, 0.5);
}

// 折叠按钮
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  cursor: pointer;
  color: #8b949e;
  transition: all 0.2s;

  &:hover {
    background: rgba(0, 212, 255, 0.1);
    color: #00D4FF;
  }
}

// 桌面端隐藏
.desktop-only {
  @media (max-width: #{$mobile-breakpoint - 1}) {
    display: none !important;
  }
}

.menu-section {
  flex: 1;
  overflow-y: auto;
}

.menu-section-title {
  padding: 8px 16px 4px;
  font-size: 11px;
  font-weight: 700;
  color: #484f58;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.sidebar-menu {
  border-right: none;
  background: transparent !important;
}

.el-menu-item {
  height: 40px;
  line-height: 40px;
  padding-left: 16px !important;
  font-size: 13px;
  &:hover {
    background: #1f2937 !important;
  }
  &.is-active {
    background: rgba(0, 212, 255, 0.1) !important;
    color: #00d4ff !important;
  }
  .el-icon {
    margin-right: 6px;
    font-size: 14px;
  }
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left 0.3s ease;

  &.main-content-expanded {
    margin-left: 0;
  }

  @media (max-width: #{$mobile-breakpoint - 1}) {
    padding-top: 50px;
  }
}

.header {
  height: 60px;
  background: #161b22;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-bottom: 1px solid #30363d;
  flex-shrink: 0;
}

.header-left {
  :deep(.el-breadcrumb__item) {
    .el-breadcrumb__inner {
      color: #8b949e;
      &.is-link:hover {
        color: #00d4ff;
      }
    }
    &:last-child .el-breadcrumb__inner {
      color: #e6edf3;
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;

  .notification-bell {
    display: flex;
    align-items: center;
    cursor: pointer;
    padding: 8px;
    border-radius: 8px;
    color: #8b949e;
    transition: all 0.2s ease;
    &:hover {
      background: rgba(0, 212, 255, 0.08);
      color: #00d4ff;
    }
    :deep(.el-badge__content) {
      top: 4px;
      right: 4px;
    }
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    padding: 8px 14px;
    border-radius: 8px;
    border: 1px solid #30363d;
    transition: all 0.2s ease;
    &:hover {
      background: rgba(0, 212, 255, 0.08);
      border-color: rgba(0, 212, 255, 0.4);
      box-shadow: 0 0 12px rgba(0, 212, 255, 0.15);
    }
  }

  .user-avatar {
    background: linear-gradient(135deg, #00d4ff, #7b61ff);
    color: #fff;
    font-weight: 600;
  }

  .user-text {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 2px;
  }

  .user-name {
    color: #e6edf3;
    font-size: 14px;
    font-weight: 500;
  }

  .user-role {
    color: #00d4ff;
    font-size: 12px;
    opacity: 0.9;
  }
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: #0d1117;

  // 移动端适配
  @media (max-width: #{$mobile-breakpoint - 1}) {
    padding: 16px;
  }
}
</style>
