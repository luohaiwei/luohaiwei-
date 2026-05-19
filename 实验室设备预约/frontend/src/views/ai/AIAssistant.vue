<template>
  <div class="ai-assistant">
    <!-- 图文引导弹窗 -->
    <OperationGuide
      v-model="guideVisible"
      :guide-steps="guideSteps"
      :guide-duration="guideDuration"
    />

    <!-- 分级预警弹窗 -->
    <el-dialog
      v-model="dangerDialogVisible"
      :title="dangerDialogConfig.title"
      width="500px"
      :class="dangerDialogConfig.class"
      :show-close="dangerDialogConfig.showClose"
      :close-on-click-modal="dangerDialogConfig.closeOnClick"
      :close-on-press-escape="dangerDialogConfig.closeOnEscape"
      :type="dangerDialogConfig.type"
    >
      <div class="danger-dialog-content">
        <div class="danger-icon-wrapper">
          <el-icon :size="48" :color="dangerDialogConfig.iconColor">
            <component :is="dangerDialogConfig.icon" />
          </el-icon>
        </div>
        <div class="danger-question">{{ currentDangerQuestion }}</div>
        <div class="danger-message">{{ currentDangerAnswer }}</div>
        <div v-if="dangerLevel === 'danger'" class="correct-operation">
          <div class="correct-title">正确操作建议：</div>
          <div class="correct-content">
            1. 立即停止当前操作<br/>
            2. 离开危险区域<br/>
            3. 通知实验室管理人员<br/>
            4. 如有紧急情况请拨打校园报警电话
          </div>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button v-if="dangerLevel === 'info'" type="primary" @click="dangerDialogVisible = false">我知道了</el-button>
          <el-button v-if="dangerLevel === 'warning'" type="warning" @click="dangerDialogVisible = false">我已了解风险</el-button>
          <el-button v-if="dangerLevel === 'danger'" type="danger" @click="dangerDialogVisible = false">确认紧急停止</el-button>
        </div>
      </template>
    </el-dialog>

    <div class="chat-container">
      <!-- 左侧知识库 -->
      <div class="sidebar">
        <div class="sidebar-title">
          <el-icon><Folder /></el-icon>
          知识库分类
        </div>
        <div class="category-list">
          <div
            v-for="(category, index) in categories"
            :key="category"
            class="category-item"
            :class="{ active: currentCategory === category }"
            :style="{ animationDelay: index * 0.05 + 's' }"
            @click="selectCategory(category)"
          >
            <div class="category-icon" :class="'icon-' + (index + 1)">
              <el-icon><component :is="categoryIcons[index]" /></el-icon>
            </div>
            <span class="category-text">{{ category }}</span>
            <el-icon class="category-arrow"><ArrowRight /></el-icon>
          </div>
        </div>
        <!-- 分类知识列表 -->
        <div v-if="knowledgeItems.length > 0" class="knowledge-list">
          <div class="knowledge-list-title">{{ currentCategory }}</div>
          <div
            v-for="item in knowledgeItems"
            :key="item.id"
            class="knowledge-item"
            :title="item.question"
            @click="askItem(item)"
          >
            {{ item.question }}
          </div>
        </div>
        <div v-else-if="currentCategory && !knowledgeLoading" class="knowledge-empty">
          暂无相关知识
        </div>
        <!-- 安全预警提醒 -->
        <div class="safety-tip" @click="showDangerList">
          <el-icon><WarningFilled /></el-icon>
          <span>查看安全预警</span>
          <el-badge :value="dangerCount" :hidden="dangerCount === 0" type="danger" />
        </div>
      </div>

      <!-- 中间对话区 -->
      <div class="chat-main">
        <div class="chat-messages" ref="messagesRef">
          <div v-if="messages.length === 0" class="empty-tip">
            <el-icon :size="48"><ChatDotRound /></el-icon>
            <p>您好！我是AI实验助手</p>
            <p>有什么可以帮您的？</p>
          </div>
          <div v-else v-for="(msg, index) in messages" :key="index" class="message-item" :class="[msg.role, msg.dangerLevel]">
            <!-- 危险等级提示标签 -->
            <div v-if="msg.dangerLevel === 'danger'" class="danger-badge">
              <el-icon><WarningFilled /></el-icon> 紧急危险 - 请立即停止操作
            </div>
            <div v-if="msg.dangerLevel === 'warning'" class="warning-badge">
              <el-icon><Warning /></el-icon> 操作警告 - 请注意安全
            </div>
            <div v-if="msg.dangerLevel === 'info'" class="info-badge">
              <el-icon><InfoFilled /></el-icon> 提示信息
            </div>
            <div class="message-content">
              <span v-if="msg.content.includes('[step_guide_button]')">
                <span v-html="msg.content.replace('[step_guide_button]', '')"></span>
                <el-button type="primary" size="small" @click="openGuide" class="guide-btn">
                  <el-icon><Guide /></el-icon>
                  查看图文引导
                </el-button>
              </span>
              <span v-else v-html="formatMessage(msg.content)"></span>
            </div>
          </div>
        </div>
        <div class="chat-input">
          <el-input
            v-model="inputText"
            placeholder="请输入您的问题..."
            @keyup.enter="sendMessage"
          >
            <template #append>
              <el-button :icon="Promotion" @click="sendMessage" />
            </template>
          </el-input>
        </div>
      </div>

      <!-- 右侧快捷操作 -->
      <div class="quick-reply">
        <div class="quick-title">常见问题</div>
        <div class="quick-list">
          <div class="quick-item" @click="sendQuickQuestion('显微镜如何操作？')">
            显微镜如何操作？
          </div>
          <div class="quick-item" @click="sendQuickQuestion('离心机如何操作？')">
            离心机如何操作？
          </div>
          <div class="quick-item" @click="sendQuickQuestion('如何预约设备？')">
            如何预约设备？
          </div>
          <div class="quick-item" @click="sendQuickQuestion('设备故障如何报修？')">
            设备故障如何报修？
          </div>
          <div class="quick-item" @click="sendQuickQuestion('遇到智能冲突检测冲突怎么办？')">
            遇到智能冲突检测冲突怎么办？
          </div>
        </div>
        <!-- 危险操作快速入口 -->
        <div class="danger-section">
          <div class="danger-title">
            <el-icon><WarningFilled /></el-icon>
            安全警示
          </div>
          <div class="danger-item" @click="sendQuickQuestion('高压设备有什么危险？')">
            <el-icon><Warning /></el-icon>
            高压设备安全
          </div>
          <div class="danger-item" @click="sendQuickQuestion('有毒化学品如何处理？')">
            <el-icon><Warning /></el-icon>
            化学品安全
          </div>
          <div class="danger-item" @click="sendQuickQuestion('设备着火怎么办？')">
            <el-icon><Warning /></el-icon>
            火灾应急
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, onMounted } from 'vue'
import { aiChat, getChatHistory, getCategories, getDangerAlerts, getCategoryList } from '@/api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ChatDotRound, Promotion, Warning, WarningFilled, InfoFilled, CircleCloseFilled, WarnTriangleFilled, View, Guide, Folder, ArrowRight, Document, Tools, Lock, QuestionFilled, Setting } from '@element-plus/icons-vue'
import OperationGuide from '@/components/OperationGuide.vue'

