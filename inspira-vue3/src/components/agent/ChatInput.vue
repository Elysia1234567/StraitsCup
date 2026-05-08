<template>
  <div class="mx-auto w-full max-w-3xl">
    <div class="relative rounded-2xl border border-white/25 bg-transparent text-white shadow-sm backdrop-blur-sm">
      <textarea
        ref="textareaRef"
        v-model="inputText"
        rows="1"
        class="block w-full resize-none overflow-hidden bg-transparent px-3 pb-9 pt-2.5 text-base leading-6 text-white outline-none placeholder:text-white/60 disabled:opacity-50"
        :placeholder="placeholder"
        :disabled="disabled"
        @input="adjustTextareaHeight"
        @keydown.enter.prevent="handleSend"
      />

      <div class="absolute bottom-0 left-0 right-0 flex items-center justify-between border-t border-white/15 px-1.5 py-0.5">
        <div class="flex min-w-0 flex-1 items-center gap-1 overflow-x-auto">
          <button
            class="flex shrink-0 items-center gap-0.5 rounded-md px-1.5 py-0.5 transition hover:bg-white/10"
            :class="{ 'bg-white/15 ring-1 ring-white/20': searchEnabled }"
            type="button"
            :disabled="disabled"
            @click="searchEnabled = !searchEnabled"
          >
            <MagnifyingGlassIcon class="h-3.5 w-3.5 text-white/80" />
            <span class="text-xs text-white/80">联网搜索</span>
          </button>

          <select
            v-model="voiceDialect"
            class="h-6 shrink-0 rounded-md border border-white/15 bg-slate-950/80 px-1.5 text-xs text-white/80 outline-none transition hover:bg-white/10 disabled:opacity-50"
            :disabled="disabled || voiceRecording || voiceConnecting"
            title="语音识别语言"
          >
            <option value="mandarin">普通话</option>
            <option value="minnan">闽南语</option>
          </select>

          <span v-if="voiceStatusText" class="shrink-0 text-xs text-white/55">
            {{ voiceStatusText }}
          </span>
        </div>

        <div class="flex shrink-0 items-center gap-1 pl-1">
          <button
            class="rounded-md p-1 text-white transition hover:bg-white/10 disabled:opacity-50"
            :class="voiceRecording ? 'bg-rose-500/25 text-rose-100 ring-1 ring-rose-300/40' : 'text-white/80'"
            type="button"
            :disabled="disabled || voiceConnecting"
            :title="voiceRecording ? '停止语音输入' : '语音输入'"
            @click="toggleVoiceInput"
          >
            <StopIcon v-if="voiceRecording" class="h-4 w-4" />
            <MicrophoneIcon v-else class="h-4 w-4" />
          </button>
          <button
            class="rounded-md bg-primary p-1 text-white transition hover:bg-primary/90 disabled:opacity-50"
            type="button"
            :disabled="disabled || !inputText.trim()"
            @click="handleSend"
          >
            <ArrowUpIcon class="h-4 w-4" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { ArrowUpIcon, MagnifyingGlassIcon, MicrophoneIcon, StopIcon } from '@heroicons/vue/24/outline';
import { buildVoiceWebSocketUrl } from '@/api/chatApi.js';

const inputText = ref('');
const searchEnabled = ref(false);
const textareaRef = ref(null);
const voiceSocket = ref(null);
const voiceAudioContext = ref(null);
const voiceStream = ref(null);
const voiceProcessor = ref(null);
const voiceSource = ref(null);
const voiceRecording = ref(false);
const voiceConnecting = ref(false);
const voiceError = ref('');
const voiceDialect = ref('mandarin');

const MAX_TEXT_LINES = 4;

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false,
  },
  placeholder: {
    type: String,
    default: '向 Agent 提问',
  },
});

const emit = defineEmits(['send']);

const voiceDialectLabel = computed(() => (voiceDialect.value === 'minnan' ? '闽南语' : '普通话'));

const voiceStatusText = computed(() => {
  if (voiceError.value) return voiceError.value;
  if (voiceConnecting.value) return '语音连接中';
  if (voiceRecording.value) return `正在听 ${voiceDialectLabel.value}`;
  return '';
});

function adjustTextareaHeight() {
  const el = textareaRef.value;
  if (!el) return;
  const cs = getComputedStyle(el);
  const lineHeight = parseFloat(cs.lineHeight) || 24;
  const padTop = parseFloat(cs.paddingTop) || 0;
  const padBottom = parseFloat(cs.paddingBottom) || 0;
  const minH = padTop + padBottom + lineHeight;
  const maxH = padTop + padBottom + lineHeight * MAX_TEXT_LINES;

  el.style.height = '0px';
  el.style.height = 'auto';
  const scrollH = el.scrollHeight;
  const next = Math.min(Math.max(scrollH, minH), maxH);
  el.style.height = `${next}px`;
  el.style.overflowY = scrollH > maxH ? 'auto' : 'hidden';
}

