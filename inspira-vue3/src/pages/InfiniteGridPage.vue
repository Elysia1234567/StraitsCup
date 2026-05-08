<script setup lang="ts">
import { useFullscreen } from "@vueuse/core";
import { ref, useTemplateRef } from "vue";
import { useRouter } from "vue-router";
import InfiniteGrid from "../components/ui/infinite-grid/infiniteGrid.vue";
import AnimatedActionButton from "../components/ui/AnimatedActionButton.vue";
import type { CardData } from "../components/ui/infinite-grid/types";
import { HOME_GALLERY_ITEMS } from "../data/homeGalleryConfig";

const el = useTemplateRef<HTMLElement>("el");
const { toggle } = useFullscreen(el);
const tilesLoaded = ref(false);
const router = useRouter();

const cardData: CardData[] = HOME_GALLERY_ITEMS.map((item) => {
  const [city] = item.text.split(" · ");
  return {
    title: item.title,
    badge: city ?? "非遗",
    image: item.image,
    description: item.description,
    tags: ["非遗", city ?? "福建", "数字文化"],
    date: "2026",
  };
});

function onTilesLoaded() {
  tilesLoaded.value = true;
}

function onTileClicked() {
  router.push("/data");
}
</script>

<template>
  <main class="relative min-h-screen overflow-hidden bg-[#05070f] text-white">
    <section
      ref="el"
      class="relative min-h-screen"
    >
      <InfiniteGrid
        :card-data="cardData"
        @tiles-loaded="onTilesLoaded"
        @tile-clicked="onTileClicked"
      />

      <AnimatedActionButton
        v-if="tilesLoaded"
        text="View Fullscreen"
        class="absolute bottom-10 left-1/2 z-20 -translate-x-1/2 border-white/20 bg-white/10 text-sm text-white/85 transition hover:bg-white/20"
        @click="toggle"
      />
      <span
        v-else
        class="absolute bottom-10 left-1/2 z-20 -translate-x-1/2 text-sm font-semibold text-white/80"
      >
        Loading Tiles
      </span>
    </section>
  </main>
</template>
