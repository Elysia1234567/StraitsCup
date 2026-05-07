<script setup>
import { RouterLink, useRoute } from 'vue-router';

const route = useRoute();

const navItemClass = (path) =>
  route.path === path ? 'text-white' : 'text-white/70 transition hover:text-white';
</script>

<template>
  <header class="absolute inset-x-0 top-0 z-30 h-16 border-b border-white/[0.08] bg-transparent backdrop-blur-md">
    <nav class="flex h-full w-full items-center px-[10px]">
      <RouterLink
        class="inline-flex items-center gap-7 text-xl font-semibold text-white/95"
        to="/"
      >
        <img
          class="h-[50px] w-[50px] rounded-full object-cover"
          src="https://fastly.picsum.photos/id/596/200/200.jpg?hmac=TiMsstBNF6YKq9Gn7QGsihITEUcv_O8QuTXEVR3T6GA"
          alt="brand avatar"
        />
        同源
      </RouterLink>

      <div class="ml-[200px] flex items-center gap-[80px] text-sm">
        <RouterLink :class="navItemClass('/')" to="/">
          Home
        </RouterLink>
        <RouterLink :class="navItemClass('/agent')" to="/agent">
          Agent
        </RouterLink>
        <RouterLink :class="navItemClass('/infinite-grid')" to="/infinite-grid">
          Models
        </RouterLink>
      </div>

      <div class="ml-auto flex items-center gap-2.5">
        <button
          class="inline-flex h-9 items-center gap-2 rounded-md border border-white/15 bg-white/[0.03] px-3 text-xs text-white/70"
          type="button"
        >
          <span class="text-white/50">Search...</span>
          <kbd class="rounded border border-white/20 px-1.5 py-0.5 text-[10px] text-white/60">K</kbd>
        </button>
        <a
          class="inline-flex h-9 items-center rounded-md border border-white/15 bg-white/[0.03] px-3 text-xs text-white/80 transition hover:bg-white/[0.08]"
          href="https://github.com/unovue/inspira-ui"
          target="_blank"
          rel="noreferrer"
        >
          GitHub
        </a>
      </div>
    </nav>
  </header>
</template>
