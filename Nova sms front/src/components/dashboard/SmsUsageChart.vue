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
import type { DailyVolumePoint, MonthlyUsagePoint } from '@/models/report.model'

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

const props = defineProps<{
  type: 'line' | 'bar'
  daily?: DailyVolumePoint[]
  monthly?: MonthlyUsagePoint[]
}>()

const chartData = computed(() => {
  if (props.type === 'line' && props.daily) {
    return {
      labels: props.daily.map((d) => d.date.slice(5)),
      datasets: [
        {
          label: 'Sent',
          data: props.daily.map((d) => d.sent),
          borderColor: '#0d9488',
          backgroundColor: 'rgba(13, 148, 136, 0.12)',
          fill: true,
          tension: 0.35,
          pointRadius: 0,
          borderWidth: 2,
        },
        {
          label: 'Delivered',
          data: props.daily.map((d) => d.delivered),
          borderColor: '#0284c7',
          backgroundColor: 'transparent',
          tension: 0.35,
          pointRadius: 0,
          borderWidth: 2,
        },
      ],
    }
  }

  return {
    labels: (props.monthly ?? []).map((m) => m.month),
    datasets: [
      {
        label: 'Volume',
        data: (props.monthly ?? []).map((m) => m.volume),
        backgroundColor: '#0d9488',
        borderRadius: 6,
        maxBarThickness: 36,
      },
    ],
  }
})

const options = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: props.type === 'line',
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
      grid: { color: '#f1f5f9' },
      ticks: { color: '#64748b', font: { size: 11 } },
      border: { display: false },
    },
  },
}
</script>

<template>
  <div class="h-64 w-full">
    <Line v-if="type === 'line'" :data="chartData" :options="options" />
    <Bar v-else :data="chartData" :options="options" />
  </div>
</template>
