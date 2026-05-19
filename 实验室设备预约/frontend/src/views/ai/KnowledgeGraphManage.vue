<template>
  <div class="kg-manage">
    <div class="page-header" v-if="!embedded">
      <div class="header-left">
        <h2>图谱管理</h2>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openAddNode">新增节点</el-button>
        <el-button @click="openAddEdge">新增关系</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="kg-tabs" type="card">
      <el-tab-pane label="节点管理" name="nodes">
        <div class="table-toolbar">
          <div class="toolbar-left">
            <el-select v-model="nodeFilter.nodeType" placeholder="节点类型" clearable style="width:140px" @change="loadNodes">
              <el-option label="设备" value="DEVICE" />
              <el-option label="知识" value="KNOWLEDGE" />
              <el-option label="分类" value="CATEGORY" />
            </el-select>
            <el-input v-model="nodeFilter.keyword" placeholder="名称/标签/描述" clearable style="width:220px" @keyup.enter="loadNodes" />
            <el-select v-model="nodeFilter.status" placeholder="状态" clearable style="width:120px" @change="loadNodes">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadNodes">搜索</el-button>
            <el-button @click="resetNodeFilter">重置</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="openAddNode">新增节点</el-button>
          </div>
        </div>

        <el-table :data="nodeList" v-loading="nodeLoading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="nodeType" label="类型" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="nodeTypeTag(row.nodeType)">{{ nodeTypeText(row.nodeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="nodeName" label="名称" min-width="140" show-overflow-tooltip />
          <el-table-column prop="nodeLabel" label="标签" min-width="140" show-overflow-tooltip />
          <el-table-column prop="nodeId" label="关联ID" width="110" />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="openEditNode(row)">编辑</el-button>
              <el-button size="small" text @click="toggleNodeStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
              <el-button type="danger" size="small" text @click="deleteNode(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="nodePage.pageNum"
            v-model:page-size="nodePage.pageSize"
            :total="nodePage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadNodes"
            @current-change="loadNodes"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="关系管理" name="edges">
        <div class="table-toolbar">
          <div class="toolbar-left">
            <el-select v-model="edgeFilter.edgeType" placeholder="关系类型" clearable style="width:140px" @change="loadEdges">
              <el-option label="属于" value="BELONGS_TO" />
              <el-option label="关联" value="RELATED_TO" />
              <el-option label="包含" value="CONTAINS" />
              <el-option label="依赖" value="DEPENDS_ON" />
            </el-select>
            <el-input v-model="edgeFilter.keyword" placeholder="标签/描述" clearable style="width:220px" @keyup.enter="loadEdges" />
            <el-select v-model="edgeFilter.status" placeholder="状态" clearable style="width:120px" @change="loadEdges">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
            <el-button type="primary" @click="loadEdges">搜索</el-button>
            <el-button @click="resetEdgeFilter">重置</el-button>
          </div>
          <div class="toolbar-right">
            <el-button type="primary" @click="openAddEdge">新增关系</el-button>
          </div>
        </div>

        <el-table :data="edgeList" v-loading="edgeLoading" stripe>
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="edgeType" label="类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="edgeTypeTag(row.edgeType)">{{ edgeTypeText(row.edgeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sourceNodeId" label="源节点" width="110" />
          <el-table-column prop="targetNodeId" label="目标节点" width="110" />
          <el-table-column prop="edgeLabel" label="标签" min-width="140" show-overflow-tooltip />
          <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
          <el-table-column prop="weight" label="权重" width="90" />
          <el-table-column prop="status" label="状态" width="80" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" size="small" text @click="openEditEdge(row)">编辑</el-button>
              <el-button size="small" text @click="toggleEdgeStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
              <el-button type="danger" size="small" text @click="deleteEdge(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination">
          <el-pagination
            v-model:current-page="edgePage.pageNum"
            v-model:page-size="edgePage.pageSize"
            :total="edgePage.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="loadEdges"
            @current-change="loadEdges"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 节点弹窗 -->
    <el-dialog v-model="nodeDialogVisible" :title="nodeDialogTitle" width="760px" @close="resetNodeForm">
      <el-form ref="nodeFormRef" :model="nodeForm" :rules="nodeRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="节点类型" prop="nodeType">
              <el-select v-model="nodeForm.nodeType" placeholder="选择类型" style="width:100%" @change="onNodeTypeChange">
                <el-option label="设备" value="DEVICE" />
                <el-option label="知识" value="KNOWLEDGE" />
                <el-option label="分类" value="CATEGORY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关联实体" prop="nodeId" v-if="nodeForm.nodeType === 'DEVICE' || nodeForm.nodeType === 'KNOWLEDGE'">
              <el-select v-model="nodeForm.nodeId" filterable placeholder="请选择关联实体" style="width:100%" clearable @change="fillNodeNameBySelection">
                <el-option
                  v-for="o in relatedEntityOptions"
                  :key="o.id"
                  :label="o.label"
                  :value="o.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="分类名称" v-else>
              <el-select v-model="nodeForm.nodeName" filterable allow-create default-first-option placeholder="请选择或输入分类" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="节点名称" prop="nodeName" v-if="nodeForm.nodeType !== 'CATEGORY'">
          <el-input v-model="nodeForm.nodeName" disabled />
        </el-form-item>
        <el-form-item label="节点标签">
          <el-input v-model="nodeForm.nodeLabel" placeholder="如：离心机" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="nodeForm.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="大小">
              <el-input-number v-model="nodeForm.size" :min="10" :max="120" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="颜色">
              <el-color-picker v-model="nodeForm.color" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="nodeForm.sort" :min="0" :max="999" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态">
          <el-radio-group v-model="nodeForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="nodeSaving" @click="saveNode">保存</el-button>
      </template>
    </el-dialog>

    <!-- 关系弹窗 -->
    <el-dialog v-model="edgeDialogVisible" :title="edgeDialogTitle" width="760px" @close="resetEdgeForm">
      <el-form ref="edgeFormRef" :model="edgeForm" :rules="edgeRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="源节点" prop="sourceNodeId">
              <el-select v-model="edgeForm.sourceNodeId" placeholder="选择源节点" filterable style="width:100%">
                <el-option v-for="n in nodeAllOptions" :key="n.id" :label="nodeOptionLabel(n)" :value="n.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标节点" prop="targetNodeId">
              <el-select v-model="edgeForm.targetNodeId" placeholder="选择目标节点" filterable style="width:100%">
                <el-option v-for="n in nodeAllOptions" :key="n.id" :label="nodeOptionLabel(n)" :value="n.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="关系类型" prop="edgeType">
          <el-select v-model="edgeForm.edgeType" placeholder="选择类型" style="width:100%">
            <el-option label="属于" value="BELONGS_TO" />
            <el-option label="关联" value="RELATED_TO" />
            <el-option label="包含" value="CONTAINS" />
            <el-option label="依赖" value="DEPENDS_ON" />
          </el-select>
        </el-form-item>
        <el-form-item label="关系标签">
          <el-input v-model="edgeForm.edgeLabel" placeholder="如：关联知识" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="edgeForm.description" type="textarea" :rows="3" placeholder="可选" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="权重">
              <el-input-number v-model="edgeForm.weight" :min="0" :max="999" :step="0.1" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序">
              <el-input-number v-model="edgeForm.sort" :min="0" :max="999" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态">
              <el-radio-group v-model="edgeForm.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="edgeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="edgeSaving" @click="saveEdge">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  getGraphManageOptions,
  getGraphNodeList, addGraphNode, updateGraphNode, updateGraphNodeStatus, deleteGraphNode,
  getGraphEdgeList, addGraphEdge, updateGraphEdge, updateGraphEdgeStatus, deleteGraphEdge
} from '@/api/ai'
import { getAllDevices } from '@/api/device'
import { getKnowledgeList } from '@/api/knowledge'
import { getAllCategories } from '@/api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  embedded: {
    type: Boolean,
    default: false
  }
})