// 分类图标映射
const categoryIcons = [Document, Tools, Lock, WarningFilled, QuestionFilled]

const messagesRef = ref(null)
const inputText = ref('')
const currentCategory = ref('')
const categories = ref(['设备操作手册', '实验流程', '故障排查', '安全规范', '常见问题'])
const knowledgeItems = ref([])
const knowledgeLoading = ref(false)
const sending = ref(false)
const messages = reactive([
  { role: 'assistant', content: '您好！我是AI实验助手，有什么可以帮您的？', dangerLevel: 'info' }
])

// 图文引导相关
const guideVisible = ref(false)
const guideSteps = ref('')
const guideDuration = ref(0)
const currentGuideInfo = ref({})

// 分级预警相关
const dangerDialogVisible = ref(false)
const dangerLevel = ref('info')
const currentDangerQuestion = ref('')
const currentDangerAnswer = ref('')
const dangerCount = ref(0)
const dangerAlerts = ref([])

// 预警配置
const dangerDialogConfig = ref({
  title: '安全提示',
  class: '',
  showClose: true,
  closeOnClick: false,
  closeOnEscape: false,
  type: 'info',
  icon: InfoFilled,
  iconColor: '#409EFF'
})

// 危险等级配置
const dangerConfigs = {
  danger: {
    title: '紧急危险 - 请立即停止操作',
    class: 'danger-dialog',
    showClose: false,
    closeOnClick: false,
    closeOnEscape: false,
    type: 'error',
    icon: CircleCloseFilled,
    iconColor: '#F56C6C'
  },
  warning: {
    title: '操作警告 - 请注意安全',
    class: 'warning-dialog',
    showClose: true,
    closeOnClick: false,
    closeOnEscape: true,
    type: 'warning',
    icon: WarnTriangleFilled,
    iconColor: '#E6A23C'
  },
  info: {
    title: '提示信息',
    class: 'info-dialog',
    showClose: true,
    closeOnClick: true,
    closeOnEscape: true,
    type: 'info',
    icon: InfoFilled,
    iconColor: '#409EFF'
  }
}

