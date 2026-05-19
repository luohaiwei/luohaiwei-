<template>
  <div class="user-manage">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>用户管理</h2>
      </div>
      <div class="header-actions">
        <el-button v-if="canEditUser" type="primary" class="add-btn" @click="openAdd">
          <el-icon><Plus /></el-icon> 新增用户
        </el-button>
        <el-button v-if="canEditUser" @click="handleExport" :loading="exportLoading">
          <el-icon><Download /></el-icon> 导出
        </el-button>
        <el-button v-if="canDeleteUser" type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
          <el-icon><Delete /></el-icon> 批量删除
          <span v-if="selectedRows.length > 0">({{ selectedRows.length }})</span>
        </el-button>
      </div>
    </div>

    <div class="page-card search-card">
      <div class="search-row">
        <div class="search-field">
          <label>关键词</label>
          <el-input
            v-model="searchForm.username"
            placeholder="用户名 / 真实姓名"
            clearable
            @keyup.enter="loadData"
          />
        </div>
        <div class="search-field">
          <label>角色</label>
          <el-select v-model="searchForm.userType" placeholder="全部" clearable>
            <el-option label="系统管理员" value="SYSTEM_ADMIN" />
            <el-option label="实验室管理员" value="LAB_ADMIN" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="学生" value="STUDENT" />
            <el-option label="设备维护人员" value="MAINTAINER" />
          </el-select>
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
      <div class="search-summary">
        共 <span class="highlight">{{ total }}</span> 位用户
      </div>
    </div>

    <div class="page-card table-card">
      <el-table
        ref="tableRef"
        :data="tableData"
        border
        stripe
        row-key="id"
        highlight-current-row
        @selection-change="onSelectionChange"
      >
        <el-table-column
          type="selection"
          width="50"
          :selectable="rowSelectable"
          :reserve-selection="true"
        />
        <el-table-column
          type="index"
          label="序号"
          width="65"
          align="center"
          :index="tableRowIndex"
        />
        <el-table-column prop="username" label="用户名" min-width="130">
          <template #default="{ row }">
            <div class="cell-username">
              <span class="uname">{{ row.username }}</span>
              <el-tag v-if="row.username === 'admin'" size="small" type="danger">内置</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="studentStaffNo" label="学号/工号" min-width="130" />
        <el-table-column prop="gender" label="性别" width="70" align="center">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="userType" label="角色" min-width="130">
          <template #default="{ row }">
            <el-tag size="small" :type="getRoleTagType(row.userType)">{{
              getRoleText(row.userType)
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机" min-width="130" />
        <el-table-column
          prop="department"
          label="部门/班级"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column
          prop="laboratory"
          label="所属实验室"
          min-width="140"
          show-overflow-tooltip
        />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.status === 1 ? 'status-on' : 'status-off'">
              <span class="status-dot"></span>
              {{ row.status === 1 ? '正常' : '禁用' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="missedCount" label="爽约次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.missedCount > 0" type="danger" size="small" effect="plain">
              {{ row.missedCount }}次
            </el-tag>
            <span v-else style="color: #8b949e; font-size: 12px;">0次</span>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" min-width="168" show-overflow-tooltip>
          <template #default="{ row }">{{ formatLastLogin(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="168" show-overflow-tooltip>
          <template #default="{ row }">{{ formatCreateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="430" fixed="right" align="center">
          <template #default="{ row }">
            <div v-if="isBuiltInAdmin(row)" class="builtin-ops-hint">内置账号，不可编辑或变更角色</div>
            <div v-else class="action-btns">
              <div class="action-line">
                <el-button v-if="canEditUser" type="primary" plain size="small" @click="openEdit(row)">
                  <el-icon class="btn-ico"><Edit /></el-icon>编辑
                </el-button>
                <el-button v-if="canAssignRole" type="info" plain size="small" @click="openRole(row)">
                  <el-icon class="btn-ico"><User /></el-icon>角色
                </el-button>
                <el-button v-if="canEditUser" type="warning" plain size="small" @click="handleResetPassword(row)">
                  <el-icon class="btn-ico"><Key /></el-icon>重置密码
                </el-button>
              </div>
              <div class="action-line">
                <el-button
                  v-if="canEditUser"
                  :type="row.status === 1 ? 'danger' : 'success'"
                  plain
                  size="small"
                  @click="toggleStatus(row)"
                >
                  <el-icon class="btn-ico">
                    <component :is="row.status === 1 ? 'Close' : 'Check'" />
                  </el-icon>
                  {{ row.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button
                  v-if="canEditUser && row.missedCount > 0"
                  type="warning"
                  plain
                  size="small"
                  @click="handleResetMissedCount(row)"
                >
                  清零爽约
                </el-button>
                <el-button v-if="canDeleteUser" type="danger" plain size="small" @click="handleDelete(row)">
                  <el-icon class="btn-ico"><Delete /></el-icon>删除
                </el-button>
              </div>
            </div>
          </template>
        </el-table-column>
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

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="580px"
      @close="resetForm"
      @opened="onUserDialogOpened"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="user-form-dialog"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            :disabled="isEdit"
            placeholder="登录用户名"
            autocomplete="username"
          />
          <p v-if="isEdit" class="field-tip">
            用户名是登录账号，创建后不可修改（与日志、权限关联，避免账号混乱）。
          </p>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="初始密码（至少 6 位）"
            show-password
            autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="学号/工号" prop="studentStaffNo">
          <el-input v-model="form.studentStaffNo" placeholder="学生填学号，教师/其他填工号" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="角色" prop="userType">
          <el-select v-model="form.userType" placeholder="请选择角色" style="width: 100%" filterable>
            <el-option
              v-for="r in assignableRoleOptions"
              :key="r.id"
              :label="`${r.roleName} (${r.roleCode})`"
              :value="r.roleCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" autocomplete="tel" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" type="email" autocomplete="email" />
        </el-form-item>
        <el-form-item label="部门/班级">
          <el-input v-model="form.department" autocomplete="organization" />
        </el-form-item>
        <el-form-item label="所属实验室">
          <el-select
            v-model="form.laboratory"
            placeholder="请选择所属实验室（用于数据权限）"
            style="width: 100%"
            clearable
            filterable
          >
            <el-option
              v-for="lab in laboratoryOptions"
              :key="lab"
              :label="lab"
              :value="lab"
            />
          </el-select>
          <p class="field-tip">设置后，配合"本实验室数据"权限可实现实验室级数据隔离</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <el-input :model-value="currentRow?.realName" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="roleForm.userType" placeholder="请选择角色" style="width: 100%" filterable>
            <el-option
              v-for="r in assignableRoleOptions"
              :key="r.id"
              :label="`${r.roleName} (${r.roleCode})`"
              :value="r.roleCode"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssignRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import {
  getUserList,
  addUser,
  updateUser,
  deleteUser,
  assignRole,
  updateUserStatus,
  resetUserPassword,
  resetMissedCount,
  exportUserList,
  batchDeleteUsers,
  getRoleList,
  getMyDataScope
} from '@/api/user';
import { getAllLaboratories } from '@/api/laboratory';
import { useUserStore } from '@/stores/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Edit, User, Delete, Check, Close, Key, Plus, Download } from '@element-plus/icons-vue';
import dayjs from 'dayjs';

const userStore = useUserStore();

const searchForm = reactive({ username: '', userType: '' });
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);
const roleDialogVisible = ref(false);
const isEdit = ref(false);
const submitLoading = ref(false);
const formRef = ref();
const currentRow = ref(null);
const form = reactive({
  id: undefined,
  username: '',
  password: '',
  realName: '',
  userType: 'STUDENT',
  studentStaffNo: '',
  gender: undefined,
  phone: '',
  email: '',
  department: '',
  laboratory: ''
});
const roleForm = reactive({ userType: '' });
const selectedRows = ref([]);
const roleOptions = ref([]);
/** 实验室选项列表（从设备表获取） */
const laboratoryOptions = ref([]);
/** 分配/新增/编辑用户时不可选系统管理员（内置 admin 不在此维护） */
const assignableRoleOptions = computed(() =>
  (roleOptions.value || []).filter(r => r && r.roleCode !== 'SYSTEM_ADMIN')
);
const exportLoading = ref(false);
const tableRef = ref(null);

/** 数据权限「用户数据可见性」：控制按钮显示 */
const userScope = ref([]);
const scopeLoaded = ref(false);
const canEditUser = computed(() =>
  String(userStore.userInfo?.userType) === 'SYSTEM_ADMIN' ||
  userScope.value.some(s => String(s).trim().toUpperCase() === 'EDIT')
);
const canDeleteUser = computed(() =>
  String(userStore.userInfo?.userType) === 'SYSTEM_ADMIN' ||
  userScope.value.some(s => String(s).trim().toUpperCase() === 'DELETE')
);
const canAssignRole = computed(() =>
  String(userStore.userInfo?.userType) === 'SYSTEM_ADMIN' ||
  userScope.value.some(s => String(s).trim().toUpperCase() === 'ROLE')
);

const loadUserDataScope = async () => {
  try {
    const res = await getMyDataScope();
    userScope.value = res?.userScope || [];
  } catch (e) {
    console.error(e);
    userScope.value = [];
  } finally {
    scopeLoaded.value = true;
  }
};

const rowSelectable = row =>
  row && String(row.username || '').toLowerCase() !== 'admin';

const rules = computed(() => {
  const r = {
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
    userType: [{ required: true, message: '请选择角色', trigger: 'change' }]
  };
  if (!isEdit.value) {
    r.password = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' }
    ];
  }
  return r;
});

const getRoleText = t => {
  const m = {
    SYSTEM_ADMIN: '系统管理员',
    LAB_ADMIN: '实验室管理员',
    TEACHER: '教师',
    STUDENT: '学生',
    MAINTAINER: '设备维护人员'
  };
  return m[t] || t;
};
const getRoleTagType = t => {
  const m = {
    SYSTEM_ADMIN: 'danger',
    LAB_ADMIN: 'warning',
    TEACHER: 'primary',
    STUDENT: 'success',
    MAINTAINER: 'info'
  };
  return m[t] || 'info';
};

/** 内置超级管理员：禁止编辑资料与变更角色（与后端约束一致） */
const isBuiltInAdmin = row => row && String(row.username || '').toLowerCase() === 'admin';

const formatCreateTime = v => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-');
const formatLastLogin = v => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-');
const genderText = g => ({ 1: '男', 2: '女' }[g] || '-');

/** 分页全局序号：第 2 页每页 10 条时应显示 11～20，而非再次 1～10 */
const tableRowIndex = index => (pageNum.value - 1) * pageSize.value + index + 1;

const loadData = async () => {
  try {
    const res = await getUserList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      ...searchForm
    });
    tableData.value = res.list || [];
    total.value = res.total || 0;
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败');
  }
};

const resetSearch = () => {
  searchForm.username = '';
  searchForm.userType = '';
  pageNum.value = 1;
  selectedRows.value = [];
  tableRef.value?.clearSelection();
  loadData();
};

const onUserDialogOpened = async () => {
  formRef.value?.clearValidate();
  await Promise.all([loadRoles(), loadLaboratoryOptions()]);
};

const openAdd = async () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
  await loadRoles();
};

const openEdit = async row => {
  isEdit.value = true;
  Object.assign(form, row);
  form.password = '';
  dialogVisible.value = true;
  await loadRoles();
};

const resetForm = () => {
  form.id = undefined;
  form.username = '';
  form.password = '';
  form.realName = '';
  form.userType = 'STUDENT';
  form.studentStaffNo = '';
  form.gender = undefined;
  form.phone = '';
  form.email = '';
  form.department = '';
  form.laboratory = '';
};

/** 加载实验室选项列表 */
const loadLaboratoryOptions = async () => {
  try {
    const res = await getAllLaboratories();
    if (Array.isArray(res)) {
      laboratoryOptions.value = res.map(l => l.labName || l).filter(Boolean);
    } else if (res && res.list) {
      laboratoryOptions.value = res.list.map(l => l.labName || l).filter(Boolean);
    } else {
      laboratoryOptions.value = [];
    }
  } catch (e) {
    console.error('加载实验室列表失败', e);
    laboratoryOptions.value = [];
  }
};

/** 只提交接口所需字段，避免把表格行里的 createTime/lastLoginTime 等原样回传导致后端 JSON 反序列化失败（400 请求数据格式错误） */
const buildUserSubmitPayload = () => {
  const lab = form.laboratory;
  const laboratoryVal =
    lab != null && String(lab).trim() !== '' ? String(lab).trim() : null;
  const base = {
    username: form.username,
    realName: form.realName,
    userType: form.userType,
    studentStaffNo: form.studentStaffNo || '',
    gender: form.gender,
    phone: form.phone || '',
    email: form.email || '',
    department: form.department || '',
    laboratory: laboratoryVal
  };
  if (isEdit.value) {
    return { id: form.id, ...base };
  }
  return { ...base, password: form.password };
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async valid => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      const payload = buildUserSubmitPayload();
      if (isEdit.value) {
        await updateUser(payload);
      } else {
        await addUser(payload);
      }
      ElMessage.success('操作成功');
      dialogVisible.value = false;
      loadData();
    } catch (e) {
      ElMessage.error(e.message || '操作失败');
    } finally {
      submitLoading.value = false;
    }
  });
};

