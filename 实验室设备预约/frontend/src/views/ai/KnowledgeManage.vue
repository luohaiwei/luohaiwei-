<template>
  <div class="knowledge-manage">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="openAdd">新增知识</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select
        v-model="filterForm.category"
        placeholder="选择分类"
        clearable
        class="filter-category"
        @change="loadData"
      >
        <el-option label="设备操作手册" value="设备操作手册" />
        <el-option label="实验流程" value="实验流程" />
        <el-option label="故障排查" value="故障排查" />
        <el-option label="安全规范" value="安全规范" />
        <el-option label="常见问题" value="常见问题" />
      </el-select>
      <el-input v-model="filterForm.keyword" placeholder="搜索问题/答案" clearable style="width:200px" @keyup.enter="loadData" />
      <el-select v-model="filterForm.status" placeholder="状态" clearable @change="loadData" style="width:120px">
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="loadData">搜索</el-button>
      <el-button @click="resetFilter">重置</el-button>
    </div>

    <el-table :data="list" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="category" label="分类" width="110">
        <template #default="{ row }">
          <el-tag size="small">{{ row.category || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deviceId" label="关联设备" width="130">
        <template #default="{ row }">
          <span v-if="row.deviceId" style="font-size:12px">{{ row.deviceName || getDeviceName(row.deviceId) }}</span>
          <span v-else style="color:#6E7681">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="question" label="问题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="keywords" label="关键词" width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.keywords || '-' }}</template>
      </el-table-column>
      <el-table-column label="危险" width="65" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isDanger === 1" type="danger" size="small">是</el-tag>
          <span v-else style="color:#6E7681">否</span>
        </template>
      </el-table-column>
      <el-table-column label="有引导" width="70" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.guideType === 'IMAGE'" type="success" size="small">是</el-tag>
          <span v-else style="color:#6E7681">否</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="65" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="openEdit(row)">编辑</el-button>
          <el-button type="danger" size="small" text @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.pageNum"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="800px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类" prop="category">
              <el-select v-model="form.category" placeholder="请选择分类" style="width:100%">
                <el-option label="设备操作手册" value="设备操作手册" />
                <el-option label="实验流程" value="实验流程" />
                <el-option label="故障排查" value="故障排查" />
                <el-option label="安全规范" value="安全规范" />
                <el-option label="常见问题" value="常见问题" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联设备" prop="deviceId">
              <el-select v-model="form.deviceId" placeholder="请选择关联设备（可选）" style="width:100%" clearable filterable :loading="deviceLoading">
                <el-option v-for="d in allDevices" :key="d.id" :label="d.deviceName + ' (' + d.deviceNo + ')'" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="问题" prop="question">
          <el-input v-model="form.question" placeholder="请输入问题标题，如：如何进行日常维护？" />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input v-model="form.answer" type="textarea" :rows="4" placeholder="请输入答案内容" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="用逗号分隔，如：开机,启动,操作" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="危险操作">
              <el-switch v-model="form.isDanger" :active-value="1" :inactive-value="0" />
              <span style="margin-left:8px;color:#8B949E;font-size:12px">开启后预警</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="引导类型">
              <el-select v-model="form.guideType" style="width:100%">
                <el-option label="纯文本" value="TEXT" />
                <el-option label="图文引导" value="IMAGE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="预计时长(分钟)">
              <el-input-number v-model="form.guideDuration" :min="0" :max="999" placeholder="可选" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="操作引导步骤" v-if="form.guideType === 'IMAGE'">
          <div class="guide-steps">
            <div v-for="(step, index) in guideStepsList" :key="index" class="guide-step-item">
              <div class="guide-step-header">
                <span class="step-num">步骤 {{ step.step }}</span>
                <el-button type="danger" size="small" link @click="removeGuideStep(index)">删除</el-button>
              </div>
              <el-form-item label="步骤标题">
                <el-input v-model="step.title" placeholder="如：打开仪器电源" />
              </el-form-item>
              <el-form-item label="步骤内容">
                <el-input v-model="step.content" type="textarea" :rows="2" placeholder="详细操作说明" />
              </el-form-item>
              <el-form-item label="安全警示">
                <el-input v-model="step.warning" placeholder="该步骤的安全注意事项（可选）" />
              </el-form-item>
              <el-form-item label="步骤图片">
                <el-upload
                  class="avatar-uploader"
                  :show-file-list="false"
                  :on-success="(response, file) => handleStepImageUpload(index, response, file)"
                  :on-error="handleUploadError"
                  :before-upload="beforeUpload"
                  :headers="getUploadHeaders()"
                  action="/api/device/upload-image"
                >
                  <img v-if="step.image" :src="step.image" class="step-image" />
                  <el-button v-else type="primary" size="small">上传图片</el-button>
                </el-upload>
                <div v-if="step.image" class="image-actions">
                  <el-button type="danger" size="small" link @click="removeStepImage(index)">删除图片</el-button>
                </div>
              </el-form-item>
            </div>
            <el-button type="primary" plain size="small" @click="addGuideStep">+ 添加步骤</el-button>
          </div>
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getKnowledgeList, addKnowledge, updateKnowledge, deleteKnowledge } from '@/api/knowledge'
import { getAllDevices } from '@/api/device'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const loading = ref(false)
const saveLoading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增知识')
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref()
const allDevices = ref([])
const deviceLoading = ref(false)
const guideStepsList = ref([])

const pagination = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const filterForm = reactive({ category: '', keyword: '', status: null })

const defaultForm = () => ({
  category: '',
  deviceId: null,
  question: '',
  answer: '',
  keywords: '',
  isDanger: 0,
  status: 1,
  guideType: 'TEXT',
  guideSteps: null,
  guideDuration: null
})

const form = reactive(defaultForm())

const rules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

const loadDevices = async () => {
  deviceLoading.value = true
  try {
    const res = await getAllDevices()
    allDevices.value = res || []
  } catch (e) {
    console.error(e)
    allDevices.value = []
  } finally {
    deviceLoading.value = false
  }
}

const addGuideStep = () => {
  guideStepsList.value.push({ step: guideStepsList.value.length + 1, title: '', content: '', warning: '', image: '' })
}

const removeGuideStep = (index) => {
  guideStepsList.value.splice(index, 1)
  guideStepsList.value.forEach((item, i) => { item.step = i + 1 })
}

const handleStepImageUpload = (index, response, file) => {
  if (response && response.path) {
    // 确保图片路径包含 /api 前缀
    if (!response.path.startsWith('/api')) {
      guideStepsList.value[index].image = '/api' + response.path
    } else {
      guideStepsList.value[index].image = response.path
    }
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败: ' + (response.message || '未知错误'))
  }
}

const handleUploadError = (error) => {
  ElMessage.error('上传失败，请重试')
  console.error('上传错误:', error)
}

const beforeUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isJpgOrPng) {
    ElMessage.error('只能上传 JPG/PNG 图片')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
  }
  return isJpgOrPng && isLt2M
}