const selectCategory = async (category) => {
  currentCategory.value = category
  knowledgeLoading.value = true
  try {
    const res = await getCategoryList(category)
    knowledgeItems.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    knowledgeItems.value = []
  } finally {
    knowledgeLoading.value = false
  }
}

// 显示危险列表
const showDangerList = async () => {
  try {
    const res = await getDangerAlerts()
    dangerAlerts.value = res.data || []
    if (dangerAlerts.value.length > 0) {
      ElMessageBox.alert(
        dangerAlerts.value.map((item, index) => `${index + 1}. ${item.question}\n   答案: ${item.answer.substring(0, 50)}...`).join('\n\n'),
        '安全预警知识库',
        {
          confirmButtonText: '关闭',
          dangerouslyUseHTMLString: true
        }
      )
    } else {
      ElMessage.info('暂无安全预警记录')
    }
  } catch (e) {
    console.error('获取危险列表失败', e)
  }
}

// 处理预警弹窗显示
const handleDangerAlert = (level, question, answer) => {
  dangerLevel.value = level
  currentDangerQuestion.value = question
  currentDangerAnswer.value = answer
  dangerDialogConfig.value = dangerConfigs[level] || dangerConfigs.info

  if (level === 'danger') {
    // 紧急危险 - 强制弹窗，不可关闭
    dangerDialogVisible.value = true
  } else if (level === 'warning') {
    // 警告 - 确认后关闭
    ElMessageBox.confirm(
      `问题: ${question}\n\n答案: ${answer}`,
      '操作警告 - 请注意安全',
      {
        confirmButtonText: '我已了解风险',
        cancelButtonText: '继续提问',
        type: 'warning',
        showClose: false
      }
    ).catch(() => {})
  }
  // info级别不弹窗，直接显示在消息中
}

const sendQuickQuestion = (question) => {
  if (sending.value) return
  inputText.value = question
  sendMessage()
}

