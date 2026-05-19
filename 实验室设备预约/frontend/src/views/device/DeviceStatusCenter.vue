<template>
  <div class="device-status-center">
    <div class="page-card page-header-card">
      <div class="header-left">
        <h2>设备状态中心</h2>
      </div>
    </div>

    <el-tabs v-model="activeTab" class="status-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="实时监控" name="monitor">
        <DeviceMonitorTab />
      </el-tab-pane>
      <el-tab-pane label="变更历史" name="track">
        <DeviceTrackTab />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import DeviceMonitorTab from './DeviceStatus.vue'
import DeviceTrackTab from './DeviceStatusTrack.vue'

const route = useRoute()
const activeTab = ref('monitor')

function syncTabFromRoute() {
  // 统一默认显示实时监控，用户可手动切换到变更历史
  const tab = route.query.tab
  if (tab === 'track' || tab === 'monitor') {
    activeTab.value = tab
    return
  }
  activeTab.value = 'monitor'
}

onMounted(syncTabFromRoute)
watch(() => [route.name, route.path, route.query.tab], syncTabFromRoute)

const handleTabChange = (tab) => {
  // 同步 URL query（可选，方便刷新后保持 Tab）
  if (tab !== activeTab.value) {
    activeTab.value = tab
  }
}
</script>

<style scoped lang="scss">
.device-status-center {
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
  h2 {
    margin: 0;
    font-size: 18px;
    font-weight: 700;
    color: #E6EDF3;
  }
  .header-sub {
    font-size: 12px;
    color: #8B949E;
  }
}

// Tabs 样式覆盖
.status-tabs {
  :deep(.el-tabs__header) {
    background: #161B22;
    border: 1px solid #30363D;
    border-radius: 10px 10px 0 0;
    margin-bottom: 0;
    padding: 0 20px;
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }

  :deep(.el-tabs__item) {
    color: #8B949E;
    font-size: 14px;
    font-weight: 500;
    height: 48px;
    line-height: 48px;
    padding: 0 20px;
    transition: color 0.2s;

    &:hover {
      color: #E6EDF3;
    }

    &.is-active {
      color: #00D4FF;
      font-weight: 600;
    }
  }

  :deep(.el-tabs__active-bar) {
    background-color: #00D4FF;
    height: 3px;
    border-radius: 2px;
  }

  :deep(.el-tabs__content) {
    background: #161B22;
    border: 1px solid #30363D;
    border-top: none;
    border-radius: 0 0 10px 10px;
    padding: 20px;
  }
}
</style>
