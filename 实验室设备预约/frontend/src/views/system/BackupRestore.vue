<template>
  <div class="backup-restore">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>数据备份与恢复</h2>
      </div>
    </div>

    <!-- 全局操作 -->
    <div class="page-card action-card">
      <div class="action-header">
        <el-icon class="header-icon"><Connection /></el-icon>
        <span>备份管理</span>
      </div>
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 16px;">
        备份文件将保存至 D:/lab-backup/ 目录。本系统使用 JDBC 方式备份与恢复，无需安装 mysqldump/mysql 客户端。自动备份由系统定时任务执行。
      </el-alert>
      <div class="action-buttons">
        <el-button type="primary" size="default" :loading="backupLoading" @click="handleBackup">
          <el-icon><Download /></el-icon>手动备份
        </el-button>
        <el-button type="success" size="default" :loading="verifyLoading" @click="handleVerifyBackup">
          <el-icon><CircleCheck /></el-icon>检查数据
        </el-button>
        <el-button type="success" plain size="default" @click="showRestoreDialog">
          <el-icon><Upload /></el-icon>恢复数据
        </el-button>
        <el-button type="info" size="default" @click="loadBackupList">
          <el-icon><Refresh /></el-icon>刷新列表
        </el-button>
      </div>
    </div>

    <!-- 备份列表 -->
    <div class="page-card">
      <div class="action-header">
        <el-icon class="header-icon"><Document /></el-icon>
        <span>备份文件列表</span>
      </div>
      <el-table
        v-if="backupList.length > 0"
        class="backup-file-table"
        :data="backupList"
        size="default"
        border
        stripe
        :row-style="{ height: '52px' }"
      >
        <el-table-column prop="name" label="文件名" min-width="280" show-overflow-tooltip />
        <el-table-column prop="size" label="文件大小" width="130" align="center">
          <template #default="{ row }">{{ formatSize(row.size) }}</template>
        </el-table-column>
        <el-table-column prop="lastModified" label="备份时间" min-width="200">
          <template #default="{ row }">{{ formatTime(row.lastModified) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right" align="left">
          <template #default="{ row }">
            <div class="backup-actions">
              <el-button type="primary" plain size="default" @click="downloadBackup(row.path)">
                <el-icon><Download /></el-icon>下载
              </el-button>
              <el-button type="danger" plain size="default" @click="handleDeleteBackup(row)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
              <el-button type="warning" plain size="default" @click="handleRestore(row.path)">
                <el-icon><RefreshLeft /></el-icon>恢复
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无备份文件，请先执行一次手动备份" />
    </div>

    <!-- 自动备份配置 -->
    <div class="page-card">
      <div class="action-header">
        <el-icon class="header-icon"><Timer /></el-icon>
        <span>自动备份配置</span>
        <el-tag type="warning" size="small" style="margin-left: auto;">数据安全</el-tag>
      </div>
      <el-form :model="autoBackupConfig" label-width="140px" class="config-form">
        <el-form-item label="启用自动备份">
          <el-switch
            v-model="autoBackupConfig.enabled"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>
        <template v-if="autoBackupConfig.enabled">
          <el-form-item label="备份周期">
            <el-select v-model="autoBackupConfig.cycle" placeholder="选择备份周期" style="width: 200px;">
              <el-option label="每天" value="DAILY" />
              <el-option label="每周" value="WEEKLY" />
              <el-option label="每月" value="MONTHLY" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="autoBackupConfig.cycle === 'DAILY'" label="备份时间">
            <el-time-picker
              v-model="autoBackupConfig.time"
              format="HH:mm"
              value-format="HH:mm"
              placeholder="选择备份时间"
              style="width: 200px;"
            />
          </el-form-item>
          <el-form-item v-if="autoBackupConfig.cycle === 'WEEKLY'" label="备份星期">
            <el-select v-model="autoBackupConfig.weekDay" placeholder="选择星期几" style="width: 200px;">
              <el-option label="星期一" value="1" />
              <el-option label="星期二" value="2" />
              <el-option label="星期三" value="3" />
              <el-option label="星期四" value="4" />
              <el-option label="星期五" value="5" />
              <el-option label="星期六" value="6" />
              <el-option label="星期日" value="7" />
            </el-select>
          </el-form-item>
          <el-form-item label="保留备份数量">
            <el-input-number v-model="autoBackupConfig.keepCount" :min="1" :max="30" />
            <span class="form-hint">最多保留最近30个备份文件，超出自动清理</span>
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" :loading="saveConfigLoading" @click="saveAutoBackupConfig">保存配置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 恢复数据弹窗 -->
    <el-dialog v-model="restoreVisible" title="恢复数据" width="560px">
      <el-alert type="warning" :closable="false" show-icon style="margin-bottom: 16px;">
        恢复将覆盖当前数据库，请谨慎操作！恢复后需重新登录系统。
      </el-alert>
      <el-tabs v-model="restoreTab">
        <el-tab-pane label="选择已有备份" name="list">
          <el-select v-model="restorePath" placeholder="选择备份文件" filterable style="width: 100%">
            <el-option
              v-for="f in backupList"
              :key="f.path"
              :label="f.name + ' (' + formatTime(f.lastModified) + ')'"
              :value="f.path"
            />
          </el-select>
        </el-tab-pane>
        <el-tab-pane label="上传备份文件" name="upload">
          <el-upload
            drag
            action=""
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".sql"
            :limit="1"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">请上传 SQL 格式的备份文件</div>
            </template>
          </el-upload>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="restoreVisible = false">取消</el-button>
        <el-button type="danger" :loading="restoreLoading" @click="confirmRestore">确认恢复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { backupDatabase, getBackupList, restoreDatabase, deleteBackup, getAutoBackupConfig, saveAutoBackupConfig as saveBackupCfg, uploadAndRestore } from '@/api/sysconfig'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Upload, Refresh, RefreshLeft, Delete, Connection, Document, Timer, CircleCheck } from '@element-plus/icons-vue'