function handleSend() {
  if (props.disabled || !inputText.value.trim()) return;
  stopVoiceInput();
  emit('send', {
    text: inputText.value.trim(),
    searchEnabled: searchEnabled.value,
  });
  inputText.value = '';
  nextTick(() => adjustTextareaHeight());
}

async function toggleVoiceInput() {
  if (voiceRecording.value) {
    stopVoiceInput();
    return;
  }
  await startVoiceInput();
}

async function startVoiceInput() {
  if (voiceConnecting.value || voiceRecording.value) return;
  voiceError.value = '';
  voiceConnecting.value = true;
  try {
    const socket = new WebSocket(buildVoiceWebSocketUrl(voiceDialect.value));
    socket.binaryType = 'arraybuffer';
    voiceSocket.value = socket;
    socket.onmessage = (event) => handleVoiceMessage(event.data);
    socket.onerror = () => {
      voiceError.value = '语音连接失败';
    };
    socket.onclose = () => {
      voiceRecording.value = false;
    };
    await waitForOpen(socket);
    await startAudioPipeline(socket);
    voiceRecording.value = true;
  } catch (e) {
    voiceError.value = e instanceof Error ? e.message : String(e);
    stopVoiceInput();
  } finally {
    voiceConnecting.value = false;
  }
}

function stopVoiceInput() {
  if (voiceSocket.value?.readyState === WebSocket.OPEN) {
    voiceSocket.value.send(JSON.stringify({ type: 'stop' }));
    voiceSocket.value.close();
  }
  voiceSocket.value = null;
  voiceProcessor.value?.disconnect();
  voiceSource.value?.disconnect();
  voiceAudioContext.value?.close();
  voiceStream.value?.getTracks().forEach((track) => track.stop());
  voiceProcessor.value = null;
  voiceSource.value = null;
  voiceAudioContext.value = null;
  voiceStream.value = null;
  voiceRecording.value = false;
  voiceConnecting.value = false;
}

async function startAudioPipeline(socket) {
  const stream = await navigator.mediaDevices.getUserMedia({
    audio: {
      echoCancellation: true,
      noiseSuppression: true,
      autoGainControl: true,
    },
  });
  const context = new AudioContext();
  const source = context.createMediaStreamSource(stream);
  const processor = context.createScriptProcessor(4096, 1, 1);
  processor.onaudioprocess = (event) => {
    if (socket.readyState !== WebSocket.OPEN) return;
    const input = event.inputBuffer.getChannelData(0);
    const pcm = floatTo16BitPcm(downsample(input, context.sampleRate, 16000));
    if (pcm.byteLength > 0) {
      socket.send(pcm);
    }
  };
  source.connect(processor);
  processor.connect(context.destination);
  voiceStream.value = stream;
  voiceAudioContext.value = context;
  voiceSource.value = source;
  voiceProcessor.value = processor;
}

function handleVoiceMessage(raw) {
  try {
    const payload = JSON.parse(raw);
    if (payload.type === 'error') {
      voiceError.value = payload.data?.message || '语音识别失败';
      return;
    }
    if (payload.type !== 'transcript') return;
    const text = extractTranscript(payload.data?.text ?? payload.data?.result);
    if (!text) return;
    inputText.value = text;
    nextTick(() => adjustTextareaHeight());
  } catch {
    // Ignore non-JSON messages.
  }
}

function extractTranscript(result) {
  if (!result) return '';
  if (typeof result === 'string') return result;
  return result.sentence?.text
    || result.output?.sentence?.text
    || result.output?.text
    || result.text
    || result.transcription
    || '';
}

function waitForOpen(socket) {
  return new Promise((resolve, reject) => {
    socket.onopen = resolve;
    socket.onerror = reject;
  });
}

function downsample(samples, fromRate, toRate) {
  if (fromRate === toRate) return samples;
  const ratio = fromRate / toRate;
  const length = Math.round(samples.length / ratio);
  const result = new Float32Array(length);
  let offset = 0;
  for (let i = 0; i < length; i += 1) {
    const nextOffset = Math.round((i + 1) * ratio);
    let sum = 0;
    let count = 0;
    for (let j = offset; j < nextOffset && j < samples.length; j += 1) {
      sum += samples[j];
      count += 1;
    }
    result[i] = count > 0 ? sum / count : 0;
    offset = nextOffset;
  }
  return result;
}

function floatTo16BitPcm(samples) {
  const buffer = new ArrayBuffer(samples.length * 2);
  const view = new DataView(buffer);
  for (let i = 0; i < samples.length; i += 1) {
    const s = Math.max(-1, Math.min(1, samples[i]));
    view.setInt16(i * 2, s < 0 ? s * 0x8000 : s * 0x7fff, true);
  }
  return buffer;
}

watch(
  () => props.disabled,
  () => nextTick(() => adjustTextareaHeight()),
);

onMounted(() => {
  nextTick(() => adjustTextareaHeight());
});

onBeforeUnmount(() => {
  stopVoiceInput();
});
</script>