const activeTab = ref('nodes')
const optionLoading = ref(false)
const deviceOptions = ref([])
const knowledgeOptions = ref([])
const categoryOptions = ref([])
const nodeAllOptions = ref([])

const nodeList = ref([])
const nodeLoading = ref(false)
const nodeSaving = ref(false)
const nodeDialogVisible = ref(false)
const nodeDialogTitle = ref('新增节点')
const nodeFormRef = ref()
const nodeEditingId = ref(null)

const nodePage = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const nodeFilter = reactive({ nodeType: '', keyword: '', status: null })

const nodeForm = reactive({
  nodeType: '',
  nodeId: null,
  nodeName: '',
  nodeLabel: '',
  description: '',
  extraData: '',
  size: 40,
  color: '',
  sort: 0,
  status: 1
})

const nodeRules = {
  nodeType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
  nodeName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  nodeId: [{
    validator: (_, value, callback) => {
      if (nodeForm.nodeType === 'DEVICE' || nodeForm.nodeType === 'KNOWLEDGE') {
        if (!value) return callback(new Error('请选择关联实体'))
      }
      callback()
    },
    trigger: 'change'
  }]
}

const relatedEntityOptions = computed(() => {
  if (nodeForm.nodeType === 'DEVICE') {
    return deviceOptions.value.map(d => ({ id: d.id, label: `${d.name || ''} (${d.code || ''}) #${d.id}` }))
  }
  if (nodeForm.nodeType === 'KNOWLEDGE') {
    return knowledgeOptions.value.map(k => ({ id: k.id, label: `${k.name || ''} #${k.id}` }))
  }
  return []
})