const backupLoading = ref(false)
const verifyLoading = ref(false)
const saveConfigLoading = ref(false)
const restoreLoading = ref(false)
const restoreVisible = ref(false)
const restorePath = ref('')
const restoreTab = ref('list')
const restoreFile = ref(null)
const backupList = ref([])

const autoBackupConfig = reactive({
  enabled: false,
  cycle: 'DAILY',
  time: '02:00',
  weekDay: '1',
  keepCount: 7
})

const loadBackupList = async () => {
  try {
    const res = await getBackupList()
    backupList.value = res.list || []
  } catch (e) {
    console.error(e)
    ElMessage.error('获取备份列表失败')
  }
}

/** 与界面「检查数据」一致：拉取列表并汇总，用于快速验证备份服务与文件是否可用 */
const handleVerifyBackup = async () => {
  verifyLoading.value = true
  try {
    await loadBackupList()
    const list = backupList.value
    if (list.length === 0) {
      ElMessage.warning('暂无备份文件，请先执行「手动备份」')
      return
    }
    const totalBytes = list.reduce((s, f) => s + (Number(f.size) || 0), 0)
    const latest = list[0]
    ElMessage.success(
      `检查完成：共 ${list.length} 个备份文件，最近为「${latest.name}」，合计 ${formatSize(totalBytes)}`
    )
  } catch (e) {
    ElMessage.error(e?.message || '检查失败')
  } finally {
    verifyLoading.value = false
  }
}

const loadAutoConfig = async () => {
  try {
    const res = await getAutoBackupConfig()
    if (res) {
      Object.assign(autoBackupConfig, res)
    }
  } catch (e) {
    console.error('加载自动备份配置失败', e)
  }
}

const handleBackup = async () => {
  backupLoading.value = true
  try {
    const res = await backupDatabase()
    ElMessage.success(res.message || '备份成功')
    loadBackupList()
  } catch (e) {
    ElMessage.error(e.message || '备份失败')
  } finally {
    backupLoading.value = false
  }
}

const downloadBackup = (path) => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  window.open(`${baseUrl}/sys/config/backup/download?path=${encodeURIComponent(path)}`)
}

