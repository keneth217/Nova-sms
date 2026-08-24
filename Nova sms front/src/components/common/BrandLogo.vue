<script setup lang="ts">
withDefaults(
  defineProps<{
    to?: string
    heightClass?: string
  }>(),
  {
    to: '/',
    heightClass: 'h-10',
  },
)
</script>

<template>
  <RouterLink :to="to" class="inline-flex items-center">
    <img
      src="/novasmslogo.png"
      alt="Nova SMS"
      :class="['w-auto', heightClass]"
    />
  </RouterLink>
</template>