const sendMessage = async () => {
  if (!inputText.value.trim() || sending.value) return

  sending.value = true
  const question = inputText.value.trim()
  messages.push({ role: 'user', content: question, dangerLevel: 'info' })
  inputText.value = ''

  try {
    const res = await aiChat({ question, category: currentCategory.value || undefined })
    // 处理回复中的危险等级
    const level = res.dangerLevel || 'info'
    const hasGuide = res.hasGuide || false

    // 保存引导信息用于后续显示
    if (hasGuide) {
      currentGuideInfo.value = {
        guideSteps: res.guideSteps,
        guideDuration: res.guideDuration || 0
      }
    }

    // 构建回复内容
    let replyContent = res.answer
    // 如果有引导，添加引导提示按钮
    if (hasGuide) {
      replyContent += '\n\n[step_guide_button]'
    }

    messages.push({ role: 'assistant', content: replyContent, dangerLevel: level, hasGuide })

    // 如果是危险或警告级别，触发弹窗预警
    if (level === 'danger' || level === 'warning') {
      handleDangerAlert(level, question, res.answer)
    }
  } catch (e) {
    messages.push({ role: 'assistant', content: '抱歉，我暂时无法回答您的问题。', dangerLevel: 'info' })
  } finally {
    sending.value = false
  }

  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 打开图文引导
const openGuide = () => {
  console.log('currentGuideInfo:', currentGuideInfo.value)
  const stepsData = currentGuideInfo.value.guideSteps
  if (stepsData) {
    try {
      // 确保guideSteps是正确的JSON格式
      let parsedSteps = stepsData
      if (typeof stepsData === 'string') {
        parsedSteps = JSON.parse(stepsData)
      }
      // 确保每个步骤有正确的格式
      if (Array.isArray(parsedSteps)) {
        guideSteps.value = parsedSteps.map((s, i) => ({
          step: s.step || (i + 1),
          title: s.title || `步骤 ${i + 1}`,
          content: s.content || '',
          warning: s.warning || '',
          image: s.image ? (s.image.startsWith('/api') ? s.image : '/api' + s.image) : ''
        }))
      } else {
        guideSteps.value = stepsData
      }
      guideDuration.value = currentGuideInfo.value.guideDuration || 0
      guideVisible.value = true
    } catch (e) {
      console.error('解析引导步骤失败:', e)
      ElMessage.error('引导步骤解析失败')
    }
  } else {
    ElMessage.warning('没有可用的引导步骤')
  }
}

// 格式化消息内容
const formatMessage = (content) => {
  if (!content) return ''
  return content.replace(/\n/g, '<br>')
}

const askItem = (item) => {
  inputText.value = item.question || ''
  sendMessage()
}

// 加载危险预警数量
const loadDangerCount = async () => {
  try {
    const res = await getDangerAlerts()
    dangerCount.value = res.data ? res.data.length : 0
  } catch (e) {
    console.error('获取危险预警数量失败', e)
  }
}

onMounted(async () => {
  loadDangerCount()
  await loadChatHistory()
})

// 加载聊天历史
const loadChatHistory = async () => {
  try {
    const res = await getChatHistory(50)
    const history = res.list || []
    if (history.length > 0) {
      messages.length = 0
      history.forEach(item => {
        messages.push({ role: 'user', content: item.question, dangerLevel: 'info' })
        let answerContent = item.answer || ''
        if (answerContent.includes('⚠️ 安全警告')) {
          messages.push({ role: 'assistant', content: answerContent, dangerLevel: 'danger' })
        } else if (answerContent.includes('⚠️ 警告')) {
          messages.push({ role: 'assistant', content: answerContent, dangerLevel: 'warning' })
        } else {
          messages.push({ role: 'assistant', content: answerContent, dangerLevel: 'info' })
        }
      })
      nextTick(() => {
        if (messagesRef.value) {
          messagesRef.value.scrollTop = messagesRef.value.scrollHeight
        }
      })
    }
  } catch (e) {
    console.error('获取聊天历史失败', e)
  }
}
</script>

<style lang="scss" scoped>
.ai-assistant {
  width: 100%;
  height: calc(100vh - 140px);
}

.chat-container {
  display: flex;
  height: 100%;
  gap: 20px;
}

.sidebar {
  width: 200px;
  background: #161B22;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363D;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #E6EDF3;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-item {
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  color: #8B949E;
  margin-bottom: 6px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 12px;
  opacity: 0;
  animation: fadeInLeft 0.4s ease forwards;

  &:hover {
    background: rgba(0, 212, 255, 0.1);
    color: #00D4FF;
    transform: translateX(4px);

    .category-arrow {
      opacity: 1;
      transform: translateX(0);
    }
  }

  &.active {
    background: linear-gradient(135deg, rgba(0, 212, 255, 0.15), rgba(123, 97, 255, 0.15));
    color: #00D4FF;
    border-left: 3px solid #00D4FF;

    .category-icon {
      background: rgba(0, 212, 255, 0.2);
      color: #00D4FF;
    }

    .category-arrow {
      opacity: 1;
      color: #00D4FF;
    }
  }
}

.category-icon {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.05);
  color: #8B949E;
  transition: all 0.3s ease;
  font-size: 14px;

  &.icon-1 { background: rgba(64, 158, 255, 0.15); }
  &.icon-2 { background: rgba(103, 194, 58, 0.15); }
  &.icon-3 { background: rgba(245, 108, 108, 0.15); }
  &.icon-4 { background: rgba(230, 162, 60, 0.15); }
  &.icon-5 { background: rgba(123, 97, 255, 0.15); }
}

.category-text {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
}

.category-arrow {
  font-size: 12px;
  opacity: 0;
  transform: translateX(-4px);
  transition: all 0.3s ease;
}

@keyframes fadeInLeft {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

// 分类知识列表
.knowledge-list {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #30363D;
  max-height: 200px;
  overflow-y: auto;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background: #30363D;
    border-radius: 2px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
}

.knowledge-list-title {
  font-size: 12px;
  color: #8B949E;
  margin-bottom: 8px;
  padding-left: 4px;
}

.knowledge-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  color: #8B949E;
  font-size: 13px;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: all 0.2s ease;

  &:hover {
    background: #1F2937;
    color: #00D4FF;
  }
}