const handleDeleteBackup = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除备份文件「${row.name}」？`, '删除确认', { type: 'warning' })
    await deleteBackup(row.path)
    ElMessage.success('删除成功')
    loadBackupList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

const showRestoreDialog = () => {
  loadBackupList()
  restorePath.value = ''
  restoreTab.value = 'list'
  restoreFile.value = null
  restoreVisible.value = true
}

const handleFileChange = (file) => {
  restoreFile.value = file.raw
}

const confirmRestore = async () => {
  if (restoreTab.value === 'list' && !restorePath.value) {
    ElMessage.warning('请选择要恢复的备份文件')
    return
  }
  if (restoreTab.value === 'upload' && !restoreFile.value) {
    ElMessage.warning('请上传备份文件')
    return
  }
  try {
    await ElMessageBox.confirm('恢复将覆盖当前数据库，确定继续？', '警告', {
      type: 'warning',
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消'
    })
  } catch { return }

  restoreLoading.value = true
  try {
    if (restoreTab.value === 'list') {
      await restoreDatabase(restorePath.value)
    } else {
      const formData = new FormData()
      formData.append('file', restoreFile.value)
      await uploadAndRestore(formData)
    }
    ElMessage.success('恢复成功，请重新登录')
    setTimeout(() => { window.location.href = '/login' }, 1500)
  } catch (e) {
    ElMessage.error(e.message || '恢复失败')
  } finally {
    restoreLoading.value = false
  }
}

const handleRestore = async (path) => {
  try {
    await ElMessageBox.confirm('恢复将覆盖当前数据库，确定继续？', '警告', {
      type: 'warning',
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消'
    })
  } catch { return }
  restoreLoading.value = true
  try {
    await restoreDatabase(path)
    ElMessage.success('恢复成功，请重新登录')
    setTimeout(() => { window.location.href = '/login' }, 1500)
  } catch (e) {
    ElMessage.error(e.message || '恢复失败')
  } finally {
    restoreLoading.value = false
  }
}

const saveAutoBackupConfig = async () => {
  saveConfigLoading.value = true
  try {
    await saveBackupCfg(autoBackupConfig)
    ElMessage.success('配置保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saveConfigLoading.value = false
  }
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
}

const formatTime = (ts) => {
  if (!ts) return ''
  return new Date(ts).toLocaleString()
}

onMounted(() => {
  loadBackupList()
  loadAutoConfig()
})
</script>

<style scoped lang="scss">
.backup-restore {
  color: #E6EDF3;
  min-height: 100%;
}

.page-card {
  background: #161B22;
  border: 1px solid #30363D;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.page-header-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #161B22 0%, #1A1F35 100%);
  border-left: 4px solid #00D4FF;
  padding: 18px 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
  h2 { margin: 0; font-size: 18px; font-weight: 700; color: #E6EDF3; }
  .header-sub { font-size: 12px; color: #8B949E; }
}

.action-card { padding: 20px 24px; }

.action-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #E6EDF3;
  margin-bottom: 16px;
  .header-icon { color: #00D4FF; }
}

.action-buttons {
  display: flex;
  gap: 12px;
  .el-button {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
}

.config-form {
  max-width: 600px;
  :deep(.el-form-item__label) { color: #8B949E; font-weight: 600; }
}

.form-hint {
  margin-left: 12px;
  color: #8B949E;
  font-size: 13px;
}

.backup-file-table {
  width: 100%;
  :deep(.el-table__cell) {
    padding: 14px 16px;
    font-size: 15px;
    line-height: 1.5;
    color: #e6edf3;
  }
  :deep(.el-table__header .el-table__cell) {
    padding: 14px 16px;
    font-size: 14px;
    font-weight: 600;
    color: #e6edf3;
    background: #21262d !important;
  }
  :deep(.el-table__body tr:hover > td.el-table__cell) {
    background-color: #1c2128 !important;
  }
}

.backup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  :deep(.el-button) {
    min-height: 36px;
    padding: 8px 16px;
    font-size: 14px;
    font-weight: 500;
  }
}

:deep(.el-table) { background: #161B22; color: #E6EDF3; }
:deep(.el-table__header) { background: #21262D; }
:deep(.el-upload-dragger) { background: #0D1117; border-color: #30363D; color: #8B949E; }
:deep(.el-upload-dragger:hover) { border-color: #00D4FF; }
</style>
