<script setup>
import { RouterLink, useRoute } from 'vue-router';

const route = useRoute();

const navItemClass = (path) =>
  route.path === path ? 'text-white' : 'text-white/70 transition hover:text-white';
</script>

<template>
  <header class="fixed inset-x-0 top-0 z-40 h-16 border-b border-white/10 bg-[rgba(2,8,20,0.74)] backdrop-blur-xl">
    <nav class="grid h-full w-full grid-cols-[1fr_auto_1fr] items-center px-6">
      <RouterLink
        class="flex h-10 w-10 items-center justify-center rounded-full ring-1 ring-white/15 transition hover:bg-white/10 hover:ring-white/25"
        to="/"
        aria-label="Omni Source home"
      >
        <img class="h-8 w-8" src="/logo.svg" alt="Omni Source strait logo" />
      </RouterLink>

      <div class="flex items-center gap-12 text-sm sm:gap-16">
        <RouterLink :class="navItemClass('/')" to="/">
          Home
        </RouterLink>
        <RouterLink :class="navItemClass('/agent')" to="/agent">
          Agent
        </RouterLink>
        <RouterLink :class="navItemClass('/infinite-grid')" to="/infinite-grid">
          Models
        </RouterLink>
        <RouterLink :class="navItemClass('/graph')" to="/graph">
          Graph
        </RouterLink>
      </div>

      <div class="flex items-center justify-end gap-2.5">
        <a
          class="inline-flex h-9 items-center rounded-md border border-white/15 bg-white/[0.03] px-3 text-xs text-white/80 transition hover:bg-white/[0.08]"
          href="https://github.com/Elysia1234567/StraitsCup"
          target="_blank"
          rel="noreferrer"
        >
          GitHub
        </a>
      </div>
    </nav>
  </header>
</template>
