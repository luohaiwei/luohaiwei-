<template>
  <div class="sys-permission">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>权限分配</h2>
      </div>
    </div>

    <div class="page-card role-pick-card">
      <div class="role-pick-row">
        <span class="role-pick-label">目标角色</span>
        <el-select
          v-model="selectedRoleId"
          placeholder="请先选择要配置的角色"
          filterable
          clearable
          style="min-width: 280px"
          @change="onRoleChange"
        >
          <el-option
            v-for="r in roleOptions"
            :key="r.id"
            :label="`${r.roleName}（${r.roleCode}）`"
            :value="r.id"
          />
        </el-select>
        <span v-if="!selectedRoleId" class="role-pick-hint">未选择角色时无法保存，避免误改系统管理员权限</span>
      </div>
      <el-alert
        v-if="isBuiltinRole"
        type="info"
        :closable="false"
        show-icon
        class="builtin-role-alert"
        title="当前为系统预置基础角色：功能权限树与任务书五种角色模型一致，不可在此修改。需要差异化授权时请新建「自定义角色」并为其分配菜单/按钮权限。"
      />
    </div>

    <div class="permission-layout">
      <!-- 左侧：功能权限树 -->
      <div class="perm-panel">
        <div class="panel-header">
          <span>功能权限配置</span>
        </div>
        <div class="perm-tree-wrapper">
          <el-tree
            ref="permTreeRef"
            :data="permTreeData"
            :props="{ label: 'permName', children: 'children' }"
            :show-checkbox="!isBuiltinRole"
            check-strictly
            node-key="id"
            default-expand-all
            highlight-current
            class="perm-tree"
          >
            <template #default="{ data }">
              <span class="tree-node-label">
                <el-icon class="node-icon"><component :is="getPermIcon(data.permType)" /></el-icon>
                <span>{{ data.permName }}</span>
                <el-tag
                  v-if="data.permType === 'BUTTON'"
                  size="small"
                  type="info"
                  style="margin-left: 6px"
                  >按钮</el-tag
                >
                <el-tag
                  v-if="data.permType === 'MENU'"
                  size="small"
                  type="success"
                  style="margin-left: 6px"
                  >菜单</el-tag
                >
                <el-tag
                  v-if="data.permType === 'API'"
                  size="small"
                  type="warning"
                  style="margin-left: 6px"
                  >接口</el-tag
                >
              </span>
            </template>
          </el-tree>
        </div>
        <div class="panel-footer">
          <el-button
            type="primary"
            :loading="savePermLoading"
            :disabled="isBuiltinRole"
            @click="handleSavePermission"
            >保存权限配置</el-button
          >
        </div>
      </div>

      <!-- 右侧：数据权限范围 -->
      <div class="data-panel">
        <div class="panel-header">
          <span>数据权限范围配置</span>
        </div>
        <div class="data-scope-wrapper">
          <el-form :model="dataScope" label-width="140px" class="data-scope-form">
            <el-form-item label="可管理的数据范围">
              <el-radio-group v-model="dataScope.scopeType">
                <el-radio label="ALL">全部数据（所有实验室/设备/预约记录）</el-radio>
                <el-radio label="DEPT">本实验室数据（仅限所属实验室）</el-radio>
                <el-radio label="SELF">本人数据（仅本人提交/操作的记录）</el-radio>
                <el-radio label="CUSTOM">自定义数据范围</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="dataScope.scopeType === 'CUSTOM'" label="自定义实验室">
              <el-select
                v-model="dataScope.customLabIds"
                multiple
                placeholder="选择可访问的实验室"
                style="width: 100%"
              >
                <el-option
                  v-for="lab in labList"
                  :key="lab.labName"
                  :label="lab.labName"
                  :value="lab.labName"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="设备数据可见性">
              <el-checkbox-group v-model="dataScope.deviceScope">
                <el-checkbox label="VIEW">查看设备信息</el-checkbox>
                <el-checkbox label="EDIT">编辑设备信息</el-checkbox>
                <el-checkbox label="DELETE">删除设备</el-checkbox>
                <el-checkbox label="STATUS">变更设备状态</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="预约数据可见性">
              <el-checkbox-group v-model="dataScope.bookingScope">
                <el-checkbox label="VIEW">查看预约记录</el-checkbox>
                <el-checkbox label="AUDIT">审核预约申请</el-checkbox>
                <el-checkbox label="CANCEL">取消预约</el-checkbox>
                <el-checkbox label="EXPORT">导出预约数据</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="用户数据可见性">
              <el-checkbox-group v-model="dataScope.userScope">
                <el-checkbox label="VIEW">查看用户信息</el-checkbox>
                <el-checkbox label="EDIT">编辑用户</el-checkbox>
                <el-checkbox label="ROLE">分配用户角色</el-checkbox>
                <el-checkbox label="DELETE">删除用户</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveScopeLoading" @click="handleSaveDataScope"
                >保存数据权限</el-button
              >
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, computed } from 'vue';
import {
  getPermissionTree,
  getRolePermissions,
  saveRolePermissions,
  getDataScope,
  saveDataScope,
  getPermLabList,
  getRoleList
} from '@/api/user';
import { ElMessage } from 'element-plus';
import { Folder, Operation, Link } from '@element-plus/icons-vue';

const permTreeRef = ref();
const permTreeData = ref([]);
const savePermLoading = ref(false);
const saveScopeLoading = ref(false);
const labList = ref([]);
const roleOptions = ref([]);
const selectedRoleId = ref(null);

