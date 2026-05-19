<template>
  <div class="notification-page">
    <el-card class="filter-card">
      <div class="filter-row">
        <el-select v-model="queryForm.type" placeholder="消息类型" clearable class="filter-select-type">
          <el-option label="预约审核" value="BOOKING_AUDIT" />
          <el-option label="工单分配" value="REPAIR_ASSIGN" />
          <el-option label="校准提醒" value="CALIBRATION_REMIND" />
          <el-option label="系统通知" value="SYSTEM" />
        </el-select>
        <el-select v-model="queryForm.isRead" placeholder="阅读状态" clearable class="filter-select-read">
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
        <div class="filter-daterange-wrap">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="filter-daterange"
          />
        </div>
        <div class="filter-actions">
          <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>
      </div>
      <div class="action-row">
        <el-button
          type="success"
          :icon="Check"
          :disabled="unreadCount === 0"
          @click="handleReadAll"
        >
          全部标为已读 ({{ unreadCount }})
        </el-button>
        <span class="unread-tip" v-if="unreadCount > 0">
          您有 {{ unreadCount }} 条未读消息
        </span>
      </div>
    </el-card>

    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        :max-height="tableMaxHeight"
        @row-click="handleRowClick"
        row-class-name="message-row"
      >
        <el-table-column label="状态" width="60" align="center">
          <template #default="{ row }">
            <span v-if="row.isRead === 0" class="unread-dot" title="未读"></span>
            <span v-else class="read-icon">&#10003;</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeText(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" min-width="280" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" align="center" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="op-btns">
              <el-button
                v-if="row.isRead === 0"
                type="primary"
                plain
                size="default"
                @click.stop="handleRead(row)"
              >标为已读</el-button>
              <el-button
                type="danger"
                plain
                size="default"
                @click.stop="handleDelete(row)"
              >删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Check } from '@element-plus/icons-vue'
import { getMyMessages, markAsRead, markAllAsRead, deleteMessage, getUnreadCount } from '@/api/notification'

const loading = ref(false)
const tableData = ref([])
const unreadCount = ref(0)
const dateRange = ref([])

const queryForm = reactive({
  type: '',
  isRead: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const tableMaxHeight = ref(520)
function updateTableMaxHeight () {
  tableMaxHeight.value = Math.max(400, window.innerHeight - 300)
}

const typeMap = {
  BOOKING_AUDIT: { text: '预约审核', tag: 'warning' },
  REPAIR_ASSIGN: { text: '工单分配', tag: 'danger' },
  CALIBRATION_REMIND: { text: '校准提醒', tag: 'info' },
  SYSTEM: { text: '系统通知', tag: '' }
}

const typeText = (type) => typeMap[type]?.text || type || '未知'
const typeTag = (type) => typeMap[type]?.tag || ''

async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNum: pagination.page,
      pageSize: pagination.size,
      type: queryForm.type || undefined,
      isRead: queryForm.isRead !== '' ? queryForm.isRead : undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined
    }
    const res = await getMyMessages(params)
    tableData.value = res?.list || []
    pagination.total = res?.total || 0
    unreadCount.value = res?.unreadCount || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    unreadCount.value = await getUnreadCount()
  } catch (e) {
    console.error(e)
  }
}

function reset() {
  queryForm.type = ''
  queryForm.isRead = ''
  dateRange.value = []
  pagination.page = 1
  loadData()
}

async function handleRead(row) {
  try {
    await markAsRead(row.id)
    row.isRead = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

async function handleReadAll() {
  try {
    await ElMessageBox.confirm(`确定将全部 ${unreadCount.value} 条未读消息标为已读？`, '确认操作')
    await markAllAsRead()
    ElMessage.success('全部已读')
    unreadCount.value = 0
    tableData.value.forEach(r => (r.isRead = 1))
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该消息？', '确认删除', { type: 'warning' })
    await deleteMessage(row.id)
    ElMessage.success('删除成功')
    if (row.isRead === 0) unreadCount.value = Math.max(0, unreadCount.value - 1)
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

function handleRowClick(row) {
  if (row.isRead === 0) handleRead(row)
}

onMounted(() => {
  updateTableMaxHeight()
  window.addEventListener('resize', updateTableMaxHeight)
  loadData()
  loadUnreadCount()
})

onUnmounted(() => {
  window.removeEventListener('resize', updateTableMaxHeight)
})
</script>

<style lang="scss" scoped>
.notification-page {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}

.filter-select-type {
  width: 180px;
  flex-shrink: 0;
}

.filter-select-read {
  width: 140px;
  flex-shrink: 0;
}

/* 限制日期范围选择器宽度，避免占满一行 */
.filter-daterange-wrap {
  flex: 0 1 280px;
  min-width: 220px;
  max-width: 280px;
}

.filter-daterange {
  width: 100% !important;
  max-width: 280px;
}

.filter-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

@media (max-width: 900px) {
  .filter-daterange-wrap {
    flex: 1 1 100%;
    max-width: none;
  }
  .filter-actions {
    margin-left: 0;
  }
}

.op-btns {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.table-card :deep(.el-table .el-button + .el-button) {
  margin-left: 0;
}

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.unread-tip {
  color: #E6A23C;
  font-size: 13px;
}

.table-card :deep(.message-row) {
  cursor: pointer;
}

.unread-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #F56C6C;
}

.read-icon {
  color: #67C23A;
  font-size: 14px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
