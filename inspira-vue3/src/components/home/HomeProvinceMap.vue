<template>
  <div ref="chartEl" class="home-province-map" />
</template>

<script setup lang="ts">
import * as echarts from 'echarts';
import type { ECharts, EChartsOption } from 'echarts';
import { onMounted, onUnmounted, ref, watch } from 'vue';
import fujianGeo from '@/assets/fujian-cities.json';

type GeoFeature = {
  properties?: {
    name?: string;
    center?: [number, number];
    centroid?: [number, number];
    cp?: [number, number];
    childrenNum?: number;
  };
};

type GeoJson = {
  type: string;
  features: GeoFeature[];
};

const props = withDefaults(
  defineProps<{
    /** 覆盖默认福建省数据；结构与阿里云市级 GeoJSON 一致即可 */
    geoJson?: GeoJson;
  }>(),
  { geoJson: undefined },
);

const emit = defineEmits<{
  /** 点击某个市级区域时传出名称 */
  regionClick: [name: string];
}>();

const chartEl = ref<HTMLDivElement | null>(null);
let chart: ECharts | null = null;

const MAP_NAME = 'homeProvinceMap';

function buildFromGeo(geo: GeoJson) {
  const mapData: { name: string; value: number }[] = [];
  for (const f of geo.features ?? []) {
    const name = f.properties?.name;
    if (!name) continue;
    const p = f.properties;
    mapData.push({
      name,
      value: typeof p.childrenNum === 'number' ? p.childrenNum : 0,
    });
  }
  return { mapData };
}

function buildOption(geo: GeoJson): EChartsOption {
  const { mapData } = buildFromGeo(geo);

  return {
    backgroundColor: 'transparent',
    tooltip: { show: false },
    geo: [
      {
        layoutCenter: ['50%', '50%'],
        layoutSize: '200%',
        show: true,
        map: MAP_NAME,
        roam: false,
        zoom: 0.5,
        aspectScale: 1.2,
        label: {
          color: '#fff',
          show: false,
        },
        itemStyle: {
          areaColor: {
            type: 'linear',
            x: 1200,
            y: 0,
            x2: 0,
            y2: 0,
            colorStops: [
              { offset: 0, color: 'rgba(3,27,78,0.75)' },
              { offset: 1, color: 'rgba(58,149,253,0.75)' },
            ],
            global: true,
          },
          borderColor: '#c0f3fb',
          borderWidth: 2.5,
          shadowColor: '#8cd3ef',
          shadowOffsetY: 10,
          shadowBlur: 30,
        },
        emphasis: {
          label: { show: true, color: '#000' },
          itemStyle: { areaColor: 'rgba(255,255,255,0.8)' },
        },
        select: {
          label: { show: true, color: '#000' },
          itemStyle: { areaColor: 'rgba(255,255,255,0.8)' },
        },
      },
      {
        type: 'map',
        map: MAP_NAME,
        zlevel: -1,
        aspectScale: 1.2,
        zoom: 0.5,
        layoutCenter: ['50%', '51%'],
        layoutSize: '180%',
        roam: false,
        silent: true,
        itemStyle: {
          borderWidth: 2,
          borderColor: 'rgba(58,149,253,0.8)',
          shadowColor: 'rgba(172, 122, 255,0.5)',
          shadowOffsetY: 5,
          shadowBlur: 15,
          areaColor: 'rgba(5,21,35,0.1)',
        },
      },
      {
        type: 'map',
        map: MAP_NAME,
        zlevel: -2,
        aspectScale: 1.2,
        zoom: 0.5,
        layoutCenter: ['50%', '52%'],
        layoutSize: '180%',
        roam: false,
        silent: true,
        itemStyle: {
          borderWidth: 2,
          borderColor: 'rgba(58,149,253,0.6)',
          shadowColor: 'rgba(65, 214, 255,1)',
          shadowOffsetY: 5,
          shadowBlur: 15,
          areaColor: 'transparent',
        },
      },
      {
        type: 'map',
        map: MAP_NAME,
        zlevel: -3,
        aspectScale: 1.2,
        zoom: 0.5,
        layoutCenter: ['50%', '53%'],
        layoutSize: '180%',
        roam: false,
        silent: true,
        itemStyle: {
          borderWidth: 2,
          borderColor: 'rgba(58,149,253,0.4)',
          shadowColor: 'rgba(58,149,253,1)',
          shadowOffsetY: 15,
          shadowBlur: 10,
          areaColor: 'transparent',
        },
      },
      {
        type: 'map',
        map: MAP_NAME,
        zlevel: -4,
        aspectScale: 1.2,
        zoom: 0.5,
        layoutCenter: ['50%', '54%'],
        layoutSize: '180%',
        roam: false,
        silent: true,
        itemStyle: {
          borderWidth: 8,
          borderColor: 'rgba(5,9,57,0.8)',
          shadowColor: 'rgba(29, 111, 165,0.8)',
          shadowOffsetY: 15,
          shadowBlur: 10,
          areaColor: 'rgba(5,21,35,0.1)',
        },
      },
    ],
    series: [
      {
        type: 'map',
        map: MAP_NAME,
        geoIndex: 0,
        aspectScale: 1.2,
        zoom: 0.5,
        selectedMode: 'single',
        showLegendSymbol: true,
        roam: true,
        label: {
          show: true,
          color: '#fff',
          fontSize: 12,
        },
        itemStyle: {
          areaColor: {
            type: 'linear',
            x: 1200,
            y: 0,
            x2: 0,
            y2: 0,
            colorStops: [
              { offset: 0, color: 'rgba(3,27,78,0.75)' },
              { offset: 1, color: 'rgba(58,149,253,0.75)' },
            ],
            global: true,
          },
          borderColor: '#fff',
          borderWidth: 1.5,
        },
        emphasis: {
          label: { show: true, color: '#000' },
          itemStyle: { areaColor: 'rgba(255,255,255,0.8)' },
        },
        select: {
          label: { show: true, color: '#000' },
          itemStyle: { areaColor: 'rgba(255,255,255,0.8)' },
        },
        layoutCenter: ['50%', '50%'],
        layoutSize: '180%',
        animation: false,
        markPoint: { symbol: 'none' },
        data: mapData,
      },
    ],
  };
}

function render() {
  const el = chartEl.value;
  if (!el) return;
  const geo = (props.geoJson ?? fujianGeo) as GeoJson;
  el.removeAttribute('_echarts_instance_');
  if (chart) {
    chart.dispose();
    chart = null;
  }
  chart = echarts.init(el, undefined, { renderer: 'canvas' });
  chart.showLoading();
  echarts.registerMap(MAP_NAME, geo as never);
  chart.hideLoading();
  chart.setOption(buildOption(geo));

  chart.off('click');
  chart.on('click', (params: unknown) => {
    const p = params as { componentType?: string; seriesType?: string; name?: string };
    if (p.componentType === 'series' && p.seriesType === 'map' && p.name) {
      emit('regionClick', p.name);
    }
  });
}

function onResize() {
  chart?.resize();
}

onMounted(() => {
  render();
  window.addEventListener('resize', onResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', onResize);
  chart?.dispose();
  chart = null;
});

watch(
  () => props.geoJson,
  () => {
    render();
  },
  { deep: true },
);
</script>

<style scoped>
.home-province-map {
  width: 100%;
  height: 100%;
  min-height: 0;
  animation: map-float 5s linear infinite;
}

@keyframes map-float {
  0% {
    transform: translateY(-10px);
  }
  50% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-10px);
  }
}
</style>
