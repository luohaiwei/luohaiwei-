<template>
  <div class="sys-config">
    <div class="page-header">
      <h2>系统配置</h2>
    </div>

    <div class="config-grid">
      <!-- 预约审核流程配置 -->
      <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>预约审核流程配置</span>
              <el-tag type="primary" size="small">核心配置</el-tag>
            </div>
          </template>
          <el-form :model="auditConfig" label-width="140px">
            <el-form-item label="审核模式">
              <el-radio-group v-model="auditConfig.mode">
                <el-radio label="MANUAL">人工审核</el-radio>
                <el-radio label="AUTO">自动审核</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审核说明">
              <div class="audit-desc">
                <div class="desc-item" :class="{ 'desc-item--active': auditConfig.mode === 'MANUAL' }">
                  <div class="desc-label">
                    <el-icon><InfoFilled /></el-icon>
                    <span class="desc-title">人工审核</span>
                    <el-tag size="small" type="warning">当前模式</el-tag>
                  </div>
                  <div class="desc-text">教师/管理员需手动审核预约申请</div>
                </div>
                <div class="desc-item" :class="{ 'desc-item--active': auditConfig.mode === 'AUTO' }">
                  <div class="desc-label">
                    <el-icon><InfoFilled /></el-icon>
                    <span class="desc-title">自动审核</span>
                    <el-tag size="small" type="success">推荐</el-tag>
                  </div>
                  <div class="desc-text">系统自动通过符合条件的预约申请</div>
                </div>
              </div>
            </el-form-item>
            <el-form-item v-if="auditConfig.mode === 'AUTO'" label="自动审核条件">
              <el-checkbox-group v-model="auditConfig.autoConditions">
                <el-checkbox label="noConflict">无时间冲突</el-checkbox>
                <el-checkbox label="withinLimit">在预约限额内</el-checkbox>
                <el-checkbox label="advanceTime">符合提前申请时间</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saveLoading.audit" @click="saveAuditConfig">保存配置</el-button>
            </el-form-item>
          </el-form>
      </el-card>

      <!-- 消息通知配置 -->
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <span>消息通知方式配置</span>
            <el-tag type="success" size="small">通知配置</el-tag>
          </div>
        </template>
        <el-form :model="notifyConfig" label-width="140px">
          <el-form-item label="通知方式">
            <el-checkbox-group v-model="notifyConfig.channels">
              <el-checkbox label="SYSTEM">系统消息</el-checkbox>
              <el-checkbox label="SMS">短信通知</el-checkbox>
              <el-checkbox label="EMAIL">邮件通知</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="通知场景">
            <div class="notify-scenes">
              <div class="scene-item">
                <el-switch v-model="notifyConfig.scenes.bookingSubmit" />
                <span class="scene-label">预约提交</span>
                <span class="scene-desc">预约申请提交后通知审核人员</span>
              </div>
              <div class="scene-item">
                <el-switch v-model="notifyConfig.scenes.bookingAudit" />
                <span class="scene-label">审核结果</span>
                <span class="scene-desc">预约审核通过后通知申请人</span>
              </div>
              <div class="scene-item">
                <el-switch v-model="notifyConfig.scenes.repairAssign" />
                <span class="scene-label">工单分配</span>
                <span class="scene-desc">维修工单分配给维护人员时通知</span>
              </div>
              <div class="scene-item">
                <el-switch v-model="notifyConfig.scenes.calibrationReminder" />
                <span class="scene-label">校准提醒</span>
                <span class="scene-desc">设备校准到期前自动提醒</span>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saveLoading.notify" @click="saveNotifyConfig">保存配置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import { getConfigsByGroup, saveConfigs } from '../../api/sysconfig'

// 加载状态
const saveLoading = ref({
  audit: false,
  notify: false
})

// 预约审核配置
const auditConfig = ref({
  mode: 'MANUAL',
  autoConditions: ['noConflict', 'withinLimit', 'advanceTime']
})

// 通知配置
const notifyConfig = ref({
  channels: ['SYSTEM'],
  scenes: {
    bookingSubmit: true,
    bookingAudit: true,
    repairAssign: true,
    calibrationReminder: true
  }
})