/** 系统预置五大基础角色：功能权限由后端拒绝保存，前端同步禁用编辑 */
const isBuiltinRole = computed(() => {
  const r = roleOptions.value.find(x => x.id === selectedRoleId.value);
  return r?.isSystem === 1;
});

const dataScope = reactive({
  scopeType: 'ALL',
  customLabIds: [],
  deviceScope: ['VIEW', 'EDIT'],
  bookingScope: ['VIEW'],
  userScope: ['VIEW']
});

const getPermIcon = type => {
  const iconMap = { MENU: 'Folder', BUTTON: 'Operation', API: 'Link' };
  return iconMap[type] || 'Folder';
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

const loadDataScope = async () => {
  if (!selectedRoleId.value) {
    dataScope.scopeType = 'ALL';
    dataScope.customLabIds = [];
    dataScope.deviceScope = ['VIEW', 'EDIT'];
    dataScope.bookingScope = ['VIEW'];
    dataScope.userScope = ['VIEW'];
    return;
  }
  try {
    const res = await getDataScope(selectedRoleId.value);
    if (res) {
      dataScope.scopeType = res.scopeType || 'ALL';
      dataScope.customLabIds = res.customLabIds || [];
      dataScope.deviceScope = res.deviceScope || ['VIEW', 'EDIT'];
      dataScope.bookingScope = res.bookingScope || ['VIEW'];
      dataScope.userScope = res.userScope || ['VIEW'];
    }
  } catch (e) {
    console.error('加载数据权限失败', e);
  }
};

const loadLabs = async () => {
  try {
    const res = await getPermLabList();
    labList.value = res || [];
  } catch (e) {
    console.error('加载实验室列表失败', e);
    labList.value = [];
  }
};

const handleSavePermission = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请先选择目标角色');
    return;
  }
  if (isBuiltinRole.value) {
    ElMessage.warning('预置基础角色的功能权限不可修改');
    return;
  }
  // 仅提交实际勾选的节点；不再合并 halfChecked（半选父节点），否则会保存父级 ID，刷新后子权限全部被勾选
  const allChecked = [...new Set((permTreeRef.value?.getCheckedKeys(false) || []).filter(id => id != null))];
  savePermLoading.value = true;
  try {
    await saveRolePermissions(selectedRoleId.value, allChecked);
    ElMessage.success('功能权限配置保存成功');
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  } finally {
    savePermLoading.value = false;
  }
};

const handleSaveDataScope = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请先选择目标角色');
    return;
  }
  saveScopeLoading.value = true;
  try {
    await saveDataScope(selectedRoleId.value, dataScope);
    ElMessage.success('数据权限保存成功');
  } catch (e) {
    ElMessage.error(e.message || '保存失败');
  } finally {
    saveScopeLoading.value = false;
  }
};

const loadRoleOptions = async () => {
  try {
    const res = await getRoleList({ pageNum: 1, pageSize: 500 });
    roleOptions.value = res.list || [];
  } catch (e) {
    console.error(e);
    roleOptions.value = [];
  }
};

const loadRolePermsForTree = async () => {
  if (!selectedRoleId.value || !permTreeRef.value) return;
  const checkedIds = await getRolePermissions(selectedRoleId.value);
  permTreeRef.value.setCheckedKeys(checkedIds || []);
};

const onRoleChange = async () => {
  await loadDataScope();
  await nextTick();
  await loadRolePermsForTree();
};

onMounted(async () => {
  await loadRoleOptions();
  await loadPermTree();
  await loadLabs();
});
</script>

<style scoped lang="scss">
.sys-permission {
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

.role-pick-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 14px 20px;
  gap: 12px;
}

.builtin-role-alert {
  margin: 0;
}

.role-pick-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.role-pick-label {
  font-size: 13px;
  font-weight: 600;
  color: #8b949e;
}

.role-pick-hint {
  font-size: 12px;
  color: #f85149;
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
  }
  .header-sub {
    font-size: 12px;
    color: #8b949e;
  }
}

.permission-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: start;
}

.perm-panel,
.data-panel {
  background: #161b22;
  border: 1px solid #30363d;
  border-radius: 10px;
  overflow: hidden;
}

.panel-header {
  padding: 14px 20px;
  background: #21262d;
  border-bottom: 1px solid #30363d;
  font-size: 14px;
  font-weight: 700;
  color: #e6edf3;
  letter-spacing: 0.5px;
}

.perm-tree-wrapper {
  padding: 16px;
  max-height: 500px;
  overflow-y: auto;
}

.panel-footer {
  padding: 14px 20px;
  border-top: 1px solid #30363d;
  display: flex;
  justify-content: flex-end;
}

.tree-node-label {
  display: flex;
  align-items: center;
  gap: 6px;
  .node-icon {
    font-size: 14px;
    color: #00d4ff;
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
    font-size: 13px;
  }
}

.data-scope-wrapper {
  padding: 20px;
}

.data-scope-form {
  :deep(.el-form-item__label) {
    color: #8b949e;
    font-weight: 600;
  }
  :deep(.el-radio__label) {
    color: #e6edf3;
    font-size: 13px;
    line-height: 1.8;
  }
  :deep(.el-checkbox__label) {
    color: #e6edf3;
    font-size: 13px;
  }
}

@media (max-width: 1100px) {
  .permission-layout {
    grid-template-columns: 1fr;
  }
}
</style>