const loadManageOptions = async () => {
  optionLoading.value = true
  try {
    // 1) 优先读取图谱后端汇总
    const res = await getGraphManageOptions()

    // 设备：兼容 {name,code} 与原实体字段
    const graphDevices = Array.isArray(res.devices) ? res.devices : []
    deviceOptions.value = graphDevices.map(d => ({
      id: d.id,
      name: d.name || d.deviceName || '',
      code: d.code || d.deviceNo || ''
    }))

    // 知识：兼容 {name} 与原字段 question
    const graphKnowledge = Array.isArray(res.knowledge) ? res.knowledge : []
    knowledgeOptions.value = graphKnowledge.map(k => ({
      id: k.id,
      name: k.name || k.question || k.title || '',
      category: k.category || '',
      deviceId: k.deviceId || null
    }))

    // 分类：可能是字符串，也可能是对象
    const graphCategories = Array.isArray(res.categories) ? res.categories : []
    categoryOptions.value = graphCategories
      .map(c => {
        if (typeof c === 'string') return c
        if (c && typeof c === 'object') return c.categoryName || c.name || c.label || ''
        return ''
      })
      .filter(Boolean)

    nodeAllOptions.value = Array.isArray(res.nodes) ? res.nodes : []

    // 2) 回填兜底：避免某个接口返回空导致下拉“无数据”
    if (deviceOptions.value.length === 0) {
      const allDevices = await getAllDevices()
      deviceOptions.value = (allDevices || []).map(d => ({
        id: d.id,
        name: d.deviceName || '',
        code: d.deviceNo || ''
      }))
    }

    if (knowledgeOptions.value.length === 0) {
      const kb = await getKnowledgeList({ pageNum: 1, pageSize: 1000, status: 1 })
      const rows = Array.isArray(kb?.list) ? kb.list : []
      knowledgeOptions.value = rows.map(k => ({
        id: k.id,
        name: k.question || '',
        category: k.category || '',
        deviceId: k.deviceId || null
      }))
    }

    if (categoryOptions.value.length === 0) {
      const categories = await getAllCategories()
      categoryOptions.value = (categories || [])
        .map(c => (typeof c === 'string' ? c : (c?.categoryName || c?.name || '')))
        .filter(Boolean)
    }

    // 去重
    categoryOptions.value = Array.from(new Set(categoryOptions.value))
  } catch (e) {
    console.error(e)
    // 全量兜底
    try {
      const [allDevices, kb, categories] = await Promise.all([
        getAllDevices(),
        getKnowledgeList({ pageNum: 1, pageSize: 1000, status: 1 }),
        getAllCategories()
      ])
      deviceOptions.value = (allDevices || []).map(d => ({
        id: d.id,
        name: d.deviceName || '',
        code: d.deviceNo || ''
      }))
      knowledgeOptions.value = (kb?.list || []).map(k => ({
        id: k.id,
        name: k.question || '',
        category: k.category || '',
        deviceId: k.deviceId || null
      }))
      categoryOptions.value = (categories || [])
        .map(c => (typeof c === 'string' ? c : (c?.categoryName || c?.name || '')))
        .filter(Boolean)
      nodeAllOptions.value = []
    } catch {
      deviceOptions.value = []
      knowledgeOptions.value = []
      categoryOptions.value = []
      nodeAllOptions.value = []
    }
  } finally {
    optionLoading.value = false
  }
}

const onNodeTypeChange = () => {
  nodeForm.nodeId = null
  if (nodeForm.nodeType === 'CATEGORY') {
    nodeForm.nodeName = ''
  } else {
    nodeForm.nodeName = ''
  }
}

