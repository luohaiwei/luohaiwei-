<template>
  <div class="category-container">
    <div class="page-header">
      <h2>设备分类管理</h2>
      <el-button type="primary" @click="handleAdd">添加分类</el-button>
    </div>

    <el-table :data="tableData" border stripe row-key="id">
      <el-table-column prop="categoryName" label="分类名称" min-width="140">
        <template #default="{ row }">
          <span v-if="row.parentId === 0 || row.parentId === null">{{ row.categoryName }}</span>
          <span v-else style="padding-left: 20px;">└ {{ row.categoryName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="categoryCode" label="分类编码" width="120" />
      <el-table-column prop="parentName" label="父分类" width="140">
        <template #default="{ row }">
          {{ row.parentId === 0 || row.parentId === null ? '-' : (row.parentName || '未知') }}
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="remark" label="备注" min-width="150">
        <template #default="{ row }">
          {{ row.remark || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="handleAddChild(row)" style="margin-right: 8px;">添加子分类</el-button>
          <el-button size="small" @click="handleEdit(row)" style="margin-right: 8px;">编辑</el-button>
          <el-button size="small" type="danger" plain @click="handleDelete(row)" :disabled="row.hasChildren">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="分类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="例如：显微镜类" />
        </el-form-item>
        <el-form-item label="分类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="英文/拼音大写，如 MICRO、CEN" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-select v-model="form.parentId" placeholder="请选择父分类" style="width: 100%">
            <el-option label="无（顶级分类）" :value="0" />
            <el-option v-for="item in parentOptions" :key="item.id" :label="item.categoryName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="选填，表格中不展示，仅备查" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategoryList, addCategory, updateCategory, deleteCategory } from '../../api/category'

const tableData = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref()
const parentOptions = ref([])

const form = ref({
  id: null,
  categoryName: '',
  categoryCode: '',
  parentId: 0,
  sort: 0,
  remark: ''
})

const rules = {
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  categoryCode: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]{1,32}$/, message: '编码为1–32位字母、数字、下划线或连字符', trigger: 'blur' }
  ]
}

function formatErrorMessage(e) {
  if (!e) return '操作失败'
  if (typeof e.message === 'string' && e.message) return e.message
  const d = e.response?.data
  if (d && typeof d.message === 'string') return d.message
  return '操作失败'
}

const fetchData = async () => {
  try {
    const res = await getCategoryList({ pageNum: pageNum.value, pageSize: pageSize.value })
    // 处理父子分类显示（添加hasChildren和parentName字段）
    const allList = res.list || []
    const parentIds = new Set(allList.filter(c => c.parentId !== 0 && c.parentId != null).map(c => c.parentId))
    tableData.value = allList.map(c => ({
      ...c,
      hasChildren: parentIds.has(c.id),
      parentName: c.parentId === 0 || c.parentId == null ? null :
        (allList.find(p => p.id === c.parentId)?.categoryName || '未知')
    }))
    total.value = res.total || 0
  } catch (e) {
    ElMessage.error(formatErrorMessage(e))
  }
}

const fetchParentOptions = async () => {
  try {
    const res = await getCategoryList({ pageNum: 1, pageSize: 100 })
    parentOptions.value = res.list || []
  } catch (e) {
    console.error(e)
  }
}

const handleAdd = async () => {
  Object.assign(form.value, { id: null, categoryName: '', categoryCode: '', parentId: 0, sort: 0, remark: '' })
  dialogTitle.value = '添加分类'
  await fetchParentOptions()
  dialogVisible.value = true
}

const handleAddChild = async (parentRow) => {
  Object.assign(form.value, { id: null, categoryName: '', categoryCode: '', parentId: parentRow.id, sort: 0, remark: '' })
  dialogTitle.value = '添加子分类 - ' + parentRow.categoryName
  await fetchParentOptions()
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  Object.assign(form.value, { ...row })
  dialogTitle.value = '编辑分类'
  await fetchParentOptions()
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (e) {
      ElMessage.error(formatErrorMessage(e))
    }
  })
}

const handleSubmit = async () => {
  await formRef.value.validate()
  try {
    // 只发送必要的字段，排除后端不需要的字段
    const submitData = {
      id: form.value.id,
      categoryName: form.value.categoryName,
      categoryCode: form.value.categoryCode,
      parentId: form.value.parentId,
      sort: form.value.sort,
      remark: form.value.remark
    }
    if (form.value.id) {
      await updateCategory(submitData)
      ElMessage.success('更新成功')
    } else {
      await addCategory(submitData)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    ElMessage.error(formatErrorMessage(e))
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.category-container {
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

/* 编辑分类弹窗深色主题 */
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
</style>
