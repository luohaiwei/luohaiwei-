<template>
  <div class="booking-rule-global">
    <div class="page-header">
    <h2>预约全局规则</h2>
  </div>

    <div class="config-grid">
      <!-- 左：全局预约基础规则 -->
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <span>全局预约基础规则</span>
            <el-tag type="primary" size="small">核心规则</el-tag>
          </div>
        </template>
        <el-form :model="ruleForm" label-width="150px" class="basic-form">
          <el-form-item label="预约提前申请时长">
            <div class="field-stack">
              <el-input-number v-model="ruleForm.minAdvanceHours" :min="1" :max="720" controls-position="right" />
              <span class="field-desc">用户至少提前多少小时提交预约申请（范围：1~720 小时）</span>
            </div>
          </el-form-item>
          <el-form-item label="单台设备最大预约时长">
            <div class="field-stack">
              <el-input-number v-model="ruleForm.maxBookingHours" :min="1" :max="24" controls-position="right" />
              <span class="field-desc">单次预约单台设备的最长使用时长（小时）</span>
            </div>
          </el-form-item>
          <el-form-item label="每日每设备最大预约次数">
            <div class="field-stack">
              <el-input-number v-model="ruleForm.maxBookingsPerDevicePerDay" :min="1" :max="10" controls-position="right" />
              <span class="field-desc">同一设备同一天最多允许的预约次数</span>
            </div>
          </el-form-item>
          <el-form-item label="每人每日最大预约次数">
            <div class="field-stack">
              <el-input-number v-model="ruleForm.maxBookingsPerUserPerDay" :min="1" :max="20" controls-position="right" />
              <span class="field-desc">同一用户同一天最多允许的预约次数（含所有设备）</span>
            </div>
          </el-form-item>
          <el-form-item label="允许取消的最晚时间">
            <div class="field-stack">
              <el-input-number v-model="ruleForm.cancelDeadlineHours" :min="1" :max="72" controls-position="right" />
              <span class="field-desc">预约开始前多少小时内不允许取消（防止恶意取消）</span>
            </div>
          </el-form-item>
          <el-form-item label="爽约判定阈值">
            <div class="field-stack">
              <el-input-number v-model="ruleForm.noShowThreshold" :min="1" :max="10" controls-position="right" />
              <span class="field-desc">用户累计爽约达到此次数后，自动禁止预约权限</span>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saveLoading" @click="saveRule">保存全局规则</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 右：按角色预约限额（与系统配置「通知场景」同款行样式） -->
      <el-card class="config-card">
        <template #header>
          <div class="card-header">
            <span>按角色预约限额</span>
            <el-tag type="success" size="small">角色配置</el-tag>
          </div>
        </template>
        <el-form :model="roleLimits" label-width="0" class="role-form">
          <el-form-item>
            <div class="notify-scenes">
              <div class="scene-item">
                <el-input-number v-model="roleLimits.studentDeviceHours" :min="1" :max="12" controls-position="right" class="scene-number" />
                <span class="scene-label">学生单日上限</span>
                <span class="scene-desc">学生单台设备单日累计使用上限（小时），防止资源垄断</span>
              </div>
              <div class="scene-item">
                <el-input-number v-model="roleLimits.teacherDeviceHours" :min="1" :max="24" controls-position="right" class="scene-number" />
                <span class="scene-label">教师单日上限</span>
                <span class="scene-desc">教师单台设备单日累计使用上限（小时）</span>
              </div>
              <div class="scene-item">
                <el-switch v-model="roleLimits.maintainerCanBook" />
                <span class="scene-label">维护人员预约</span>
                <span class="scene-desc">是否允许维护人员提交设备预约</span>
              </div>
              <div class="scene-item">
                <el-switch v-model="roleLimits.studentNeedAudit" />
                <span class="scene-label">学生需审核</span>
                <span class="scene-desc">学生提交后是否需教师或管理员审核</span>
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saveRoleLimitLoading" @click="saveRoleLimits">保存角色限额</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 底部通栏：工作时段 -->
      <el-card class="config-card config-span-2">
        <template #header>
          <div class="card-header">
            <span>全局工作时段配置</span>
            <el-tag type="info" size="small">时段配置</el-tag>
          </div>
        </template>
        <el-form label-width="150px" class="time-form">
          <el-form-item label="允许预约时段">
            <div class="field-stack field-stack--inline">
              <div class="time-range-row">
                <el-time-picker
                  v-model="globalTimeRange.start"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="开始"
                  class="time-picker-field"
                />
                <span class="time-range-sep">至</span>
                <el-time-picker
                  v-model="globalTimeRange.end"
                  format="HH:mm"
                  value-format="HH:mm"
                  placeholder="结束"
                  class="time-picker-field"
                />
              </div>
              <span class="field-desc">超出此时段的预约申请将被系统自动拒绝</span>
            </div>
          </el-form-item>
          <el-form-item label="允许预约的星期">
            <div class="field-stack">
              <el-checkbox-group v-model="globalTimeRange.weekdays" class="weekday-group">
                <el-checkbox label="1">周一</el-checkbox>
                <el-checkbox label="2">周二</el-checkbox>
                <el-checkbox label="3">周三</el-checkbox>
                <el-checkbox label="4">周四</el-checkbox>
                <el-checkbox label="5">周五</el-checkbox>
                <el-checkbox label="6">周六</el-checkbox>
                <el-checkbox label="7">周日</el-checkbox>
              </el-checkbox-group>
              <span class="field-desc">未勾选的星期将拒绝所有预约申请</span>
            </div>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saveTimeLoading" @click="saveTimeRange">保存工作时段</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getGlobalBookingRules, saveGlobalBookingRules } from '@/api/bookingRule'