const fillNodeNameBySelection = () => {
  if (nodeForm.nodeType === 'DEVICE') {
    const found = deviceOptions.value.find(d => d.id === nodeForm.nodeId)
    nodeForm.nodeName = found?.name || ''
    if (!nodeForm.nodeLabel) {
      nodeForm.nodeLabel = found?.name || ''
    }
  } else if (nodeForm.nodeType === 'KNOWLEDGE') {
    const found = knowledgeOptions.value.find(k => k.id === nodeForm.nodeId)
    nodeForm.nodeName = found?.name || ''
    if (!nodeForm.nodeLabel) {
      nodeForm.nodeLabel = found?.name || ''
    }
  }
}

const loadNodes = async () => {
  nodeLoading.value = true
  try {
    const res = await getGraphNodeList({
      pageNum: nodePage.pageNum,
      pageSize: nodePage.pageSize,
      ...nodeFilter
    })
    nodeList.value = res.list || []
    nodePage.total = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    nodeLoading.value = false
  }
}

const resetNodeFilter = () => {
  nodeFilter.nodeType = ''
  nodeFilter.keyword = ''
  nodeFilter.status = null
  nodePage.pageNum = 1
  loadNodes()
}

const openAddNode = () => {
  nodeDialogTitle.value = '新增节点'
  nodeEditingId.value = null
  Object.assign(nodeForm, {
    nodeType: '', nodeId: null, nodeName: '', nodeLabel: '', description: '', extraData: '',
    size: 40, color: '', sort: 0, status: 1
  })
  nodeDialogVisible.value = true
}

const openEditNode = (row) => {
  nodeDialogTitle.value = '编辑节点'
  nodeEditingId.value = row.id
  Object.assign(nodeForm, {
    nodeType: row.nodeType || '',
    nodeId: row.nodeId ?? null,
    nodeName: row.nodeName || '',
    nodeLabel: row.nodeLabel || '',
    description: row.description || '',
    extraData: row.extraData || '',
    size: row.size ?? 40,
    color: row.color || '',
    sort: row.sort ?? 0,
    status: row.status ?? 1
  })
  nodeDialogVisible.value = true
}

const resetNodeForm = () => {
  nodeFormRef.value?.resetFields()
}

const saveNode = async () => {
  fillNodeNameBySelection()
  await nodeFormRef.value.validate()
  nodeSaving.value = true
  try {
    const payload = {
      nodeType: nodeForm.nodeType,
      nodeId: nodeForm.nodeType === 'CATEGORY' ? null : (nodeForm.nodeId ? Number(nodeForm.nodeId) : null),
      nodeName: nodeForm.nodeName,
      nodeLabel: nodeForm.nodeLabel,
      description: nodeForm.description,
      extraData: nodeForm.extraData,
      size: nodeForm.size,
      color: nodeForm.color,
      sort: nodeForm.sort,
      status: nodeForm.status
    }
    if (nodeEditingId.value) {
      await updateGraphNode(nodeEditingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await addGraphNode(payload)
      ElMessage.success('添加成功')
    }
    nodeDialogVisible.value = false
    await Promise.all([loadNodes(), loadManageOptions()])
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    nodeSaving.value = false
  }
}

const toggleNodeStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await updateGraphNodeStatus(row.id, next)
  loadNodes()
}

const deleteNode = async (row) => {
  await ElMessageBox.confirm('确定删除该节点吗？关联边将一并删除。', '确认删除', { type: 'warning' })
  await deleteGraphNode(row.id)
  ElMessage.success('删除成功')
  await Promise.all([loadNodes(), loadManageOptions()])
}

const nodeTypeText = (t) => ({ DEVICE: '设备', KNOWLEDGE: '知识', CATEGORY: '分类' }[t] || t)
const nodeTypeTag = (t) => ({ DEVICE: 'success', KNOWLEDGE: 'warning', CATEGORY: 'info' }[t] || 'info')

const edgeList = ref([])
const edgeLoading = ref(false)
const edgeSaving = ref(false)
const edgeDialogVisible = ref(false)
const edgeDialogTitle = ref('新增关系')
const edgeFormRef = ref()
const edgeEditingId = ref(null)

const edgePage = reactive({ pageNum: 1, pageSize: 10, total: 0 })
const edgeFilter = reactive({ edgeType: '', keyword: '', status: null })

const edgeForm = reactive({
  sourceNodeId: null,
  targetNodeId: null,
  edgeType: '',
  edgeLabel: '',
  description: '',
  weight: 1,
  sort: 0,
  status: 1
})

const edgeRules = {
  sourceNodeId: [{ required: true, message: '请选择源节点', trigger: 'change' }],
  targetNodeId: [{ required: true, message: '请选择目标节点', trigger: 'change' }],
  edgeType: [{ required: true, message: '请选择关系类型', trigger: 'change' }]
}

