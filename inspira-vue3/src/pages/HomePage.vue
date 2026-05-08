<script lang="ts" setup>
import { computed, ref } from 'vue';
import FlickeringGrid from '../components/FlickeringGrid.vue';
import SiteHeader from '../components/SiteHeader.vue';
import ClientOnly from '../components/ClientOnly.vue';
import BlurReveal from '../components/ui/BlurReveal.vue';
import ProgressiveBlur from '../components/ui/ProgressiveBlur.vue';
import StaggerBlurWords from '../components/ui/StaggerBlurWords.vue';
import CircularGallery from '../components/ui/CircularGallery.vue';
import type { GalleryCardPickPayload } from '../components/ui/CircularGallery.vue';
import FlipCard from '../components/ui/FlipCard.vue';
import HomeProvinceMap from '@/components/home/HomeProvinceMap.vue';
import { HOME_GALLERY_ITEMS, cityNameToGalleryGroup } from '@/data/homeGalleryConfig';

/** 与 StaggerBlurWords 的 word-stagger-ms 保持一致 */
const WORD_STAGGER_MS = 200;
/** 上一行最后一个词开始显现后，再等多久启动下一行 */
const BETWEEN_LINES_MS = 500;
/** 首行相对挂载的延迟（接在 Hey there 模糊入场之后） */
const FIRST_LINE_DELAY_MS = 1100;

const subtitleLines = [
  '“同源” —— 基于 多智能体(Multi-Agent) ',
  '与 AIGC 的 非遗 文化 数字生命 共创平台',
  '点击 地图中的 城市 可查看 该城市 的 非遗 数字生命',
  '每个 数字生命 都有 独特的 文化 背景 和 故事',
  '点击 下方卡片 来了解 详情 或与 数字生命 互动',
 
] as const;

const staggeredSubtitleLines = computed(() => {
  let t = FIRST_LINE_DELAY_MS;
  return subtitleLines.map((words) => {
    const start = t;
    const count = words.split(/\s+/).filter(Boolean).length;
    const n = Math.max(1, count);
    t = start + (n - 1) * WORD_STAGGER_MS + BETWEEN_LINES_MS;
    return { words, delay: start };
  });
});

/** 地图点击市 → 底部画廊组 0–7 */
const galleryFocusGroup = ref<number | null>(null);

function onMapRegionClick(name: string) {
  galleryFocusGroup.value = cityNameToGalleryGroup(name);
}

const selectedGalleryCard = ref(HOME_GALLERY_ITEMS[0] ?? {
  image: '',
  text: '',
  title: '同源',
  description: '点击下方卡片以查看对应非遗图像与说明。',
});

function onGalleryCardPick(payload: GalleryCardPickPayload) {
  selectedGalleryCard.value = {
    ...selectedGalleryCard.value,
    image: payload.image,
    title: payload.title,
    description: payload.description,
  };
}

</script>

<template>
  <main class="relative min-h-screen overflow-hidden bg-slate-950 text-white">
    <FlickeringGrid
      class="absolute inset-0 z-0 [mask-image:radial-gradient(ellipse_at_center,white,transparent_80%)]"
      :square-size="5"
      :grid-gap="7"
      color="#a78bfa"
      :max-opacity="0.7"
      :flicker-chance="0.2"
    />

    <SiteHeader />

    <ClientOnly>
      <div
        class="pointer-events-auto fixed right-[calc(100vw/5*0.1)] top-1/2 z-[26] flex h-[50vh] w-1/5 min-h-0 min-w-0 -translate-y-1/2 items-center justify-center"
      >
        <div class="flex h-full w-full min-h-0 min-w-0 items-center justify-center">
          <FlipCard class="h-full w-full min-h-0 shadow-xl shadow-violet-900/25 ring-1 ring-white/10">
            <template #default>
              <img
                :src="selectedGalleryCard.image"
                :alt="selectedGalleryCard.title"
                class="size-full rounded-2xl object-cover shadow-2xl shadow-black/40"
              />
              <ProgressiveBlur
                class="absolute inset-x-0 bottom-0 h-[16.6667%] rounded-b-2xl bg-black/30"
                direction="bottom"
                :blur-layers="12"
                :blur-intensity="0.75"
              />
              <div class="absolute bottom-4 left-4 text-xl font-bold text-white">
                {{ selectedGalleryCard.title }}
              </div>
            </template>
            <template #back>
              <div class="flex min-h-full flex-col gap-2">
                <h1 class="text-xl font-bold text-white">{{ selectedGalleryCard.title }}</h1>
                <p
                  class="mt-1 border-t border-t-gray-200 py-4 text-base leading-normal font-medium text-gray-100"
                >
                  {{ selectedGalleryCard.description }}
                </p>
              </div>
            </template>
          </FlipCard>
        </div>
      </div>
    </ClientOnly>

    <!-- 地图：置于内容层之下，避免遮挡底部 WebGL 画廊（卡片标题会伸入中部视区） -->
    <div class="pointer-events-none fixed inset-0 z-[5] flex items-center justify-center">
      <ClientOnly>
        <div class="pointer-events-auto h-[70vh] w-1/2 min-h-0 min-w-0">
          <HomeProvinceMap @region-click="onMapRegionClick" />
        </div>
      </ClientOnly>
    </div>

    <section
      class="relative z-10 min-h-screen px-6 pt-24 pb-[28vh] pointer-events-none"
    >
      <ClientOnly>
        <BlurReveal
          :delay="0.2"
          :duration="0.75"
          class="pointer-events-none p-8"
        >
          <h2 class="text-3xl font-bold tracking-tighter sm:text-5xl xl:text-6xl/none">同源 👋</h2>
          <span class="text-xl tracking-tighter text-pretty sm:text-3xl xl:text-4xl/none">
            Omni Source
          </span>
        </BlurReveal>
        <div class="pointer-events-none mt-10 space-y-2 px-8 sm:mt-12 sm:space-y-2.5">
          <StaggerBlurWords
            v-for="(line, i) in staggeredSubtitleLines"
            :key="i"
            :words="line.words"
            :delay="line.delay"
            :duration="0.7"
            :word-stagger-ms="WORD_STAGGER_MS"
            class="block text-base text-white/75 sm:text-lg"
          />
        </div>
      </ClientOnly>
    </section>

    <section class="pointer-events-auto absolute inset-x-0 bottom-0 z-[25] h-[25vh]">
      <ClientOnly>
        <CircularGallery
          :items="HOME_GALLERY_ITEMS"
          :focus-group-index="galleryFocusGroup"
          @card-pick="onGalleryCardPick"
        />
      </ClientOnly>
    </section>
  </main>
</template>
