import { createRouter, createWebHistory } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ElMessage } from 'element-plus';
import { BUILTIN_USER_TYPES } from '../constants/userRoles';

// 静态路由
const staticRoutes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录' }
  }
];

// 动态路由 - 严格按论文角色权限分配
// SYSTEM_ADMIN=系统管理员  LAB_ADMIN=实验室管理员  TEACHER=教师  STUDENT=学生  MAINTAINER=设备维护人员
const dynamicRoutes = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('../views/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      // ==================== 首页（所有角色） ====================
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/home/Dashboard.vue'),
        meta: {
          title: '首页',
          icon: 'DataAnalysis',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },

      // ==================== 系统管理员专属路由 ====================
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('../views/system/UserManage.vue'),
        meta: { title: '用户管理', icon: 'User', roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'sys-role',
        name: 'SysRole',
        component: () => import('../views/system/SysRole.vue'),
        meta: { title: '角色管理', icon: 'Key', roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'sys-permission',
        name: 'SysPermission',
        component: () => import('../views/system/SysPermission.vue'),
        meta: { title: '权限分配', icon: 'Grid', roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'sys-config',
        name: 'SysConfig',
        component: () => import('../views/system/SysConfig.vue'),
        meta: { title: '系统全局配置', icon: 'Setting', roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'sys-log',
        name: 'SysLog',
        component: () => import('../views/system/SysLog.vue'),
        meta: { title: '日志审计管理', icon: 'Document', roles: ['SYSTEM_ADMIN'] }
      },
      // 与后端 /global-list 一致：系统管理员全量；实验室管理员按数据权限（通常为本实验室）
      {
        path: 'booking-global',
        name: 'BookingGlobal',
        component: () => import('../views/booking/BookingGlobalList.vue'),
        meta: { title: '预约全局列表', icon: 'Calendar', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN'] }
      },
      {
        path: 'booking-rule-global',
        name: 'BookingRuleGlobal',
        component: () => import('../views/booking/BookingRuleGlobal.vue'),
        meta: { title: '预约全局规则', icon: 'Setting', roles: ['SYSTEM_ADMIN'] }
      },
      // 原「预约规则配置」页面已合并至预约全局规则；旧链接与书签仍指向 /booking-rule，统一重定向
      {
        path: 'booking-rule',
        redirect: '/booking-rule-global'
      },
      {
        path: 'device-scrap',
        name: 'DeviceScrap',
        component: () => import('../views/device/DeviceScrap.vue'),
        meta: { title: '设备报废管理', icon: 'Delete', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN'] }
      },
      {
        path: 'repair-global-monitor',
        name: 'RepairGlobalMonitor',
        component: () => import('../views/repair/RepairGlobalMonitor.vue'),
        meta: { title: '维修工单全局监控', icon: 'Tools', roles: ['SYSTEM_ADMIN'] }
      },
      {
        path: 'backup-restore',
        name: 'BackupRestore',
        component: () => import('../views/system/BackupRestore.vue'),
        meta: { title: '数据备份与恢复', icon: 'FolderOpened', roles: ['SYSTEM_ADMIN'] }
      },

      // ==================== 实验室管理员专属路由 ====================
      {
        path: 'device-entry',
        name: 'DeviceEntry',
        component: () => import('../views/device/DeviceEntry.vue'),
        meta: { title: '设备入库管理', icon: 'Plus', roles: ['LAB_ADMIN'] }
      },
      {
        path: 'laboratory',
        name: 'Laboratory',
        component: () => import('../views/lab/LaboratoryList.vue'),
        meta: { title: '实验室信息管理', icon: 'OfficeBuilding', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN'] }
      },
      {
        path: 'device-usage',
        name: 'DeviceUsage',
        component: () => import('../views/device/DeviceUsage.vue'),
        meta: { title: '设备使用记录', icon: 'Document', roles: ['LAB_ADMIN'] }
      },

      // ==================== 设备管理员（系统管理员+实验室管理员）路由 ====================
      {
        path: 'category',
        name: 'Category',
        component: () => import('../views/category/CategoryList.vue'),
        meta: { title: '设备分类管理', icon: 'Folder', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN'] }
      },

      // ==================== 预约相关路由（按论文严格分配） ====================
      // 我的预约：学生/教师/实验室管理员/维护人员（与后端 createBooking、/my 的 MAINTAINER 一致；系统管理员无预约业务）
      {
        path: 'booking',
        name: 'Booking',
        component: () => import('../views/booking/BookingList.vue'),
        meta: { title: '我的预约', icon: 'Calendar', roles: ['LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER'] }
      },
      // 预约审核：与后端 BookingController 审核接口一致（SYSTEM_ADMIN / 实验室管理员 / 教师 / 含 booking-audit 的自定义角色）
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('../views/booking/AuditList.vue'),
        meta: { title: '预约审核', icon: 'Checked', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER'] }
      },

      // ==================== 设备路由（全部角色可浏览，编辑仅管理员） ====================
      {
        path: 'device',
        name: 'Device',
        component: () => import('../views/device/DeviceList.vue'),
        meta: {
          title: '设备列表',
          icon: 'Monitor',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },
      {
        path: 'device/:id',
        name: 'DeviceDetail',
        component: () => import('../views/device/DeviceDetail.vue'),
        meta: {
          title: '设备详情',
          icon: 'Monitor',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },
      {
        path: 'device-status-track',
        name: 'DeviceStatusTrack',
        component: () => import('../views/device/DeviceStatusCenter.vue'),
        meta: {
          title: '设备状态中心',
          icon: 'Monitor',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'MAINTAINER']
        }
      },

      // ==================== 维护相关路由（实验室管理员+维护人员） ====================
      {
        path: 'repair',
        name: 'Repair',
        component: () => import('../views/repair/RepairList.vue'),
        meta: {
          title: '维修工单',
          icon: 'Tools',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'MAINTAINER']
        }
      },
      {
        path: 'repair-records',
        name: 'RepairRecords',
        component: () => import('../views/repair/RepairRecordQuery.vue'),
        meta: {
          title: '维修记录',
          icon: 'Document',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'MAINTAINER']
        }
      },
      {
        path: 'calibration',
        name: 'Calibration',
        component: () => import('../views/maintenance/Calibration.vue'),
        meta: {
          title: '校准管理',
          icon: 'Setting',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'MAINTAINER']
        }
      },
      {
        path: 'health-record',
        name: 'HealthRecord',
        component: () => import('../views/maintenance/HealthRecord.vue'),
        meta: {
          title: '健康档案',
          icon: 'Document',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'MAINTAINER']
        }
      },

      // ==================== 统计分析路由 ====================
      // 系统管理员：全局统计  实验室管理员：所属实验室统计  教师：教学统计  维护人员：故障统计
      {
        path: 'statistics',
        name: 'Statistics',
        component: () => import('../views/statistics/Statistics.vue'),
        meta: {
          title: '数据分析',
          icon: 'PieChart',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },

      // ==================== AI 助手（所有角色） ====================
      {
        path: 'ai-assistant',
        name: 'AIAssistant',
        component: () => import('../views/ai/AIAssistant.vue'),
        meta: {
          title: 'AI实验助手',
          icon: 'ChatDotRound',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },
      {
        path: 'knowledge-graph',
        name: 'KnowledgeGraph',
        component: () => import('../views/ai/KnowledgeGraph.vue'),
        meta: {
          title: '知识图谱',
          icon: 'Connection',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },
      // 知识库管理（系统管理员+实验室管理员）
      {
        path: 'knowledge-manage',
        name: 'KnowledgeManage',
        component: () => import('../views/ai/KnowledgeManage.vue'),
        meta: { title: '知识库管理', icon: 'Collection', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN'] }
      },
      {
        path: 'knowledge-graph-manage',
        name: 'KnowledgeGraphManage',
        component: () => import('../views/ai/KnowledgeGraphManage.vue'),
        meta: { title: '图谱管理', icon: 'Connection', roles: ['SYSTEM_ADMIN', 'LAB_ADMIN'] }
      },
      // AI日志审计（系统管理员）
      {
        path: 'ai-log-audit',
        name: 'AiLogAudit',
        component: () => import('../views/ai/AiLogAudit.vue'),
        meta: { title: 'AI日志审计', icon: 'DataAnalysis', roles: ['SYSTEM_ADMIN'] }
      },

      // ==================== 通知中心（所有登录角色） ====================
      {
        path: 'notification',
        name: 'Notification',
        component: () => import('../views/notification/NotificationList.vue'),
        meta: {
          title: '通知中心',
          icon: 'Bell',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      },

      // ==================== 个人中心（所有角色） ====================
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('../views/profile/Profile.vue'),
        meta: {
          title: '个人中心',
          icon: 'User',
          roles: ['SYSTEM_ADMIN', 'LAB_ADMIN', 'TEACHER', 'STUDENT', 'MAINTAINER']
        }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes: [...staticRoutes, ...dynamicRoutes]
});

// 路由守卫（含前端角色守卫，防止 URL 绕过）
router.beforeEach(async (to, from, next) => {
  const store = useUserStore();
  if (to.path === '/login') {
    next();
    return;
  }
  if (!store.token) {
    ElMessage.warning('请先登录');
    next('/login');
    return;
  }
  const rawType = store.userInfo?.userType;
  if (!rawType || BUILTIN_USER_TYPES.includes(rawType)) {
    const allowedRoles = to.meta?.roles;
    if (allowedRoles && allowedRoles.length > 0 && !allowedRoles.includes(rawType)) {
      ElMessage.warning('您没有权限访问该页面');
      next('/dashboard');
      return;
    }
    next();
    return;
  }
  if (!store.dynamicMenusLoaded) {
    await store.loadMenus();
  }
  const path = to.path.replace(/\/$/, '') || '/';
  if (store.canCustomAccessPath(path)) {
    next();
    return;
  }
  // 兜底：通知中心（/notification）允许所有已登录用户访问
  if (path === '/notification') {
    next();
    return;
  }
  ElMessage.warning('您没有权限访问该页面');
  next('/dashboard');
});

export default router;