import { ElMessage } from 'element-plus'

const saveLoading = ref(false)
const saveRoleLimitLoading = ref(false)
const saveTimeLoading = ref(false)

const ruleForm = reactive({
  minAdvanceHours: 24,
  maxBookingHours: 4,
  maxBookingsPerDevicePerDay: 3,
  maxBookingsPerUserPerDay: 5,
  cancelDeadlineHours: 2,
  noShowThreshold: 3
})

const roleLimits = reactive({
  studentDeviceHours: 4,
  teacherDeviceHours: 8,
  maintainerCanBook: false,
  studentNeedAudit: true
})

const globalTimeRange = reactive({
  start: '08:00',
  end: '22:00',
  weekdays: ['1', '2', '3', '4', '5']
})

const loadRules = async () => {
  try {
    const res = await getGlobalBookingRules()
    if (res) {
      Object.assign(ruleForm, res.rule || {})
      Object.assign(roleLimits, res.roleLimits || {})
      if (res.timeRange) {
        globalTimeRange.start = res.timeRange.start || '08:00'
        globalTimeRange.end = res.timeRange.end || '22:00'
        const wd = res.timeRange.weekdays || ['1', '2', '3', '4', '5']
        globalTimeRange.weekdays = wd.map((d) => String(d))
      }
    }
  } catch (e) {
    console.error('加载规则失败', e)
  }
}

function inRange (n, min, max) {
  return typeof n === 'number' && !Number.isNaN(n) && n >= min && n <= max
}

