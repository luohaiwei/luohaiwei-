<template>
  <el-dialog
    v-model="visible"
    :title="'操作引导 - 预计 ' + guideDuration + ' 分钟'"
    width="700px"
    class="operation-guide-dialog"
    :close-on-click-modal="false"
  >
    <div class="guide-container">

      <!-- 动画进度条 -->
      <div class="progress-track">
        <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
      </div>

      <!-- 步骤指示器 -->
      <el-steps :active="currentStep" finish-status="success" align-center class="guide-steps">
        <el-step
          v-for="(step, index) in steps"
          :key="index"
          :title="step.step + ''"
          :class="{ 'is-animating': index === currentStep }"
        />
      </el-steps>

      <!-- 当前步骤内容（带过渡动画） -->
      <Transition name="step-fade" mode="out-in">
        <div class="step-content" v-if="currentStepData" :key="currentStep">
          <div class="step-header">
            <span class="step-badge">第 {{ currentStepData.step }} 步</span>
            <span class="step-title">{{ currentStepData.title }}</span>
          </div>

          <!-- 步骤序号大图标 -->
          <div class="step-icon-wrapper">
            <div class="step-number-ring">
              <span class="step-number">{{ currentStepData.step }}</span>
            </div>
          </div>

          <!-- 图片展示 -->
          <div v-if="currentStepData.image" class="step-image-container">
            <img :src="currentStepData.image" alt="步骤图片" class="step-image" />
          </div>

          <!-- 步骤内容 -->
          <div class="step-text" v-html="formatContent(currentStepData.content)"></div>

          <!-- 安全警示 -->
          <div v-if="currentStepData.warning" class="step-warning">
            <el-icon class="warning-icon"><WarningFilled /></el-icon>
            <div class="warning-content">
              <div class="warning-title">安全警示</div>
              <div class="warning-text">{{ currentStepData.warning }}</div>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 进度文字 -->
      <div class="step-progress">
        <span>已完成 {{ currentStep + 1 }} 步，共 {{ steps.length }} 步</span>
        <span class="progress-percent">{{ progressPercent }}%</span>
      </div>

      <!-- 完成时的庆祝文字 -->
      <Transition name="celebrate">
        <div v-if="isLastStep && showCelebrate" class="celebrate-banner">
          <el-icon class="celebrate-icon"><CircleCheck /></el-icon>
          <span>恭喜！您已完成全部操作步骤</span>
        </div>
      </Transition>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button
          @click="prevStep"
          :disabled="currentStep === 0"
          class="footer-btn-prev"
        >
          <el-icon><ArrowLeft /></el-icon>
          上一步
        </el-button>

        <!-- 步骤小圆点导航 -->
        <div class="step-dots">
          <span
            v-for="(_, i) in steps"
            :key="i"
            class="step-dot"
            :class="{ active: i === currentStep, done: i < currentStep }"
            @click="jumpToStep(i)"
          ></span>
        </div>

        <el-button
          type="primary"
          @click="nextStep"
          :class="isLastStep ? 'footer-btn-finish' : 'footer-btn-next'"
        >
          {{ isLastStep ? '完成引导' : '下一步' }}
          <el-icon v-if="!isLastStep"><ArrowRight /></el-icon>
          <el-icon v-else><CircleCheck /></el-icon>
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { WarningFilled, ArrowLeft, ArrowRight, CircleCheck } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  guideSteps: {
    type: [String, Array],
    default: ''
  },
  guideDuration: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const currentStep = ref(0)
const steps = ref([])
const showCelebrate = ref(false)

const progressPercent = computed(() => {
  if (!steps.value.length) return 0
  return Math.round(((currentStep.value + 1) / steps.value.length) * 100)
})

watch(() => props.guideSteps, (val) => {
  if (val) {
    try {
      steps.value = typeof val === 'string' ? JSON.parse(val) : val
      currentStep.value = 0
      showCelebrate.value = false
    } catch (e) {
      console.error('解析引导步骤失败', e)
      steps.value = []
    }
  }
}, { immediate: true })

const currentStepData = computed(() => {
  return steps.value[currentStep.value] || null
})

const isLastStep = computed(() => {
  return currentStep.value === steps.value.length - 1
})

const prevStep = () => {
  if (currentStep.value > 0) {
    showCelebrate.value = false
    currentStep.value--
  }
}

const nextStep = () => {
  if (isLastStep.value) {
    visible.value = false
    currentStep.value = 0
    showCelebrate.value = false
  } else {
    currentStep.value++
    if (isLastStep.value) {
      setTimeout(() => { showCelebrate.value = true }, 300)
    }
  }
}

const jumpToStep = (index) => {
  if (index >= 0 && index < steps.value.length) {
    showCelebrate.value = false
    currentStep.value = index
    if (isLastStep.value) {
      setTimeout(() => { showCelebrate.value = true }, 300)
    }
  }
}

const formatContent = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}

watch(visible, (val) => {
  if (!val) {
    currentStep.value = 0
    showCelebrate.value = false
  }
})
</script>

<style lang="scss" scoped>
.operation-guide-dialog {
  --el-dialog-bg-color: #161B22;
  --el-dialog-border-color: #30363D;
}

