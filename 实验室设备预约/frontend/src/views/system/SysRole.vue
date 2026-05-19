<template>
  <div class="sys-role-container">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>角色管理</h2>
      </div>
      <el-button type="primary" class="add-btn" @click="openAdd">
        <el-icon><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <div class="page-card search-card">
      <div class="search-row">
        <div class="search-field">
          <label>角色名称</label>
          <el-input
            v-model="searchForm.roleName"
            placeholder="角色名称"
            clearable
            @keyup.enter="loadData"
          />
        </div>
        <div class="search-actions">
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </div>
      </div>
      <div class="search-summary">
        共 <span class="highlight">{{ total }}</span> 个角色
      </div>
    </div>

    <div class="page-card table-card">
      <el-table :data="tableData" border stripe highlight-current-row>
        <el-table-column prop="roleCode" label="角色编码" min-width="160">
          <template #default="{ row }">
            <span class="role-code">{{ row.roleCode }}</span>
            <el-tag v-if="row.isSystem === 1" size="small" type="danger">内置</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column
          prop="description"
          label="角色描述"
          min-width="200"
          show-overflow-tooltip
        />
        <el-table-column prop="userCount" label="用户数量" width="100" align="center">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.userCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="createTime"
          label="创建时间"
          min-width="170"
          :formatter="formatDate"
        />
        <el-table-column label="操作" width="360" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button type="primary" plain size="small" @click="openEdit(row)">
                <el-icon class="btn-ico"><Edit /></el-icon>编辑
              </el-button>
              <el-button type="warning" plain size="small" @click="openPermission(row)">
                <el-icon class="btn-ico"><Key /></el-icon>权限配置
              </el-button>
              <el-button
                v-if="row.isSystem !== 1"
                type="info"
                plain
                size="small"
                @click="handleDelete(row)"
              >
                <el-icon class="btn-ico"><Delete /></el-icon>删除
              </el-button>
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

    <!-- 新增/编辑角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="520px"
      @close="resetForm"
      @opened="onDialogOpened"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="role-form-dialog"
      >
        <el-form-item label="角色编码" prop="roleCode">
          <el-input
            v-model="form.roleCode"
            :disabled="isEdit"
            placeholder="唯一标识，如 SYSTEM_ADMIN"
          />
          <p v-if="isEdit" class="field-tip">角色编码是唯一标识，创建后不可修改。</p>
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="角色显示名称" />
        </el-form-item>
        <el-form-item label="角色描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="角色职责描述（选填）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置弹窗 -->
    <el-dialog
      v-model="permDialogVisible"
      title="权限配置"
      width="680px"
      @close="permDialogVisible = false"
      @opened="onPermDialogOpened"
    >
      <div class="perm-config-wrapper">
        <div class="perm-tip">
          <el-icon><InfoFilled /></el-icon>
          勾选该角色可访问的菜单和功能按钮。权限变更后需重新登录生效。
        </div>
        <el-tree
          v-if="permDialogVisible"
          ref="permTreeRef"
          :data="permTreeData"
          :props="{ label: 'permName', children: 'children' }"
          show-checkbox
          node-key="id"
          default-expand-all
          highlight-current
          class="perm-tree"
        >
          <template #default="{ data }">
            <span class="tree-node-content">
              <span class="node-label">{{ data.permName }}</span>
              <span v-if="data.permType === 'MENU'" class="perm-tag perm-tag--menu">菜单</span>
              <span v-else-if="data.permType === 'BUTTON'" class="perm-tag perm-tag--btn">按钮</span>
              <span v-else-if="data.permType === 'API'" class="perm-tag perm-tag--api">接口</span>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="permLoading" @click="handleSavePermission"
          >保存配置</el-button
        >
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import {
  getRoleList,
  addRole,
  updateRole,
  deleteRole,
  getRolePermissions,
  saveRolePermissions
} from '@/api/user';
import { getPermissionTree } from '@/api/user';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Edit, Delete, Key, Plus, InfoFilled } from '@element-plus/icons-vue';
import dayjs from 'dayjs';

const searchForm = reactive({ roleName: '' });
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const dialogVisible = ref(false);
const permDialogVisible = ref(false);
const isEdit = ref(false);
const submitLoading = ref(false);
const permLoading = ref(false);
const formRef = ref();
const permTreeRef = ref();
const currentRole = ref(null);
const permTreeData = ref([]);

const form = reactive({
  id: undefined,
  roleCode: '',
  roleName: '',
  description: ''
});

const rules = {
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
};

const formatDate = row => dayjs(row.createTime).format('YYYY-MM-DD HH:mm');

