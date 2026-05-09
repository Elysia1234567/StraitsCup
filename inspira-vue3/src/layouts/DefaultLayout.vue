<script setup>
import SiteHeader from '@/components/SiteHeader.vue';
</script>

<template>
  <div class="default-layout flex min-h-screen min-h-[100dvh] flex-col">
    <SiteHeader />
    <RouterView class="flex min-h-0 flex-1 flex-col" />
  </div>
</template>