.guide-container {
  min-height: 420px;
  position: relative;
}

/* ========== 进度条 ========== */
.progress-track {
  height: 4px;
  background: #30363D;
  border-radius: 2px;
  margin-bottom: 24px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #00D4FF, #7B61FF);
  border-radius: 2px;
  transition: width 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}

/* ========== 步骤指示器 ========== */
.guide-steps {
  margin-bottom: 24px;
}

:deep(.el-step__title) {
  color: #8B949E !important;
  font-size: 12px;
}

:deep(.el-step__title.is-finish) {
  color: #00C98C !important;
}

:deep(.el-step__title.is-process) {
  color: #E6EDF3 !important;
}

:deep(.el-step__icon) {
  border-color: #30363D !important;
}

:deep(.el-step__icon.is-text) {
  background: #21262D;
  border-color: #30363D;
}

:deep(.el-step__icon.is-finish) {
  background: #00C98C;
  border-color: #00C98C;
}

/* ========== 步骤内容 ========== */
.step-content {
  background: #21262D;
  border-radius: 12px;
  padding: 24px;
  border: 1px solid #30363D;
  position: relative;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.step-badge {
  background: linear-gradient(135deg, #00D4FF, #7B61FF);
  color: white;
  padding: 4px 14px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.step-title {
  font-size: 18px;
  font-weight: 600;
  color: #E6EDF3;
}

/* 步骤序号圆形图标 */
.step-icon-wrapper {
  display: flex;
  justify-content: center;
  margin-bottom: 20px;
}

.step-number-ring {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(0, 212, 255, 0.15), rgba(123, 97, 255, 0.15));
  border: 2px solid rgba(0, 212, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
}

.step-number {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #00D4FF, #7B61FF);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.step-image-container {
  margin-bottom: 16px;
  text-align: center;
}

.step-image {
  max-width: 100%;
  max-height: 200px;
  border-radius: 8px;
  border: 1px solid #30363D;
}

.step-text {
  color: #8B949E;
  line-height: 1.9;
  font-size: 14px;
  white-space: pre-wrap;
  background: rgba(0, 212, 255, 0.04);
  border-left: 3px solid rgba(0, 212, 255, 0.3);
  padding: 12px 16px;
  border-radius: 0 8px 8px 0;
}

.step-warning {
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid #F56C6C;
  border-radius: 8px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.warning-icon {
  color: #F56C6C;
  font-size: 24px;
  flex-shrink: 0;
}

.warning-content {
  flex: 1;
}

.warning-title {
  color: #F56C6C;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.warning-text {
  color: #E6EDF3;
  font-size: 13px;
  line-height: 1.6;
}

/* ========== 进度文字 ========== */
.step-progress {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  padding: 0 4px;
}

.step-progress span {
  color: #8B949E;
  font-size: 13px;
}

.progress-percent {
  font-weight: 600;
  color: #00D4FF !important;
  font-family: 'JetBrains Mono', monospace;
}

/* ========== 庆祝横幅 ========== */
.celebrate-banner {
  margin-top: 16px;
  padding: 12px 20px;
  background: linear-gradient(90deg, rgba(0, 201, 140, 0.15), rgba(0, 212, 255, 0.15));
  border: 1px solid rgba(0, 201, 140, 0.4);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #00C98C;
}

.celebrate-icon {
  font-size: 20px;
}

/* ========== 底部操作区 ========== */
.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.footer-btn-prev {
  min-width: 100px;
}

.footer-btn-next,
.footer-btn-finish {
  min-width: 120px;
}

.footer-btn-finish {
  background: linear-gradient(135deg, #00C98C, #00D4FF) !important;
  border: none !important;

  &:hover {
    opacity: 0.85;
    transform: scale(1.02);
  }
}

/* 步骤小圆点 */
.step-dots {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  justify-content: center;
  flex-wrap: wrap;
}

.step-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #30363D;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;

  &.done {
    background: #00C98C;
    transform: scale(0.8);
  }

  &.active {
    background: #00D4FF;
    transform: scale(1.3);
    border-color: rgba(0, 212, 255, 0.5);
    box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
  }

  &:hover {
    background: #484F58;
  }
}

/* ========== 过渡动画 ========== */

/* 步骤内容切换：淡入淡出 + 轻微上移 */
.step-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.step-fade-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 1, 1);
}
.step-fade-enter-from {
  opacity: 0;
  transform: translateY(16px);
}
.step-fade-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}
.step-fade-enter-to,
.step-fade-leave-from {
  opacity: 1;
  transform: translateY(0);
}

/* 庆祝动画 */
.celebrate-enter-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.celebrate-leave-active {
  transition: all 0.2s ease;
}
.celebrate-enter-from {
  opacity: 0;
  transform: scale(0.7) translateY(10px);
}
.celebrate-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
.celebrate-enter-to,
.celebrate-leave-from {
  opacity: 1;
  transform: scale(1) translateY(0);
}

/* Element Plus Dialog 缩放动画（覆盖默认） */
:deep(.el-dialog) {
  animation: dialogZoomIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

:deep(.el-overlay) {
  animation: overlayFadeIn 0.25s ease;
}

@keyframes dialogZoomIn {
  from {
    opacity: 0;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes overlayFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