const openRole = async row => {
  currentRow.value = row;
  roleForm.userType = row.userType || '';
  roleDialogVisible.value = true;
  await loadRoles();
};

const loadRoles = async () => {
  try {
    const res = await getRoleList({ pageNum: 1, pageSize: 200 });
    // 直接过滤：系统管理员不可通过界面分配给用户
    roleOptions.value = (res.list || []).filter(r => r && r.roleCode !== 'SYSTEM_ADMIN');
  } catch (e) {
    console.error('加载角色列表失败', e);
  }
};

const handleAssignRole = async () => {
  try {
    const res = await assignRole(currentRow.value.id, roleForm.userType);
    ElMessage.success({
      message: '角色分配成功！用户需重新登录后新角色菜单才会生效。',
      duration: 5000
    });
    roleDialogVisible.value = false;
    loadData();
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  }
};

const toggleStatus = async row => {
  try {
    await updateUserStatus(row.id, row.status === 1 ? 0 : 1);
    ElMessage.success('操作成功');
    loadData();
  } catch (e) {
    ElMessage.error(e.message || '操作失败');
  }
};

const handleResetPassword = async row => {
  try {
    await ElMessageBox.confirm(
      '确定将用户"' +
        (row.realName || row.username) +
        '"的登录密码重置为系统默认初始密码（123456）吗？',
      '重置密码',
      { type: 'warning', confirmButtonText: '确定重置', cancelButtonText: '取消' }
    );
    const res = await resetUserPassword(row.id);
    if (res.defaultPasswordPlain) {
      await ElMessageBox.alert(
        '当前默认密码为：' +
          res.defaultPasswordPlain +
          '\n请告知用户尽快登录，并在个人中心修改为自有密码。',
        '重置成功',
        { type: 'success', confirmButtonText: '知道了' }
      );
      loadData();
    } else {
      ElMessage.error(res.message || '重置失败');
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
};

const handleResetMissedCount = async row => {
  try {
    await ElMessageBox.confirm(
      '确定将用户「' +
        (row.realName || row.username) +
        '」的爽约次数清零吗？清零后该用户将解除预约限制。',
      '清零爽约次数',
      { type: 'warning', confirmButtonText: '确定清零', cancelButtonText: '取消' }
    );
    await resetMissedCount(row.id);
    ElMessage.success('爽约次数已清零');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败');
  }
};

const handleDelete = async row => {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？', '提示', { type: 'warning' });
    await deleteUser(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败');
  }
};

const onSelectionChange = rows => {
  selectedRows.value = rows.filter(
    r => r && String(r.username || '').toLowerCase() !== 'admin'
  );
};

const handleExport = async () => {
  exportLoading.value = true;
  try {
    const res = await exportUserList(searchForm);
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/octet-stream' });
    if (blob.type && blob.type.includes('application/json')) {
      const text = await blob.text();
      const j = JSON.parse(text);
      throw new Error(j.message || '导出失败');
    }
    const url = URL.createObjectURL(
      new Blob([blob], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      })
    );
    const a = document.createElement('a');
    a.href = url;
    a.download = `用户列表_${new Date().toISOString().slice(0, 10)}.xlsx`;
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (e) {
    let msg = e.message || '导出失败';
    const data = e.response?.data;
    if (data instanceof Blob) {
      try {
        const text = await data.text();
        const j = JSON.parse(text);
        if (j.message) msg = j.message;
      } catch (_) {
        /* ignore */
      }
    }
    ElMessage.error(msg);
  } finally {
    exportLoading.value = false;
  }
};

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) return;
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 位用户吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
    );
    const ids = selectedRows.value.map(r => r.id);
    await batchDeleteUsers(ids);
    ElMessage.success('批量删除成功');
    selectedRows.value = [];
    tableRef.value?.clearSelection();
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '批量删除失败');
  }
};