const loadEdges = async () => {
  edgeLoading.value = true
  try {
    const res = await getGraphEdgeList({
      pageNum: edgePage.pageNum,
      pageSize: edgePage.pageSize,
      ...edgeFilter
    })
    edgeList.value = res.list || []
    edgePage.total = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    edgeLoading.value = false
  }
}

const resetEdgeFilter = () => {
  edgeFilter.edgeType = ''
  edgeFilter.keyword = ''
  edgeFilter.status = null
  edgePage.pageNum = 1
  loadEdges()
}

const openAddEdge = async () => {
  edgeDialogTitle.value = '新增关系'
  edgeEditingId.value = null
  Object.assign(edgeForm, {
    sourceNodeId: null,
    targetNodeId: null,
    edgeType: '',
    edgeLabel: '',
    description: '',
    weight: 1,
    sort: 0,
    status: 1
  })
  if (nodeAllOptions.value.length === 0) {
    await loadManageOptions()
  }
  edgeDialogVisible.value = true
}

const openEditEdge = async (row) => {
  edgeDialogTitle.value = '编辑关系'
  edgeEditingId.value = row.id
  Object.assign(edgeForm, {
    sourceNodeId: row.sourceNodeId,
    targetNodeId: row.targetNodeId,
    edgeType: row.edgeType || '',
    edgeLabel: row.edgeLabel || '',
    description: row.description || '',
    weight: row.weight ?? 1,
    sort: row.sort ?? 0,
    status: row.status ?? 1
  })
  if (nodeAllOptions.value.length === 0) {
    await loadManageOptions()
  }
  edgeDialogVisible.value = true
}

const resetEdgeForm = () => {
  edgeFormRef.value?.resetFields()
}

const saveEdge = async () => {
  await edgeFormRef.value.validate()
  edgeSaving.value = true
  try {
    const payload = {
      sourceNodeId: edgeForm.sourceNodeId,
      targetNodeId: edgeForm.targetNodeId,
      edgeType: edgeForm.edgeType,
      edgeLabel: edgeForm.edgeLabel,
      description: edgeForm.description,
      weight: edgeForm.weight,
      sort: edgeForm.sort,
      status: edgeForm.status
    }
    if (edgeEditingId.value) {
      await updateGraphEdge(edgeEditingId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await addGraphEdge(payload)
      ElMessage.success('添加成功')
    }
    edgeDialogVisible.value = false
    loadEdges()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    edgeSaving.value = false
  }
}

const toggleEdgeStatus = async (row) => {
  const next = row.status === 1 ? 0 : 1
  await updateGraphEdgeStatus(row.id, next)
  loadEdges()
}

const deleteEdge = async (row) => {
  await ElMessageBox.confirm('确定删除该关系吗？', '确认删除', { type: 'warning' })
  await deleteGraphEdge(row.id)
  ElMessage.success('删除成功')
  loadEdges()
}

const nodeOptionLabel = (n) => {
  const title = n.nodeName || n.nodeLabel || '-'
  const type = nodeTypeText(n.nodeType)
  const rel = n.nodeId ? ` 关联:${n.nodeId}` : ''
  return `${title} [${type}] #${n.id}${rel}`
}
const edgeTypeText = (t) => ({ BELONGS_TO: '属于', RELATED_TO: '关联', CONTAINS: '包含', DEPENDS_ON: '依赖' }[t] || t)
const edgeTypeTag = (t) => ({ BELONGS_TO: 'info', RELATED_TO: 'warning', CONTAINS: 'success', DEPENDS_ON: 'danger' }[t] || 'info')

onMounted(async () => {
  await Promise.all([loadManageOptions(), loadNodes(), loadEdges()])
})
</script>

<style scoped lang="scss">
.kg-manage { color: #E6EDF3; width: 100%; }

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  padding: 14px 16px;
  background: #111827;
  border: 1px solid #253041;
  border-radius: 12px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-desc {
  font-size: 12px;
  color: #8B949E;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px;
  background: #0f1722;
  border: 1px solid #253041;
  border-radius: 10px;
  flex-wrap: wrap;
}

.toolbar-left {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.kg-tabs :deep(.el-tabs__header) {
  margin-bottom: 12px;
}

.kg-tabs :deep(.el-tabs__item) {
  color: #9BA7B4;
}

.kg-tabs :deep(.el-tabs__item.is-active) {
  color: #00D4FF;
}

.kg-tabs :deep(.el-button--primary) {
  background: linear-gradient(90deg, #0ea5e9, #2563eb);
  border: none;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