const saveRule = async () => {
  const r = ruleForm
  if (!inRange(r.minAdvanceHours, 1, 720)) {
    ElMessage.warning('预约提前申请时长须在 1~720 小时之间')
    return
  }
  if (!inRange(r.maxBookingHours, 1, 24)) {
    ElMessage.warning('单台设备最大预约时长须在 1~24 小时之间')
    return
  }
  if (!inRange(r.maxBookingsPerDevicePerDay, 1, 10)) {
    ElMessage.warning('每日每设备最大预约次数须在 1~10 之间')
    return
  }
  if (!inRange(r.maxBookingsPerUserPerDay, 1, 20)) {
    ElMessage.warning('每人每日最大预约次数须在 1~20 之间')
    return
  }
  if (!inRange(r.cancelDeadlineHours, 1, 72)) {
    ElMessage.warning('允许取消预约的最晚时间须在 1~72 小时之间')
    return
  }
  if (!inRange(r.noShowThreshold, 1, 10)) {
    ElMessage.warning('爽约判定阈值须在 1~10 之间')
    return
  }
  saveLoading.value = true
  try {
    await saveGlobalBookingRules({ type: 'basic', data: ruleForm })
    ElMessage.success('全局规则保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

const saveRoleLimits = async () => {
  const rl = roleLimits
  if (!inRange(rl.studentDeviceHours, 1, 12)) {
    ElMessage.warning('学生单台设备单日上限须在 1~12 小时之间')
    return
  }
  if (!inRange(rl.teacherDeviceHours, 1, 24)) {
    ElMessage.warning('教师单台设备单日上限须在 1~24 小时之间')
    return
  }
  saveRoleLimitLoading.value = true
  try {
    await saveGlobalBookingRules({ type: 'roleLimits', data: roleLimits })
    ElMessage.success('角色限额保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saveRoleLimitLoading.value = false
  }
}

const saveTimeRange = async () => {
  const tr = globalTimeRange
  if (!tr.start || !tr.end) {
    ElMessage.warning('请填写允许预约的开始与结束时间')
    return
  }
  if (tr.start >= tr.end) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  if (!tr.weekdays || tr.weekdays.length === 0) {
    ElMessage.warning('请至少选择一个星期')
    return
  }
  saveTimeLoading.value = true
  try {
    await saveGlobalBookingRules({ type: 'timeRange', data: globalTimeRange })
    ElMessage.success('工作时段保存成功')
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saveTimeLoading.value = false
  }
}

onMounted(() => loadRules())
</script>

<style scoped lang="scss">
/* 与系统配置页 SysConfig.vue 对齐 */
.booking-rule-global {
  color: #E6EDF3;
  width: 100%;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px;
  color: #E6EDF3;
  font-size: 24px;
  font-weight: 700;
}

.page-sub {
  margin: 0;
  font-size: 13px;
  color: #8B949E;
  line-height: 1.5;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.config-span-2 {
  grid-column: 1 / -1;
}

.config-card {
  background: #161B22;
  border: 1px solid #30363D;
  margin-bottom: 0;

  &:hover {
    border-color: rgba(0, 212, 255, 0.3);
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

:deep(.el-card__body) {
  padding: 20px 20px 8px;
}

@media (max-width: 1200px) {
  .config-grid {
    grid-template-columns: 1fr;
  }
  .config-span-2 {
    grid-column: auto;
  }
}

.basic-form,
.time-form {
  :deep(.el-form-item__label) {
    color: #8B949E;
    font-weight: 500;
  }
  :deep(.el-form-item__content) {
    display: block;
  }
}

.field-stack {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  width: 100%;
  flex-wrap: wrap;
}

.field-desc {
  flex: 1;
  min-width: 200px;
}

.field-stack--inline {
  gap: 10px;
}

.field-desc {
  display: block;
  color: #8B949E;
  font-size: 13px;
  line-height: 1.55;
  max-width: 100%;
}

.basic-form :deep(.el-input-number) {
  width: 168px;
}

.role-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.notify-scenes {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.scene-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #0D1117;
  border-radius: 6px;
  border: 1px solid #30363D;
  flex-wrap: wrap;
}

.scene-number {
  flex-shrink: 0;
  width: 140px !important;
}

.scene-label {
  color: #E6EDF3;
  font-weight: 500;
  min-width: 100px;
  flex-shrink: 0;
}

.scene-desc {
  color: #8B949E;
  font-size: 13px;
  flex: 1;
  min-width: 200px;
  line-height: 1.5;
}

.time-range-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
}

.time-range-sep {
  color: #8B949E;
  font-size: 13px;
}

.time-picker-field {
  width: 130px;
}

.weekday-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
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
