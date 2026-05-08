<template>
  <div
    class="cursor-pointer rounded-xl border p-4 text-white transition hover:shadow-md"
    :class="
      selectable
        ? selected
          ? 'border-violet-400/80 bg-violet-500/20 ring-1 ring-violet-400/50'
          : 'border-white/20 bg-transparent hover:bg-white/10'
        : 'border-white/20 bg-transparent hover:bg-white/10'
    "
    role="button"
    tabindex="0"
    @click="onClick"
    @keydown.enter.prevent="onClick"
  >
    <div class="flex items-start gap-3">
      <img :src="agent.avatar" :alt="agent.name" class="h-12 w-12 rounded-full object-cover ring-1 ring-white/15" />
      <div class="min-w-0 flex-1">
        <div class="mb-1 flex items-center gap-2">
          <h3 class="truncate font-semibold text-white">{{ agent.name }}</h3>
          <span v-if="agent.official" class="rounded bg-white/15 px-1.5 py-0.5 text-xs text-white">官方</span>
          <span v-if="agent.new" class="rounded bg-white/15 px-1.5 py-0.5 text-xs text-white">NEW</span>
          <span
            v-if="selectable && selected"
            class="ml-auto rounded bg-violet-500/40 px-2 py-0.5 text-xs text-violet-100"
          >已选</span>
        </div>
        <p class="mb-2 line-clamp-2 text-sm text-white/80">{{ agent.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  agent: {
    type: Object,
    required: true,
  },
  selectable: {
    type: Boolean,
    default: false,
  },
  selected: {
    type: Boolean,
    default: false,
  },
});

const emit = defineEmits(['toggle']);

function onClick() {
  if (props.selectable) {
    emit('toggle', props.agent);
  }
}
</script>
