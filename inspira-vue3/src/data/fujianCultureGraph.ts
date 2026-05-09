import type { JsonLine, JsonNode, RGJsonData } from "relation-graph/vue3";
import { FUJIAN_CITY_NAMES, HOME_GALLERY_ITEMS, type HomeGalleryItem } from "./homeGalleryConfig";

/** 各市示意地理锚点（展示用，非精确测绘） */
const CITY_GEO_DETAIL: Record<string, string> = {
  福州市: "福州市鼓楼区 · 三坊七巷非遗博览苑周边（示意）",
  厦门市: "厦门市思明区 · 闽南文化生态保护实验区展示带（示意）",
  莆田市: "莆田市荔城区 · 兴化府历史文化街区（示意）",
  三明市: "三明市三元区 · 非遗传承展示中心（示意）",
  泉州市: "泉州市鲤城区 · 刺桐文脉非遗聚集区（示意）",
  漳州市: "漳州市芗城区 · 闽南文化生态保护核心区（示意）",
  南平市: "武夷山市 · 朱子文化与茶道非遗廊道（示意）",
  龙岩市: "龙岩市新罗区 · 客家文化生态保护区（示意）",
  宁德市: "宁德市蕉城区 · 畲族文化生态示范点（示意）",
};

const INHERITOR_LABELS = [
  "省级代表性传承人（名录示例）",
  "市级传承人工作室 · 传习所",
  "青年传承人 · 非遗研习计划",
];

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function clip(text: string, max: number): string {
  const t = text.trim();
  if (t.length <= max) return t;
  return `${t.slice(0, max)}…`;
}

function rootHtml(): string {
  return `<div class="fj-ng fj-ng-root-wrap"><div class="fj-ng-root-pill"><span class="fj-ng-root-title">福建省</span><span class="fj-ng-root-sub">非遗文化图谱</span></div></div>`;
}

function cityHtml(name: string): string {
  return `<div class="fj-ng fj-ng-city-wrap"><span class="fj-ng-city-pill">${escapeHtml(name.replace("市", ""))}</span></div>`;
}

function cultureHtml(item: HomeGalleryItem): string {
  const title = escapeHtml(item.title);
  const src = escapeHtml(item.image);
  return `<div class="fj-ng fj-ng-culture-wrap"><div class="fj-ng-culture-card"><img class="fj-ng-culture-img" src="${src}" alt=""/><span class="fj-ng-culture-title">${title}</span></div></div>`;
}

function leafHtml(label: string, body: string): string {
  return `<div class="fj-ng fj-ng-leaf-wrap"><div class="fj-ng-leaf-pill"><span class="fj-ng-leaf-k">${escapeHtml(label)}</span><span class="fj-ng-leaf-v">${escapeHtml(body)}</span></div></div>`;
}

/** 福建省总节点 id，与 relation-graph `focusRootNode` / `focusNodeById` 对齐 */
export const FUJIAN_GRAPH_ROOT_ID = "fj-root";

export function buildFujianCultureGraphJson(): RGJsonData {
  const nodes: JsonNode[] = [];
  const lines: JsonLine[] = [];

  const rootId = FUJIAN_GRAPH_ROOT_ID;
  nodes.push({
    id: rootId,
    text: "",
    width: 184,
    height: 76,
    nodeShape: 1,
    borderWidth: 0,
    borderColor: "transparent",
    color: "transparent",
    expanded: false,
    innerHTML: rootHtml(),
    data: { tier: 1, kind: "province" },
    styleClass: "fj-graph-node fj-graph-node--pill-host",
  });

  for (let ci = 0; ci < FUJIAN_CITY_NAMES.length; ci++) {
    const cityName = FUJIAN_CITY_NAMES[ci];
    const cityId = `city-${ci}`;
    nodes.push({
      id: cityId,
      text: "",
      width: 96,
      height: 44,
      nodeShape: 1,
      borderWidth: 0,
      borderColor: "transparent",
      color: "transparent",
      fontColor: "#ffe4d6",
      expanded: false,
      innerHTML: cityHtml(cityName),
      data: { tier: 2, kind: "city", city: cityName },
      styleClass: "fj-graph-node fj-graph-node--pill-host",
    });
    lines.push({
      from: rootId,
      to: cityId,
      text: "下辖",
      color: "rgba(34, 211, 238, 0.55)",
      fontColor: "#67e8f9",
      lineWidth: 1.5,
    });

    for (let slot = 0; slot < 3; slot++) {
      const idx = ci * 3 + slot;
      const item = HOME_GALLERY_ITEMS[idx];
      const cultureId = `culture-${ci}-${slot}`;
      nodes.push({
        id: cultureId,
        text: "",
        width: 96,
        height: 124,
        nodeShape: 1,
        borderWidth: 0,
        borderColor: "transparent",
        color: "transparent",
        expanded: false,
        innerHTML: cultureHtml(item),
        data: { tier: 3, kind: "culture", city: cityName, slot, item },
        styleClass: "fj-graph-node fj-graph-node--pill-host",
      });
      lines.push({
        from: cityId,
        to: cultureId,
        text: "非遗项目",
        color: "rgba(251, 146, 60, 0.5)",
        fontColor: "#fdba74",
        lineWidth: 1.5,
      });

      const descId = `${cultureId}-desc`;
      const locId = `${cultureId}-loc`;
      const workId = `${cultureId}-work`;
      const masterId = `${cultureId}-master`;

      const geo = CITY_GEO_DETAIL[cityName] ?? `${cityName} · 市级非遗展示空间（示意）`;
      const inheritor = INHERITOR_LABELS[idx % INHERITOR_LABELS.length];

      const leafNodes: { id: string; label: string; body: string }[] = [
        { id: descId, label: "简介", body: clip(item.description, 96) },
        { id: locId, label: "地理位置", body: geo },
        { id: workId, label: "代表作品", body: item.title },
        { id: masterId, label: "传承人", body: inheritor },
      ];

      for (const leaf of leafNodes) {
        nodes.push({
          id: leaf.id,
          text: "",
          width: 200,
          height: leaf.label === "简介" ? 72 : 52,
          nodeShape: 1,
          borderWidth: 0,
          borderColor: "transparent",
          color: "transparent",
          fontColor: "#f5e6ff",
          innerHTML: leafHtml(leaf.label, leaf.body),
          data: { tier: 4, kind: "leaf", role: leaf.label, cultureId },
          styleClass: "fj-graph-node fj-graph-node--pill-host",
        });
        lines.push({
          from: cultureId,
          to: leaf.id,
          text: leaf.label,
          color: "rgba(52, 211, 153, 0.45)",
          fontColor: "#a7f3d0",
          lineWidth: 1,
        });
      }
    }
  }

  return {
    rootId,
    nodes,
    lines,
  };
}