.knowledge-empty {
  margin-top: 12px;
  padding: 8px 4px;
  font-size: 12px;
  color: #484F58;
  text-align: center;
}

.chat-main {
  flex: 1;
  background: #161B22;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363D;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.empty-tip {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8B949E;

  p {
    margin-top: 16px;
    font-size: 16px;
  }
}

.message-item {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;

  &.user {
    align-items: flex-end;

    .message-content {
      background: linear-gradient(90deg, #00D4FF, #7B61FF);
      color: white;
    }
  }

  &.assistant {
    align-items: flex-start;

    .message-content {
      background: #21262D;
      color: #E6EDF3;
    }

    &.danger {
      .message-content {
        background: #3D1C1C;
        border: 1px solid #F56C6C;
        border-left: 3px solid #F56C6C;
      }
    }

    &.warning {
      .message-content {
        background: #3D321C;
        border: 1px solid #E6A23C;
        border-left: 3px solid #E6A23C;
      }
    }
  }
}

.danger-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #F56C6C;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 6px;
}

.warning-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #E6A23C;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 6px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
  white-space: pre-wrap;

  :deep(.guide-btn) {
    margin-top: 12px;
    background: linear-gradient(135deg, #00D4FF, #7B61FF);
    border: none;

    &:hover {
      opacity: 0.9;
    }
  }
}

.chat-input {
  margin-top: 16px;
}

.quick-reply {
  width: 240px;
  background: #161B22;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363D;
}

.quick-title {
  font-size: 16px;
  font-weight: 600;
  color: #E6EDF3;
  margin-bottom: 16px;
}

.quick-item {
  padding: 12px;
  background: #21262D;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  color: #8B949E;
  font-size: 14px;
  transition: all 0.3s ease;

  &:hover {
    background: #1F2937;
    color: #00D4FF;
  }
}

// 安全提示区域
.safety-tip {
  margin-top: 20px;
  padding: 12px;
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid #F56C6C;
  border-radius: 8px;
  color: #F56C6C;
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(245, 108, 108, 0.2);
  }
}

// 危险操作快速入口
.danger-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #30363D;
}

.danger-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #F56C6C;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.danger-item {
  padding: 10px;
  background: rgba(245, 108, 108, 0.1);
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  color: #F56C6C;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(245, 108, 108, 0.2);
  }
}

// 消息标签样式
.info-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: #409EFF;
  color: white;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 6px;
}

// 预警弹窗样式
.danger-dialog-content {
  text-align: center;
}

.danger-icon-wrapper {
  margin-bottom: 16px;
}

.danger-question {
  background: #3D1C1C;
  padding: 12px;
  border-radius: 8px;
  color: #F56C6C;
  margin-bottom: 12px;
  text-align: left;
  font-weight: 600;
}

.danger-message {
  color: #E6EDF3;
  line-height: 1.6;
  text-align: left;
}

.correct-operation {
  margin-top: 16px;
  background: rgba(245, 108, 108, 0.1);
  border: 1px solid #F56C6C;
  border-radius: 8px;
  padding: 12px;
  text-align: left;
}

.correct-title {
  color: #F56C6C;
  font-weight: bold;
  margin-bottom: 8px;
}

.correct-content {
  color: #8B949E;
  line-height: 1.8;
}

// 弹窗样式覆盖
:deep(.danger-dialog) {
  .el-dialog {
    border: 2px solid #F56C6C;
    background: #161B22;
  }
  .el-dialog__header {
    background: #3D1C1C;
    border-bottom: 1px solid #F56C6C;
  }
  .el-dialog__title {
    color: #F56C6C;
    font-weight: bold;
  }
}

:deep(.warning-dialog) {
  .el-dialog {
    border: 2px solid #E6A23C;
    background: #161B22;
  }
  .el-dialog__header {
    background: #3D321C;
    border-bottom: 1px solid #E6A23C;
  }
  .el-dialog__title {
    color: #E6A23C;
    font-weight: bold;
  }
}

:deep(.info-dialog) {
  .el-dialog {
    background: #161B22;
  }
  .el-dialog__header {
    background: #21262D;
  }
  .el-dialog__title {
    color: #409EFF;
  }
}

.dialog-footer {
  text-align: center;
}
</style>
