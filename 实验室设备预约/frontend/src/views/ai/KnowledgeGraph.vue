<template>
  <div class="knowledge-graph-page">
    <!-- 顶部标题栏 -->
    <div class="graph-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon><Connection /></el-icon>
          知识图谱
        </h2>
        <span class="page-desc">设备 — 知识 — 分类 关系可视化（测试方法/使用方法见图谱管理页）</span>
      </div>
      <div class="header-right">
        <el-button circle @click="refreshGraph" :icon="Refresh" title="刷新图谱" />
        <el-button @click="resetView" :icon="Rank">重置视图</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-bar" v-if="stats">
      <div class="stat-card stat-device">
        <div class="stat-icon"><el-icon><Monitor /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.deviceCount }}</span>
          <span class="stat-label">设备节点</span>
        </div>
      </div>
      <div class="stat-card stat-knowledge">
        <div class="stat-icon"><el-icon><Document /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.knowledgeCount }}</span>
          <span class="stat-label">知识节点</span>
        </div>
      </div>
      <div class="stat-card stat-category">
        <div class="stat-icon"><el-icon><FolderOpened /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.categoryCount }}</span>
          <span class="stat-label">分类节点</span>
        </div>
      </div>
      <div class="stat-card stat-edge">
        <div class="stat-icon"><el-icon><Connection /></el-icon></div>
        <div class="stat-info">
          <span class="stat-value">{{ stats.totalEdges }}</span>
          <span class="stat-label">关系连线</span>
        </div>
      </div>
    </div>

    <!-- 图例 -->
    <div class="graph-legend">
      <div class="legend-item">
        <span class="legend-dot" style="background:#409EFF; box-shadow:0 0 6px rgba(64,158,255,0.5)"></span>
        <span>设备</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot" style="background:#67C23A; box-shadow:0 0 6px rgba(103,194,58,0.5)"></span>
        <span>知识</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot" style="background:#7B61FF; box-shadow:0 0 6px rgba(123,97,255,0.5)"></span>
        <span>分类</span>
      </div>
      <div class="legend-divider"></div>
      <div class="legend-item">
        <span class="legend-line" style="background:#7B61FF"></span>
        <span>属于</span>
      </div>
      <div class="legend-item">
        <span class="legend-line" style="background:#E6A23C"></span>
        <span>关联</span>
      </div>
    </div>

    <!-- 图谱画布 -->
    <div class="graph-canvas-wrapper" ref="canvasWrapper" v-loading="loading">
      <svg
        ref="svgRef"
        class="graph-svg"
        :width="svgWidth"
        :height="svgHeight"
        @mousedown="onSvgMouseDown"
        @wheel.prevent="onSvgWheel"
      >
        <!-- 箭头标记（保持在根 defs，供变换组内引用） -->
        <defs>
          <marker id="arrow-purple" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
            <path d="M0,0 L0,6 L9,3 z" fill="#7B61FF" />
          </marker>
          <marker id="arrow-orange" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
            <path d="M0,0 L0,6 L9,3 z" fill="#E6A23C" />
          </marker>
          <marker id="arrow-blue" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
            <path d="M0,0 L0,6 L9,3 z" fill="#409EFF" />
          </marker>
          <marker id="arrow-red" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto">
            <path d="M0,0 L0,6 L9,3 z" fill="#F56C6C" />
          </marker>
        </defs>

        <!-- 平移+缩放：必须作用在整幅图内容上，否则滚轮/拖动画布无视觉效果 -->
        <g class="graph-viewport" :transform="viewportTransform">
        <!-- 边 -->
        <g class="edges-layer">
          <g v-for="edge in renderEdges" :key="'edge-' + edge.id">
            <line
              :x1="edge.x1" :y1="edge.y1"
              :x2="edge.x2" :y2="edge.y2"
              :stroke="edge.style.strokeColor"
              :stroke-width="edge.style.strokeWidth"
              :marker-end="'url(#arrow-' + edge.markerKey + ')'"
              class="graph-edge"
              :class="{ highlighted: isEdgeHighlighted(edge) }"
            />
            <!-- 边标签 -->
            <text
              v-if="showEdgeLabels"
              :x="(edge.x1 + edge.x2) / 2"
              :y="(edge.y1 + edge.y2) / 2 - 6"
              class="edge-label"
              :fill="edge.style.strokeColor"
              text-anchor="middle"
            >{{ edge.label }}</text>
          </g>
        </g>

        <!-- 节点 -->
        <g class="nodes-layer">
          <g
            v-for="node in renderNodes"
            :key="'node-' + node.id"
            :transform="`translate(${node.x}, ${node.y})`"
            class="graph-node"
            :class="{ selected: selectedNodeId === node.id, dragging: draggingNodeId === node.id }"
            @click.stop="onNodeClick(node)"
            @mousedown.stop="onNodeMouseDown($event, node)"
          >
            <!-- 节点光晕 -->
            <circle
              v-if="selectedNodeId === node.id"
              :r="node.size + 8"
              :fill="node.color"
              fill-opacity="0.15"
              class="node-glow"
            />

            <!-- 节点圆形 -->
            <circle
              :r="node.size"
              :fill="node.color"
              fill-opacity="0.2"
              :stroke="node.color"
              :stroke-width="selectedNodeId === node.id ? 3 : 2"
            />

            <!-- 节点图标/文字 -->
            <text
              text-anchor="middle"
              dominant-baseline="central"
              :fill="node.color"
              font-size="12"
              font-weight="bold"
              class="node-symbol"
            >{{ getNodeSymbol(node.nodeType) }}</text>

            <!-- 节点标签 -->
            <text
              text-anchor="middle"
              :y="node.size + 16"
              fill="#E6EDF3"
              font-size="12"
              class="node-label"
            >{{ node.label }}</text>
          </g>
        </g>

        <!-- 拖拽时实时连线 -->
        <line
          v-if="isDragging && tempLine"
          :x1="tempLine.x1" :y1="tempLine.y1"
          :x2="tempLine.x2" :y2="tempLine.y2"
          stroke="#00D4FF"
          stroke-width="1.5"
          stroke-dasharray="5,5"
          opacity="0.7"
        />
        </g>
      </svg>

      <!-- 节点详情弹窗 -->
      <Transition name="detail-fade">
        <div v-if="selectedNodeInfo" class="node-detail-panel">
          <div class="detail-header">
            <span class="detail-type-badge" :style="{ background: selectedNodeInfo.color + '22', color: selectedNodeInfo.color, borderColor: selectedNodeInfo.color + '55' }">
              {{ getNodeTypeName(selectedNodeInfo.nodeType) }}
            </span>
            <el-button class="detail-close" text @click="selectedNodeId = null; selectedNodeInfo = null">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <div class="detail-body">
            <div class="detail-name">{{ selectedNodeInfo.name }}</div>
            <div class="detail-desc" v-if="selectedNodeInfo.description">{{ selectedNodeInfo.description }}</div>
            <div class="detail-meta">
              <span>ID: {{ selectedNodeInfo.id }}</span>
              <span v-if="selectedNodeInfo.nodeId">关联ID: {{ selectedNodeInfo.nodeId }}</span>
            </div>

            <!-- 关联节点列表 -->
            <div class="neighbor-section" v-if="neighborNodes.length > 0">
              <div class="neighbor-title">关联节点 ({{ neighborNodes.length }})</div>
              <div class="neighbor-list">
                <div
                  v-for="n in neighborNodes"
                  :key="n.id"
                  class="neighbor-item"
                  @click.stop="selectNode(n.id)"
                >
                  <span class="neighbor-dot" :style="{ background: n.color }"></span>
                  <span class="neighbor-name">{{ n.label || n.name }}</span>
                  <span class="neighbor-type">{{ getNodeTypeName(n.nodeType) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </Transition>
    </div>

    <!-- 控制栏 -->
    <div class="graph-controls">
      <el-button-group>
        <el-button @click="zoomIn" title="放大">
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button @click="zoomOut" title="缩小">
          <el-icon><Minus /></el-icon>
        </el-button>
      </el-button-group>
      <el-checkbox v-model="showEdgeLabels" class="edge-label-toggle">显示连线标签</el-checkbox>
      <div class="zoom-info">{{ Math.round(zoomRatio * 100) }}%</div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { getGraphData, getGraphStats, getNodeNeighbors } from '@/api/ai'
import { ElMessage } from 'element-plus'
import {
  Connection, Monitor, Document, FolderOpened,
  Refresh, Rank, Plus, Minus, Close
} from '@element-plus/icons-vue'

// ===== 状态 =====
const loading = ref(true)
const graphData = ref({ nodes: [], edges: [] })
const stats = ref(null)
const selectedNodeId = ref(null)
const selectedNodeInfo = ref(null)
const neighborNodes = ref([])

const svgRef = ref(null)
const canvasWrapper = ref(null)
const svgWidth = ref(800)
const svgHeight = ref(600)

// 力导向布局状态
const renderNodes = ref([])
const renderEdges = ref([])
const zoomRatio = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)

// 拖拽状态
const isDragging = ref(false)
const draggingNodeId = ref(null)
const dragStartX = ref(0)
const dragStartY = ref(0)
const nodeStartX = ref(0)
const nodeStartY = ref(0)
const tempLine = ref(null)

// 画布拖拽（平移）状态
const isPanning = ref(false)
const panStartX = ref(0)
const panStartY = ref(0)
const panOffsetStartX = ref(0)
const panOffsetStartY = ref(0)

// 连线标签显示
const showEdgeLabels = ref(false)

// ===== 计算属性 =====
const draggingNode = computed(() =>
  renderNodes.value.find(n => n.id === draggingNodeId.value)
)

/** 画布平移 + 等比缩放（作用于整组边与节点） */
const viewportTransform = computed(
  () => `translate(${offsetX.value}, ${offsetY.value}) scale(${zoomRatio.value})`
)

// ===== 工具方法 =====
const getNodeSymbol = (type) => {
  const map = { DEVICE: '📦', KNOWLEDGE: '📖', CATEGORY: '📁' }
  return map[type] || '●'
}

const getNodeTypeName = (type) => {
  const map = { DEVICE: '设备', KNOWLEDGE: '知识', CATEGORY: '分类' }
  return map[type] || type
}

const getEdgeMarkerKey = (type) => {
  const map = { BELONGS_TO: 'purple', RELATED_TO: 'orange', CONTAINS: 'blue', DEPENDS_ON: 'red' }
  return map[type] || 'purple'
}

const isEdgeHighlighted = (edge) => {
  if (!selectedNodeId.value) return false
  return edge.source === selectedNodeId.value || edge.target === selectedNodeId.value
}

// ===== 力导向布局引擎（简化版）=====
// 布局区域跟随画布实际大小动态计算
const REPULSION = 4000
const ATTRACTION = 0.015
const DAMPING = 0.85
const MIN_MOVE = 0.1

// 动态布局区域（会在 initLayout/updateCanvasSize 时更新）
const layoutArea = reactive({ x: 900, y: 550, w: 900, h: 550 })
const LAYOUT_MARGIN = 60

function initLayout() {
  // 初始化节点位置（居中分布，随机微调）
  // 说明：后端可能把未设置坐标的节点返回为 (0,0)，会导致节点重叠且无法展开
  // 这里将 (0,0) 视为“未布局”，并增加轻微抖动避免完全重合
  renderNodes.value = graphData.value.nodes.map((n, idx) => {
    const cx = layoutArea.x + layoutArea.w / 2
    const cy = layoutArea.y + layoutArea.h / 2

    const hasPosition = Number.isFinite(n.position?.x) && Number.isFinite(n.position?.y)
    const isDefaultZero = Number(n.position?.x) === 0 && Number(n.position?.y) === 0

    const baseX = hasPosition && !isDefaultZero
      ? Number(n.position.x)
      : (cx + (Math.random() - 0.5) * layoutArea.w * 0.8)
    const baseY = hasPosition && !isDefaultZero
      ? Number(n.position.y)
      : (cy + (Math.random() - 0.5) * layoutArea.h * 0.8)

    // 轻微抖动：避免多个节点在同一坐标完全重叠导致力导向无法分离
    const jitter = (idx % 7) * 2
    const initX = baseX + (Math.random() - 0.5) * 6 + jitter
    const initY = baseY + (Math.random() - 0.5) * 6 - jitter

    return {
      ...n,
      x: initX,
      y: initY,
      vx: 0,
      vy: 0,
    }
  })

  // 边引用节点坐标
  renderEdges.value = graphData.value.edges.map(e => ({
    ...e,
    x1: 0, y1: 0, x2: 0, y2: 0,
  }))
}

function runLayoutStep() {
  if (renderNodes.value.length === 0) return

  // 斥力：所有节点互相排斥
  for (let i = 0; i < renderNodes.value.length; i++) {
    for (let j = i + 1; j < renderNodes.value.length; j++) {
      const a = renderNodes.value[i]
      const b = renderNodes.value[j]
      const dx = b.x - a.x
      const dy = b.y - a.y
      const distSq = dx * dx + dy * dy + 0.01
      const dist = Math.sqrt(distSq)
      const force = REPULSION / distSq

      const fx = (dx / dist) * force
      const fy = (dy / dist) * force

      a.vx -= fx
      a.vy -= fy
      b.vx += fx
      b.vy += fy
    }
  }

  // 引力：相连的节点互相吸引
  for (const edge of renderEdges.value) {
    const src = renderNodes.value.find(n => n.id === edge.source)
    const tgt = renderNodes.value.find(n => n.id === edge.target)
    if (!src || !tgt) continue

    const dx = tgt.x - src.x
    const dy = tgt.y - src.y
    const dist = Math.sqrt(dx * dx + dy * dy) + 0.01
    const force = dist * ATTRACTION

    const fx = (dx / dist) * force
    const fy = (dy / dist) * force

    src.vx += fx
    src.vy += fy
    tgt.vx -= fx
    tgt.vy -= fy
  }

  // 中心引力：把节点拉向画布中心，防止飘散
  const cx = layoutArea.x + layoutArea.w / 2
  const cy = layoutArea.y + layoutArea.h / 2
  for (const node of renderNodes.value) {
    const dx = cx - node.x
    const dy = cy - node.y
    node.vx += dx * 0.005
    node.vy += dy * 0.005
  }

  // 更新位置 + 阻尼
  let totalMovement = 0
  for (const node of renderNodes.value) {
    node.vx *= DAMPING
    node.vy *= DAMPING
    node.x += node.vx
    node.y += node.vy

    // 边界约束
    const margin = node.size + 20
    node.x = Math.max(layoutArea.x + margin, Math.min(layoutArea.x + layoutArea.w - margin, node.x))
    node.y = Math.max(layoutArea.y + margin, Math.min(layoutArea.y + layoutArea.h - margin, node.y))

    totalMovement += Math.abs(node.vx) + Math.abs(node.vy)
  }

  // 更新边的端点
  for (const edge of renderEdges.value) {
    const src = renderNodes.value.find(n => n.id === edge.source)
    const tgt = renderNodes.value.find(n => n.id === edge.target)
    if (src && tgt) {
      edge.x1 = src.x
      edge.y1 = src.y
      edge.x2 = tgt.x
      edge.y2 = tgt.y
    }
  }

  return totalMovement
}

let layoutTimer = null
function startLayout() {
  if (layoutTimer) cancelAnimationFrame(layoutTimer)
  let steps = 0
  const MAX_STEPS = 300

  function tick() {
    const movement = runLayoutStep()
    steps++
    if (movement > MIN_MOVE && steps < MAX_STEPS) {
      layoutTimer = requestAnimationFrame(tick)
    }
  }
  tick()
}

// ===== 数据加载 =====
async function loadGraph() {
  loading.value = true
  try {
    const [data, statsData] = await Promise.all([
      getGraphData(),
      getGraphStats(),
    ])
    graphData.value = data
    stats.value = statsData
    initLayout()
    startLayout()
  } catch (e) {
    ElMessage.error('加载图谱失败: ' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

async function refreshGraph() {
  await loadGraph()
  selectedNodeId.value = null
  selectedNodeInfo.value = null
  neighborNodes.value = []
}

/** 后端邻居接口返回实体字段 nodeName/nodeLabel，与图谱 data 接口的 name/label 对齐 */
function mapNeighborForUi(raw) {
  const n = raw || {}
  const id = n.id
  const rendered = renderNodes.value.find(rn => rn.id === id || String(rn.id) === String(id))
  const label = n.label ?? n.nodeLabel ?? n.nodeName ?? ''
  const name = n.name ?? n.nodeName ?? label
  return {
    ...n,
    id,
    label,
    name,
    nodeType: n.nodeType,
    color: rendered?.color || getNodeColor(n.nodeType),
  }
}

async function selectNode(nodeId) {
  if (nodeId == null) {
    selectedNodeId.value = null
    selectedNodeInfo.value = null
    neighborNodes.value = []
    return
  }
  const nid = typeof nodeId === 'string' && /^\d+$/.test(nodeId) ? Number(nodeId) : nodeId
  selectedNodeId.value = nid
  selectedNodeInfo.value =
    renderNodes.value.find(n => n.id === nid || String(n.id) === String(nid)) || null

  if (nid != null) {
    try {
      const data = await getNodeNeighbors(nid)
      const neighbors = data.neighbors || []
      neighborNodes.value = neighbors.map(mapNeighborForUi)
    } catch {
      neighborNodes.value = []
    }
  } else {
    neighborNodes.value = []
  }
}

function getNodeColor(type) {
  const map = { DEVICE: '#409EFF', KNOWLEDGE: '#67C23A', CATEGORY: '#7B61FF' }
  return map[type] || '#909399'
}

// ===== 节点拖拽 =====
function onNodeMouseDown(e, node) {
  isDragging.value = true
  draggingNodeId.value = node.id
  dragStartX.value = e.clientX
  dragStartY.value = e.clientY
  nodeStartX.value = node.x
  nodeStartY.value = node.y
  selectNode(node.id)
  e.preventDefault()
}

function onNodeClick(node) {
  selectNode(node.id)
}

function onSvgMouseDown(e) {
  // 左键在空白处平移画布（节点 mousedown 已 stop，不会进入此处）
  if (e.button !== 0) return
  if (e.target?.closest?.('.graph-node')) return
  isPanning.value = true
  panStartX.value = e.clientX
  panStartY.value = e.clientY
  panOffsetStartX.value = offsetX.value
  panOffsetStartY.value = offsetY.value
}

function onSvgMouseMove(e) {
  if (isDragging.value && draggingNodeId.value) {
    const dx = (e.clientX - dragStartX.value) / zoomRatio.value
    const dy = (e.clientY - dragStartY.value) / zoomRatio.value
    const node = renderNodes.value.find(n => n.id === draggingNodeId.value)
    if (node) {
      node.x = nodeStartX.value + dx
      node.y = nodeStartY.value + dy
      // 实时更新边的端点
      for (const edge of renderEdges.value) {
        if (edge.source === node.id) {
          edge.x1 = node.x
          edge.y1 = node.y
        }
        if (edge.target === node.id) {
          edge.x2 = node.x
          edge.y2 = node.y
        }
      }
    }
    // 拖拽时画临时连线
    if (draggingNode) {
      tempLine.value = {
        x1: draggingNode.value.x,
        y1: draggingNode.value.y,
        x2: nodeStartX.value + dx,
        y2: nodeStartY.value + dy,
      }
    }
  } else if (isPanning.value) {
    offsetX.value = panOffsetStartX.value + (e.clientX - panStartX.value)
    offsetY.value = panOffsetStartY.value + (e.clientY - panStartY.value)
  }
}

function onSvgMouseUp() {
  isDragging.value = false
  draggingNodeId.value = null
  isPanning.value = false
  tempLine.value = null
}

function onSvgWheel(e) {
  const scaleFactor = e.deltaY > 0 ? 0.9 : 1.1
  const oldZoom = zoomRatio.value
  const newZoom = Math.max(0.3, Math.min(3, oldZoom * scaleFactor))
  if (Math.abs(newZoom - oldZoom) < 1e-6) return
  const rect = svgRef.value?.getBoundingClientRect()
  if (!rect) {
    zoomRatio.value = newZoom
    return
  }
  const mx = e.clientX - rect.left
  const my = e.clientY - rect.top
  const gx = (mx - offsetX.value) / oldZoom
  const gy = (my - offsetY.value) / oldZoom
  offsetX.value = mx - gx * newZoom
  offsetY.value = my - gy * newZoom
  zoomRatio.value = newZoom
}

function zoomIn() { zoomRatio.value = Math.min(3, zoomRatio.value * 1.2) }
function zoomOut() { zoomRatio.value = Math.max(0.3, zoomRatio.value / 1.2) }

function resetView() {
  zoomRatio.value = 1
  offsetX.value = 0
  offsetY.value = 0
  initLayout()
  startLayout()
}

function updateCanvasSize() {
  if (canvasWrapper.value) {
    svgWidth.value = canvasWrapper.value.clientWidth
    svgHeight.value = canvasWrapper.value.clientHeight
    // 同步更新布局区域（留出边距）
    layoutArea.x = LAYOUT_MARGIN
    layoutArea.y = LAYOUT_MARGIN
    layoutArea.w = svgWidth.value - LAYOUT_MARGIN * 2
    layoutArea.h = svgHeight.value - LAYOUT_MARGIN * 2
  }
}

function onWindowPointerMove(e) {
  if (!isPanning.value && !isDragging.value) return
  onSvgMouseMove(e)
}

function onWindowPointerUp() {
  if (isPanning.value || isDragging.value) onSvgMouseUp()
}

// ===== 生命周期 =====
onMounted(() => {
  updateCanvasSize()
  loadGraph()
  window.addEventListener('resize', updateCanvasSize)
  window.addEventListener('pointermove', onWindowPointerMove)
  window.addEventListener('pointerup', onWindowPointerUp)
  window.addEventListener('pointercancel', onWindowPointerUp)
})

onUnmounted(() => {
  if (layoutTimer) cancelAnimationFrame(layoutTimer)
  window.removeEventListener('resize', updateCanvasSize)
  window.removeEventListener('pointermove', onWindowPointerMove)
  window.removeEventListener('pointerup', onWindowPointerUp)
  window.removeEventListener('pointercancel', onWindowPointerUp)
})
</script>

<style lang="scss" scoped>
.knowledge-graph-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0D1117;
  color: #E6EDF3;
  overflow: hidden;
}

/* ========== 顶部栏 ========== */
.graph-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: #161B22;
  border-bottom: 1px solid #30363D;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #E6EDF3;

  .el-icon {
    color: #00D4FF;
  }
}

.page-desc {
  font-size: 13px;
  color: #6E7681;
}

.header-right {
  display: flex;
  gap: 8px;
}

/* ========== 统计栏 ========== */
.stats-bar {
  display: flex;
  gap: 16px;
  padding: 12px 24px;
  background: #161B22;
  border-bottom: 1px solid #30363D;
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: #21262D;
  border-radius: 8px;
  border: 1px solid #30363D;
  min-width: 145px;
}

.stat-icon {
  .el-icon {
    font-size: 20px;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  font-family: 'JetBrains Mono', monospace;
}

.stat-label {
  font-size: 13px;
  color: #8B949E;
  margin-top: 2px;
}

.stat-device .stat-icon .el-icon { color: #409EFF; }
.stat-device .stat-value { color: #409EFF; }

.stat-knowledge .stat-icon .el-icon { color: #67C23A; }
.stat-knowledge .stat-value { color: #67C23A; }

.stat-category .stat-icon .el-icon { color: #7B61FF; }
.stat-category .stat-value { color: #7B61FF; }

.stat-edge .stat-icon .el-icon { color: #E6A23C; }
.stat-edge .stat-value { color: #E6A23C; }

/* ========== 图例 ========== */
.graph-legend {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 24px;
  background: #0D1117;
  border-bottom: 1px solid #21262D;
  font-size: 12px;
  color: #8B949E;
  flex-shrink: 0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.legend-line {
  display: inline-block;
  width: 24px;
  height: 3px;
  border-radius: 2px;
}

.legend-divider {
  width: 1px;
  height: 16px;
  background: #30363D;
}

/* ========== 画布 ========== */
.graph-canvas-wrapper {
  flex: 1;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 50% 50%, rgba(0, 212, 255, 0.03) 0%, transparent 70%),
    linear-gradient(#0D1117 1px, transparent 1px),
    linear-gradient(90deg, #0D1117 1px, transparent 1px),
    linear-gradient(rgba(48, 54, 61, 0.3) 1px, transparent 1px),
    linear-gradient(90deg, rgba(48, 54, 61, 0.3) 1px, transparent 1px);
  background-size: 100% 100%, 100px 100px, 100px 100px, 20px 20px, 20px 20px;
  background-position: center, 0 0, 0 0, 0 0, 0 0;
}

.graph-svg {
  display: block;
  cursor: grab;

  &:active {
    cursor: grabbing;
  }
}

/* 节点样式 */
.graph-node {
  cursor: pointer;
  transition: filter 0.2s;

  &:hover {
    filter: brightness(1.3);
  }

  &.selected circle:nth-child(2) {
    stroke-width: 3;
    filter: drop-shadow(0 0 8px currentColor);
  }

  &.dragging {
    cursor: grabbing;
  }
}

.node-glow {
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.15; r: inherit; }
  50% { opacity: 0.25; }
}

.node-symbol {
  pointer-events: none;
  font-size: 14px;
  user-select: none;
}

.node-label {
  pointer-events: none;
  user-select: none;
  font-size: 11px !important;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
}

/* 边样式 */
.graph-edge {
  transition: stroke-width 0.2s, opacity 0.2s;
  opacity: 0.6;

  &.highlighted {
    opacity: 1;
    stroke-width: 3 !important;
  }
}

.edge-label {
  font-size: 10px;
  pointer-events: none;
  user-select: none;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.8);
}

/* ========== 节点详情面板 ========== */
.node-detail-panel {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 280px;
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #1C2128;
  border-bottom: 1px solid #30363D;
}

.detail-type-badge {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid;
}

.detail-close {
  color: #6E7681;
  padding: 4px;

  &:hover { color: #E6EDF3; }
}

.detail-body {
  padding: 16px;
}

.detail-name {
  font-size: 16px;
  font-weight: 600;
  color: #E6EDF3;
  margin-bottom: 8px;
}

.detail-desc {
  font-size: 12px;
  color: #8B949E;
  line-height: 1.5;
  margin-bottom: 8px;
}

.detail-meta {
  display: flex;
  gap: 12px;
  font-size: 11px;
  color: #484F58;
  font-family: 'JetBrains Mono', monospace;
  margin-bottom: 12px;
}

.neighbor-section {
  border-top: 1px solid #21262D;
  padding-top: 12px;
}

.neighbor-title {
  font-size: 12px;
  color: #8B949E;
  margin-bottom: 8px;
  font-weight: 600;
}

.neighbor-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.neighbor-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 12px;

  &:hover {
    background: #21262D;
  }
}

.neighbor-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.neighbor-name {
  flex: 1;
  color: #E6EDF3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.neighbor-type {
  font-size: 10px;
  color: #484F58;
  flex-shrink: 0;
}

/* ========== 控制栏 ========== */
.graph-controls {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 8px;
  padding: 8px 12px;
}

.edge-label-toggle {
  font-size: 12px;
  color: #8B949E;

  :deep(.el-checkbox__label) {
    font-size: 12px;
    color: #8B949E;
  }
}

.zoom-info {
  font-size: 12px;
  color: #484F58;
  font-family: 'JetBrains Mono', monospace;
  min-width: 40px;
  text-align: right;
}

/* ========== 过渡动画 ========== */
.detail-fade-enter-active,
.detail-fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.detail-fade-enter-from,
.detail-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