const removeStepImage = (index) => {
  guideStepsList.value[index].image = ''
  ElMessage.success('图片已删除')
}

const getUploadHeaders = () => {
  const userStore = useUserStore()
  const headers = {}
  if (userStore.token) {
    headers['Authorization'] = 'Bearer ' + userStore.token
  }
  return headers
}

const loadGuideSteps = (stepsJson) => {
  guideStepsList.value = []
  if (stepsJson && typeof stepsJson === 'string') {
    try {
      const steps = JSON.parse(stepsJson)
      if (Array.isArray(steps)) {
        guideStepsList.value = steps
        guideStepsList.value.forEach((item, i) => {
          if (item.step === undefined) item.step = i + 1
        })
      }
    } catch (e) {
      console.error('解析引导步骤失败:', e)
      const lines = stepsJson.split('\n').filter(l => l.trim())
      if (lines.length > 0) {
        guideStepsList.value = lines.map((line, i) => ({ step: i + 1, title: `步骤 ${i + 1}`, content: line.trim(), warning: '' }))
      }
    }
  } else if (Array.isArray(stepsJson)) {
    guideStepsList.value = stepsJson
  }
}

const getGuideStepsJson = () => {
  if (!guideStepsList.value || guideStepsList.value.length === 0) return ''
  const validSteps = guideStepsList.value.filter(s => s.title || s.content)
  if (validSteps.length === 0) return ''
  validSteps.forEach((item, i) => { item.step = i + 1 })
  return JSON.stringify(validSteps)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getKnowledgeList({ pageNum: pagination.pageNum, pageSize: pagination.pageSize, ...filterForm })
    list.value = res.list || []
    pagination.total = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetFilter = () => {
  filterForm.category = ''
  filterForm.keyword = ''
  filterForm.status = null
  pagination.pageNum = 1
  loadData()
}

const openAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增知识'
  editingId.value = null
  Object.assign(form, defaultForm())
  guideStepsList.value = []
  loadDevices()
  dialogVisible.value = true
}

const openEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑知识'
  editingId.value = row.id
  Object.assign(form, {
    category: row.category || '',
    deviceId: row.deviceId || null,
    question: row.question || '',
    answer: row.answer || '',
    keywords: row.keywords || '',
    isDanger: row.isDanger || 0,
    status: row.status || 1,
    guideType: row.guideType || 'TEXT',
    guideDuration: row.guideDuration || null
  })
  loadGuideSteps(row.guideSteps)
  loadDevices()
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(form, defaultForm())
  guideStepsList.value = []
}

const handleSave = async () => {
  await formRef.value.validate()
  saveLoading.value = true
  try {
    const payload = {
      category: form.category,
      deviceId: form.deviceId,
      question: form.question,
      answer: form.answer,
      keywords: form.keywords,
      isDanger: form.isDanger,
      status: form.status,
      guideType: form.guideType,
      guideSteps: getGuideStepsJson(),
      guideDuration: form.guideDuration
    }
    if (isEdit.value) {
      await updateKnowledge(editingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await addKnowledge(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    saveLoading.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除该知识条目吗？', '确认删除', { type: 'warning' })
  try {
    await deleteKnowledge(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

const formatTime = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString()
}

const getDeviceName = (id) => {
  if (!id) return '-'
  const device = allDevices.value.find(d => d.id === id)
  return device ? device.deviceName : '-'
}

onMounted(() => loadData())
</script>

<style lang="scss" scoped>
.knowledge-manage { color: #E6EDF3; width: 100%; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; h2 { margin: 0; color: #E6EDF3; font-size: 24px; } }
.header-actions { display: flex; gap: 12px; }
.filter-bar { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; flex-wrap: wrap; }
.filter-category { width: 200px; max-width: 100%; flex: 0 0 200px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
.guide-steps { border: 1px dashed #30363d; border-radius: 8px; padding: 16px; background: #0d1117; }
.guide-step-item { background: #161b22; border: 1px solid #21262d; border-radius: 6px; padding: 12px; margin-bottom: 12px; }
.guide-step-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.step-num { color: #00d4ff; font-weight: 600; }
:deep(.el-form-item) { margin-bottom: 12px; }
.avatar-uploader { display: flex; align-items: center; }
.step-image { width: 120px; height: 80px; object-fit: cover; border-radius: 4px; border: 1px solid #30363d; }
.image-actions { margin-top: 8px; }
</style>