const loadData = async () => {
  try {
    const res = await getRoleList({
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
  searchForm.roleName = '';
  pageNum.value = 1;
  loadData();
};

const onDialogOpened = () => {
  formRef.value?.clearValidate();
};

const openAdd = () => {
  isEdit.value = false;
  resetForm();
  dialogVisible.value = true;
};

const openEdit = row => {
  isEdit.value = true;
  // 仅提交后端 SysRole 可反序列化字段，避免 createTime 等破坏 JSON 解析导致 400
  form.id = row.id;
  form.roleCode = row.roleCode;
  form.roleName = row.roleName;
  form.description = row.description ?? '';
  dialogVisible.value = true;
};

const resetForm = () => {
  form.id = undefined;
  form.roleCode = '';
  form.roleName = '';
  form.description = '';
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async valid => {
    if (!valid) return;
    submitLoading.value = true;
    try {
      if (isEdit.value) {
        await updateRole(form);
      } else {
        await addRole(form);
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

const handleDelete = async row => {
  try {
    await ElMessageBox.confirm(
      `确定删除角色「${row.roleName}」？删除后该角色下的用户将失去对应权限。`,
      '提示',
      { type: 'warning' }
    );
    await deleteRole(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败');
  }
};

const loadPermTree = async () => {
  try {
    const res = await getPermissionTree();
    permTreeData.value = res || [];
  } catch (e) {
    console.error('加载权限树失败', e);
    permTreeData.value = [];
  }
};

const loadRolePerms = async roleId => {
  try {
    const res = await getRolePermissions(roleId);
    return res || [];
  } catch (e) {
    console.error('加载角色权限失败', e);
    return [];
  }
};

const openPermission = async row => {
  currentRole.value = row;
  permDialogVisible.value = true;
};

const onPermDialogOpened = async () => {
  await loadPermTree();
  const checkedIds = await loadRolePerms(currentRole.value.id);
  if (permTreeRef.value) {
    permTreeRef.value.setCheckedKeys(checkedIds);
  }
};

const handleSavePermission = async () => {
  if (!currentRole.value) return;
  const checkedNodes = permTreeRef.value?.getCheckedNodes() || [];
  const halfCheckedNodes = permTreeRef.value?.getHalfCheckedNodes() || [];
  const allChecked = [
    ...new Set([...checkedNodes, ...halfCheckedNodes].map(n => n.id).filter(id => id != null))
  ];
  permLoading.value = true;
  try {
    await saveRolePermissions(currentRole.value.id, allChecked);
    ElMessage.success('权限配置保存成功');
    permDialogVisible.value = false;
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  } finally {
    permLoading.value = false;
  }
};

onMounted(() => {
  loadData();
});
</script>

<style scoped lang="scss">
.sys-role-container {
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
  h2 {
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
}

.add-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  padding: 10px 20px;
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

.role-code {
  font-weight: 600;
  color: #e6edf3;
  font-size: 13px;
}

.pagination-bar {
  padding: 14px 16px 10px;
  border-top: 1px solid #30363d;
  display: flex;
  justify-content: flex-end;
}

.role-form-dialog .field-tip {
  margin: 6px 0 0;
  font-size: 12px;
  color: #8b949e;
}

.action-btns {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  align-items: center;
  justify-content: center;
  .btn-ico {
    margin-right: 3px;
    font-size: 13px;
    flex-shrink: 0;
  }
  .el-button {
    padding: 5px 10px;
    font-size: 12px;
    white-space: nowrap;
    border-radius: 6px;
  }
}

.perm-config-wrapper {
  max-height: 480px;
  overflow-y: auto;
  padding-right: 8px;
}

.perm-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #0d1117;
  border: 1px solid #30363d;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #8b949e;
  .el-icon {
    color: #00d4ff;
    flex-shrink: 0;
  }
}

.perm-tree {
  background: transparent;
  :deep(.el-tree-node__content) {
    color: #e6edf3;
    &:hover {
      background: #1f2937;
    }
  }
  :deep(.el-tree-node.is-checked > .el-tree-node__content) {
    background: rgba(0, 212, 255, 0.1);
  }
  :deep(.el-checkbox__label) {
    color: #e6edf3;
  }
}

.tree-node-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.node-label {
  color: #e6edf3;
  font-size: 13px;
}

.perm-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 600;
  line-height: 1.4;
  flex-shrink: 0;
  &--menu {
    background: rgba(0, 212, 255, 0.15);
    color: #00d4ff;
    border: 1px solid rgba(0, 212, 255, 0.3);
  }
  &--btn {
    background: rgba(230, 162, 60, 0.15);
    color: #e6a23c;
    border: 1px solid rgba(230, 162, 60, 0.3);
  }
  &--api {
    background: rgba(103, 58, 183, 0.15);
    color: #9c6be2;
    border: 1px solid rgba(103, 58, 183, 0.3);
  }
}
</style>