// 保存审核配置
const saveAuditConfig = async () => {
  saveLoading.value.audit = true
  try {
    const configs = [
      { key: 'booking.audit.mode', value: JSON.stringify(auditConfig.value.mode), type: 'STRING', name: '预约审核模式', group: 'BOOKING', description: 'MANUAL=人工审核, AUTO=自动审核' },
      { key: 'booking.audit.autoConditions', value: JSON.stringify(auditConfig.value.autoConditions), type: 'JSON', name: '自动审核条件', group: 'BOOKING', description: '无冲突+限额+提前时间同时满足时自动通过' }
    ]
    await saveConfigs(configs)
    ElMessage.success('审核配置保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saveLoading.value.audit = false
  }
}

// 保存通知配置
const saveNotifyConfig = async () => {
  saveLoading.value.notify = true
  try {
    const configs = [
      { key: 'notification.channels', value: JSON.stringify(notifyConfig.value.channels), type: 'JSON', name: '通知方式', group: 'NOTIFICATION', description: 'SYSTEM=系统消息, SMS=短信, EMAIL=邮件' },
      { key: 'notification.scene.bookingSubmit', value: String(notifyConfig.value.scenes.bookingSubmit), type: 'BOOLEAN', name: '预约提交通知', group: 'NOTIFICATION' },
      { key: 'notification.scene.bookingAudit', value: String(notifyConfig.value.scenes.bookingAudit), type: 'BOOLEAN', name: '审核结果通知', group: 'NOTIFICATION' },
      { key: 'notification.scene.repairAssign', value: String(notifyConfig.value.scenes.repairAssign), type: 'BOOLEAN', name: '维修工单分配通知', group: 'NOTIFICATION' },
      { key: 'notification.scene.calibrationReminder', value: String(notifyConfig.value.scenes.calibrationReminder), type: 'BOOLEAN', name: '校准到期提醒', group: 'NOTIFICATION' }
    ]
    await saveConfigs(configs)
    ElMessage.success('通知配置保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saveLoading.value.notify = false
  }
}

// 加载后端配置
const loadConfigsFromBackend = async () => {
  try {
    // 加载预约审核配置
    const bookingConfigs = (await getConfigsByGroup('BOOKING')).list || []
    const auditMode = bookingConfigs.find(c => c.configKey === 'booking.audit.mode')
    if (auditMode) auditConfig.value.mode = auditMode.configValue === 'MANUAL' ? 'MANUAL' : 'AUTO'
    const auditCond = bookingConfigs.find(c => c.configKey === 'booking.audit.autoConditions')
    if (auditCond && Array.isArray(auditCond.configValue)) auditConfig.value.autoConditions = auditCond.configValue

    // 加载通知配置
    const notifyConfigs = (await getConfigsByGroup('NOTIFICATION')).list || []
    const ch = notifyConfigs.find(c => c.configKey === 'notification.channels')
    if (ch && Array.isArray(ch.configValue)) notifyConfig.value.channels = ch.configValue
    const bs = notifyConfigs.find(c => c.configKey === 'notification.scene.bookingSubmit')
    if (bs) notifyConfig.value.scenes.bookingSubmit = bs.configValue === 'true'
    const ba = notifyConfigs.find(c => c.configKey === 'notification.scene.bookingAudit')
    if (ba) notifyConfig.value.scenes.bookingAudit = ba.configValue === 'true'
    const ra = notifyConfigs.find(c => c.configKey === 'notification.scene.repairAssign')
    if (ra) notifyConfig.value.scenes.repairAssign = ra.configValue === 'true'
    const cr = notifyConfigs.find(c => c.configKey === 'notification.scene.calibrationReminder')
    if (cr) notifyConfig.value.scenes.calibrationReminder = cr.configValue === 'true'
  } catch (e) {
    console.error('加载配置失败:', e)
  }
}

onMounted(() => {
  loadConfigsFromBackend()
})
</script>

<style lang="scss" scoped>
.sys-config {
  color: #E6EDF3;
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  color: #E6EDF3;
  font-size: 24px;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.config-card {
  background: #161B22;
  border: 1px solid #30363D;
  margin-bottom: 0;

  &:hover {
    border-color: rgba(0, 212, 255, 0.3);
  }
}

@media (max-width: 1200px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-card__header) {
  color: #E6EDF3;
  border-bottom-color: #30363D;
  background: #21262D;
}

:deep(.el-form-item__label) {
  color: #8B949E;
}

.audit-desc {
  margin-top: 12px;
  padding: 12px 16px;
  background: #0D1117;
  border-radius: 6px;
  border: 1px solid #30363D;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.desc-item {
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid transparent;
  transition: all 0.2s ease;
}

.desc-item:hover {
  border-color: #30363D;
}

.desc-item--active {
  background: rgba(0, 212, 255, 0.08);
  border-color: rgba(0, 212, 255, 0.3);
}

.desc-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.desc-label .el-icon {
  color: #00D4FF;
  flex-shrink: 0;
}

.desc-title {
  color: #E6EDF3;
  font-weight: 600;
  font-size: 14px;
}

.desc-text {
  color: #8B949E;
  font-size: 13px;
  padding-left: 26px;
}

.notify-scenes {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.scene-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: #0D1117;
  border-radius: 6px;
  border: 1px solid #30363D;
}

.scene-label {
  color: #E6EDF3;
  font-weight: 500;
  min-width: 80px;
}

.scene-desc {
  color: #8B949E;
  font-size: 13px;
  flex: 1;
}

:deep(.el-radio__label) {
  color: #E6EDF3;
}

:deep(.el-checkbox__label) {
  color: #E6EDF3;
}

:deep(.el-switch__label) {
  color: #8B949E;
}

:deep(.el-switch__label.is-active) {
  color: #00D4FF;
}
</style>