onMounted(async () => {
  await loadUserDataScope();
  loadData();
});
</script>

<style scoped lang="scss">
.user-manage {
  padding: 0;
  color: #e6edf3;
  min-height: 100%;
}

.page-card {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
  transition: border-color 0.2s;
}

.page-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #161b22 0%, #1a1f35 100%);
  border-left: 4px solid #00d4ff;
  padding: 18px 24px;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
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
  letter-spacing: 1px;
}

.header-sub {
  font-size: 12px;
  color: #8b949e;
}

.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  padding: 10px 20px;
  font-size: 14px;
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
  max-width: 260px;

  label {
    font-size: 12px;
    font-weight: 600;
    color: #8b949e;
    letter-spacing: 0.5px;
    text-transform: uppercase;
  }
}

.search-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  padding-bottom: 2px;
}

.search-summary {
  margin-top: 12px;
  font-size: 13px;
  color: #8b949e;
  border-top: 1px solid #30363d;
  padding-top: 10px;

  .highlight {
    color: #00d4ff;
    font-weight: 700;
    font-size: 15px;
  }
}

.table-card {
  padding: 0;
  overflow: hidden;

  :deep(.el-table) {
    border: none;
    background: #161b22;

    th.el-table__cell {
      background: #1a1f35 !important;
      color: #8b949e !important;
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.5px;
      text-transform: uppercase;
      padding: 12px 8px;
      border-bottom: 1px solid #30363d !important;
    }

    td.el-table__cell {
      padding: 12px 8px;
      border-bottom: 1px solid #21262d !important;
      vertical-align: middle;
    }

    tr:hover > td {
      background: #1f2937 !important;
    }

    .el-table__row--striped td {
      background: #1a1f35 !important;
    }

    .el-table__body tr:hover > td {
      background: #1f2937 !important;
    }

    .el-table__body tr.current-row > td {
      background: rgba(0, 212, 255, 0.06) !important;
    }
  }
}

