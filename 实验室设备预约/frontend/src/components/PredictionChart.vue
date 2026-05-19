<template>
  <div class="prediction-chart-container">
    <div class="chart-header">
      <span class="chart-title">{{ title }}</span>
      <div class="chart-actions">
        <el-select v-model="predictDays" size="small" @change="loadData">
          <el-option :value="7" label="预测7天" />
          <el-option :value="14" label="预测14天" />
          <el-option :value="30" label="预测30天" />
        </el-select>
      </div>
    </div>
    <div ref="chartRef" class="chart-container"></div>
    <div class="chart-legend">
      <div class="legend-item">
        <span class="legend-color history"></span>
        <span class="legend-text">历史数据</span>
      </div>
      <div class="legend-item">
        <span class="legend-color prediction"></span>
        <span class="legend-text">预测值</span>
      </div>
      <div class="legend-item">
        <span class="legend-color confidence"></span>
        <span class="legend-text">置信区间</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getBookingPrediction } from '@/api/statistics'

const props = defineProps({
  title: {
    type: String,
    default: '预约趋势预测'
  }
})

const chartRef = ref(null)
const predictDays = ref(7)
let chartInstance = null

const loadData = async () => {
  try {
    const raw = await getBookingPrediction({
      historyDays: 30,
      predictDays: predictDays.value
    })
    const res = raw && typeof raw === 'object' && raw.data !== undefined && raw.success !== false
      ? raw.data
      : raw

    if (res && res.enabled === false) {
      console.warn('预测功能未启用或计算失败', res)
      return
    }
    await nextTick()
    renderChart((res && res.history) || [], (res && res.forecast) || [])
    requestAnimationFrame(() => {
      chartInstance?.resize()
      setTimeout(() => chartInstance?.resize(), 200)
    })
  } catch (e) {
    console.error('加载预测数据失败', e)
  }
}

const renderChart = (history, forecast) => {
  if (!chartRef.value) return

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const dates = []
  const historyData = []
  const predictionData = []
  const upperData = []
  const lowerData = []

  // 历史数据
  history.forEach(item => {
    dates.push(item.date)
    historyData.push(item.value)
    predictionData.push('-')
    upperData.push('-')
    lowerData.push('-')
  })

  // 预测数据
  forecast.forEach((item, index) => {
    // 在历史数据最后一个点后添加预测起点
    if (index === 0 && history.length > 0) {
      const lastHistory = history[history.length - 1]
      dates.push(lastHistory.date)
      historyData.push(lastHistory.value)
      predictionData.push('-')
      upperData.push('-')
      lowerData.push('-')
    }

    dates.push(item.date)
    historyData.push('-')
    predictionData.push(item.value)
    upperData.push(item.upper)
    lowerData.push(item.lower)
  })

  const option = {
    backgroundColor: 'transparent',
    grid: {
      top: 40,
      right: 30,
      bottom: 60,
      left: 50
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#21262D',
      borderColor: '#30363D',
      textStyle: {
        color: '#E6EDF3'
      },
      formatter: function(params) {
        let result = params[0].axisValue + '<br/>'
        params.forEach(param => {
          if (param.value !== '-' && param.value !== undefined) {
            const color = param.seriesName === '历史数据' ? '#00D4FF' :
                         param.seriesName === '预测值' ? '#7B61FF' : '#4ADE80'
            result += `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${color};"></span>`
            result += `${param.seriesName}: ${param.value}<br/>`
          }
        })
        return result
      }
    },
    legend: {
      show: false
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: {
        lineStyle: {
          color: '#30363D'
        }
      },
      axisLabel: {
        color: '#8B949E',
        rotate: 45
      }
    },
    yAxis: {
      type: 'value',
      name: '预约数',
      axisLine: {
        lineStyle: {
          color: '#30363D'
        }
      },
      axisLabel: {
        color: '#8B949E'
      },
      splitLine: {
        lineStyle: {
          color: '#21262D'
        }
      }
    },
    series: [
      {
        name: '历史数据',
        type: 'line',
        data: historyData,
        smooth: true,
        lineStyle: {
          color: '#00D4FF',
          width: 3
        },
        itemStyle: {
          color: '#00D4FF'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0)' }
          ])
        },
        connectNulls: false
      },
      {
        name: '预测值',
        type: 'line',
        data: predictionData,
        smooth: true,
        lineStyle: {
          color: '#7B61FF',
          width: 3,
          type: 'dashed'
        },
        itemStyle: {
          color: '#7B61FF'
        },
        symbol: 'circle',
        symbolSize: 8,
        connectNulls: false
      },
      {
        name: '置信区间',
        type: 'line',
        data: upperData,
        smooth: true,
        lineStyle: {
          color: '#4ADE80',
          width: 1,
          type: 'dotted',
          opacity: 0.5
        },
        areaStyle: {
          color: 'transparent'
        },
        symbol: 'none',
        connectNulls: false
      },
      {
        name: '下界',
        type: 'line',
        data: lowerData,
        smooth: true,
        lineStyle: {
          color: '#4ADE80',
          width: 1,
          type: 'dotted',
          opacity: 0.5
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(74, 222, 128, 0.1)' },
            { offset: 1, color: 'rgba(74, 222, 128, 0)' }
          ])
        },
        symbol: 'none',
        connectNulls: false
      }
    ]
  }

  chartInstance.setOption(option)
  nextTick(() => {
    chartInstance?.resize()
  })
}

// 窗口调整
const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  nextTick(() => {
    loadData()
  })
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})

watch(() => props.title, () => {
  loadData()
})
</script>

<style lang="scss" scoped>
.prediction-chart-container {
  background: #161B22;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #30363D;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #E6EDF3;
}

.chart-container {
  height: 300px;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-color {
  width: 20px;
  height: 4px;
  border-radius: 2px;

  &.history {
    background: #00D4FF;
  }

  &.prediction {
    background: #7B61FF;
  }

  &.confidence {
    background: #4ADE80;
  }
}

.legend-text {
  color: #8B949E;
  font-size: 13px;
}
</style>
