<template>
  <div class="laboratory-container">
    <div class="page-header">
      <h2>实验室信息管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索实验室名称/编码/位置"
          style="width: 250px; margin-right: 10px"
          clearable
          @clear="fetchData"
          @keyup.enter="fetchData"
        >
          <template #append>
            <el-button :icon="Search" @click="fetchData" />
          </template>
        </el-input>
        <el-select v-model="filterType" placeholder="实验室类型" clearable style="width: 150px; margin-right: 10px" @change="fetchData">
          <el-option label="物理" value="物理" />
          <el-option label="化学" value="化学" />
          <el-option label="生物" value="生物" />
          <el-option label="计算机" value="计算机" />
          <el-option label="其他" value="其他" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 120px; margin-right: 10px" @change="fetchData">
          <el-option label="正常" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleAdd">添加实验室</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-cards">
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%)">
          <el-icon :size="24"><OfficeBuilding /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.activeCount }}</div>
          <div class="stat-label">正常实验室</div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%)">
          <el-icon :size="24"><CloseBold /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.disabledCount }}</div>
          <div class="stat-label">已停用</div>
        </div>
      </el-card>
      <el-card class="stat-card" shadow="hover">
        <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)">
          <el-icon :size="24"><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ statistics.totalCapacity }}</div>
          <div class="stat-label">总容纳人数</div>
        </div>
      </el-card>
    </div>

    <!-- 实验室列表 -->
    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="labCode" label="实验室编码" width="150" />
      <el-table-column prop="labName" label="实验室名称" min-width="150">
        <template #default="{ row }">
          <el-tag type="primary" effect="plain">{{ row.labName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="labType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getTypeTagType(row.labType)">{{ row.labType || '其他' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="location" label="位置" min-width="150">
        <template #default="{ row }">
          <div>{{ row.building }} {{ row.floor }} {{ row.location }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="capacity" label="容量" width="80" align="center">
        <template #default="{ row }">
          <el-tag type="info">{{ row.capacity }}人</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responsibleName" label="负责人" width="100" />
      <el-table-column prop="responsiblePhone" label="联系电话" width="130" />
      <el-table-column prop="openTime" label="开放时间" width="120">
        <template #default="{ row }">
          {{ row.openTime }} - {{ row.closeTime }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="fetchData"
      @current-change="fetchData"
    />

    <!-- 添加/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="实验室编码" prop="labCode">
              <el-input v-model="form.labCode" placeholder="如：LAB-PHYS-001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实验室名称" prop="labName">
              <el-input v-model="form.labName" placeholder="请输入实验室名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="实验室类型" prop="labType">
              <el-select v-model="form.labType" placeholder="请选择类型" style="width: 100%">
                <el-option label="物理" value="物理" />
                <el-option label="化学" value="化学" />
                <el-option label="生物" value="生物" />
                <el-option label="计算机" value="计算机" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在建筑" prop="building">
              <el-input v-model="form.building" placeholder="如：博学楼" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所在楼层" prop="floor">
              <el-input v-model="form.floor" placeholder="如：3楼" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="详细位置" prop="location">
              <el-input v-model="form.location" placeholder="如：301室" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="面积(m²)" prop="area">
              <el-input-number v-model="form.area" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="容纳人数" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人" prop="responsibleName">
              <el-input v-model="form.responsibleName" placeholder="请输入负责人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="responsiblePhone">
              <el-input v-model="form.responsiblePhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="负责人邮箱" prop="responsibleEmail">
              <el-input v-model="form.responsibleEmail" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :label="1">正常</el-radio>
                <el-radio :label="0">停用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开放开始时间" prop="openTime">
              <el-time-select
                v-model="form.openTime"
                placeholder="选择开始时间"
                start="06:00"
                step="00:30"
                end="23:30"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开放结束时间" prop="closeTime">
              <el-time-select
                v-model="form.closeTime"
                placeholder="选择结束时间"
                start="06:00"
                step="00:30"
                end="23:30"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="实验室简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入实验室简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, OfficeBuilding, CloseBold, User } from '@element-plus/icons-vue'
import {
  getLaboratoryList,
  addLaboratory,
  updateLaboratory,
  deleteLaboratory,
  getLaboratoryStatistics
} from '../../api/laboratory'

const tableData = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const filterType = ref('')
const filterStatus = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const statistics = reactive({
  activeCount: 0,
  disabledCount: 0,
  totalCapacity: 0
})

const defaultForm = {
  labCode: '',
  labName: '',
  labType: '',
  building: '',
  floor: '',
  location: '',
  area: null,
  capacity: null,
  responsibleName: '',
  responsiblePhone: '',
  responsibleEmail: '',
  openTime: '08:00',
  closeTime: '20:00',
  description: '',
  status: 1
}

const form = reactive({ ...defaultForm })

const rules = {
  labCode: [{ required: true, message: '请输入实验室编码', trigger: 'blur' }],
  labName: [{ required: true, message: '请输入实验室名称', trigger: 'blur' }]
}

const getTypeTagType = (type) => {
  const types = {
    '物理': 'primary',
    '化学': 'warning',
    '生物': 'success',
    '计算机': 'info'
  }
  return types[type] || 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLaboratoryList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value,
      labType: filterType.value,
      status: filterStatus.value
    })
    tableData.value = res.list || []
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(e.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const res = await getLaboratoryStatistics()
    statistics.activeCount = res.activeCount || 0
    statistics.disabledCount = res.disabledCount || 0
    statistics.totalCapacity = res.totalCapacity || 0
  } catch (e) {
    console.error('获取统计数据失败', e)
  }
}

const handleAdd = () => {
  Object.assign(form, { ...defaultForm })
  dialogTitle.value = '添加实验室'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  Object.assign(form, { ...row })
  dialogTitle.value = '编辑实验室'
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除实验室"${row.labName}"吗？此操作不可恢复！`, '警告', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteLaboratory(row.id)
      ElMessage.success('删除成功')
      fetchData()
      fetchStatistics()
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

const toggleStatus = (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '停用'
  ElMessageBox.confirm(`确定要${action}实验室"${row.labName}"吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await updateLaboratory(row.id, { ...row, status: newStatus })
      ElMessage.success(`${action}成功`)
      fetchData()
      fetchStatistics()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    }
  }).catch(() => {})
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    if (form.id) {
      await updateLaboratory(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await addLaboratory(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
    fetchStatistics()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  fetchData()
  fetchStatistics()
})
</script>

<style scoped>
.laboratory-container {
  color: #E6EDF3;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #E6EDF3;
}

.header-actions {
  display: flex;
  align-items: center;
}

.stats-cards {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 16px;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #E6EDF3;
}

.stat-label {
  font-size: 14px;
  color: #8B949E;
  margin-top: 4px;
}

:deep(.el-table) {
  background: #161B22;
  color: #E6EDF3;
}

:deep(.el-table__header) {
  background: #21262D;
}

:deep(.el-pagination) {
  margin-top: 20px;
  justify-content: flex-end;
}

:deep(.el-dialog) {
  background: #161B22;
  border: 1px solid #30363D;
}

:deep(.el-dialog__header) {
  background: #21262D;
  border-bottom: 1px solid #30363D;
  margin-right: 0;
}

:deep(.el-dialog__title) {
  color: #E6EDF3;
}

:deep(.el-dialog__body) {
  background: #0D1117;
  color: #E6EDF3;
}

:deep(.el-dialog__footer) {
  background: #161B22;
  border-top: 1px solid #30363D;
}

:deep(.el-form-item__label) {
  color: #8B949E;
}

:deep(.el-input__wrapper),
:deep(.el-select .el-input__wrapper),
:deep(.el-textarea__inner) {
  background: #161B22 !important;
  color: #E6EDF3 !important;
  box-shadow: 0 0 0 1px #30363D inset !important;
}

:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  color: #E6EDF3 !important;
}

:deep(.el-input__inner::placeholder),
:deep(.el-textarea__inner::placeholder) {
  color: #8B949E !important;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-input-number .el-input__wrapper) {
  background: #161B22 !important;
  box-shadow: 0 0 0 1px #30363D inset !important;
}

:deep(.el-input-number__decrease),
:deep(.el-input-number__increase) {
  background: #21262D !important;
  color: #8B949E !important;
  border-color: #30363D !important;
}

:deep(.el-input-number__decrease:hover),
:deep(.el-input-number__increase:hover) {
  color: #00D4FF !important;
}

:deep(.el-card) {
  background: #161B22;
  border-color: #30363D;
}
</style>