.cell-username {
  display: flex;
  align-items: center;
  gap: 8px;

  .uname {
    font-weight: 600;
    color: #e6edf3;
    font-size: 14px;
  }
}

.status-on,
.status-off {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 600;
}

.status-on {
  color: #00ff88;
  .status-dot {
    background: #00ff88;
    box-shadow: 0 0 6px #00ff88;
  }
}

.status-off {
  color: #ff4757;
  .status-dot {
    background: #ff4757;
    box-shadow: 0 0 6px #ff4757;
  }
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.builtin-ops-hint {
  font-size: 12px;
  color: #8b949e;
  line-height: 1.4;
  padding: 0 4px;
}

.action-btns {
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: center;
  justify-content: center;

  .action-line {
    display: flex;
    gap: 8px;
    justify-content: center;
    align-items: center;
    flex-wrap: nowrap;
    min-height: 28px;
  }

  .btn-ico {
    margin-right: 3px;
    font-size: 13px;
    flex-shrink: 0;
  }

  .el-button {
    margin: 0 !important;
    min-width: 86px;
    padding: 5px 10px;
    font-size: 12px;
    white-space: nowrap;
    border-radius: 6px;

    &.el-button--danger,
    &.el-button--success {
      border-width: 1px !important;
    }

    &.el-button--danger {
      border-color: rgba(255, 71, 87, 0.5) !important;
      &:hover {
        background: rgba(255, 71, 87, 0.2) !important;
        border-color: #ff4757 !important;
        box-shadow: 0 0 8px rgba(255, 71, 87, 0.3) !important;
      }
    }

    &.el-button--success {
      border-color: rgba(0, 255, 136, 0.5) !important;
      &:hover {
        background: rgba(0, 255, 136, 0.2) !important;
        border-color: #00ff88 !important;
        box-shadow: 0 0 8px rgba(0, 255, 136, 0.3) !important;
      }
    }
  }
}

.pagination-bar {
  padding: 14px 16px 10px;
  border-top: 1px solid #30363d;
  display: flex;
  justify-content: flex-end;
}

.user-form-dialog .field-tip {
  margin: 6px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #8b949e;
}
</style>
