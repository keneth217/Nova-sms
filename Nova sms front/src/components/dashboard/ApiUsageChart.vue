<script setup lang="ts">
import { computed } from 'vue'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js'
import { Line, Bar } from 'vue-chartjs'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
  Filler,
)

export interface ApiUsageDataset {
  label: string
  data: number[]
  borderColor?: string
  backgroundColor?: string
  fill?: boolean
}

const props = withDefaults(
  defineProps<{
    type?: 'line' | 'bar'
    labels: string[]
    datasets: ApiUsageDataset[]
    heightClass?: string
  }>(),
  { type: 'line', heightClass: 'h-64' },
)

const chartData = computed(() => ({
  labels: props.labels,
  datasets: props.datasets.map((dataset) => ({
    label: dataset.label,
    data: dataset.data,
    borderColor: dataset.borderColor ?? '#0d9488',
    backgroundColor: dataset.backgroundColor ?? 'rgba(13, 148, 136, 0.12)',
    fill: dataset.fill ?? props.type === 'line',
    tension: 0.35,
    pointRadius: 0,
    borderWidth: 2,
    borderRadius: 6,
    maxBarThickness: 36,
  })),
}))

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top' as const,
      align: 'end' as const,
      labels: { boxWidth: 10, usePointStyle: true, pointStyle: 'circle' as const },
    },
  },
  scales: {
    x: {
      grid: { display: false },
      ticks: { color: '#64748b', font: { size: 11 } },
      border: { display: false },
    },
    y: {
      beginAtZero: true,
      grid: { color: '#f1f5f9' },
      ticks: { color: '#64748b', font: { size: 11 } },
      border: { display: false },
    },
  },
}
</script>

<template>
  <div :class="['w-full', heightClass]">
    <Line v-if="type === 'line'" :data="chartData" :options="options" />
    <Bar v-else :data="chartData" :options="options" />
  </div>
</template>
