import type { JsonLine, JsonNode, RGJsonData } from "relation-graph/vue3";
import { FUJIAN_CITY_NAMES, HOME_GALLERY_ITEMS } from "@/data/homeGalleryConfig";

/** 根节点 id（与 `GraphPageReplicaCustomNode` 中根样式判断一致） */
export const GRAPH_PAGE_REPLICA_ROOT_ID = "root";

const GALLERY_LEN = HOME_GALLERY_ITEMS.length;

function galleryItem(index: number) {
  if (GALLERY_LEN === 0) {
    return { image: "", title: "非遗", text: "" };
  }
  const i = ((index % GALLERY_LEN) + GALLERY_LEN) % GALLERY_LEN;
  return HOME_GALLERY_ITEMS[i];
}

function clipTitle(title: string, max: number): string {
  const t = title.trim();
  if (t.length <= max) return t;
  return `${t.slice(0, max)}…`;
}

const LEAF_KINDS = ["简介", "地理", "代表作品", "传承人"] as const;

/** 各级连线颜色区分（赛博配色） */
const EDGE_T1_TO_T2 = {
  color: "rgba(34, 211, 238, 0.9)",
  fontColor: "#cffafe",
  lineWidth: 2.5,
};
const EDGE_T2_TO_T3 = {
  color: "rgba(167, 139, 250, 0.92)",
  fontColor: "#ede9fe",
  lineWidth: 2.5,
};
const EDGE_T3_TO_T4 = {
  color: "rgba(251, 146, 60, 0.92)",
  fontColor: "#ffedd5",
  lineWidth: 2.5,
};

/** 节点占位：1 级最大 → 4 级最小（relation-graph 用 width/height 参与碰撞与布局） */
const NODE_BOX_T1 = 200;
const NODE_BOX_T2 = 102;
const NODE_BOX_T3 = 91;
const NODE_BOX_T4 = 50;

/**
 * 四层树形 JSON：
 * - 1 个根节点（福建省）
 * - 9 个 2 级节点（九地市）
 * - 每市 3 个 3 级节点（与 `HOME_GALLERY_ITEMS` 中该市 3 张图一一对应）
 * - 每个 3 级节点下 4 个 4 级节点；配图与对应 3 级相同（图标复用）
 */
export function buildGraphPageReplicaJson(): RGJsonData {
  const nodes: JsonNode[] = [];
  const lines: JsonLine[] = [];

  nodes.push({
    id: GRAPH_PAGE_REPLICA_ROOT_ID,
    text: "福建省",
    width: NODE_BOX_T1,
    height: NODE_BOX_T1,
    expanded: false,
    data: { tier: 1, isRoot: true },
  });

  for (let ci = 0; ci < 9; ci++) {
    const l2Id = `l2-${ci}`;
    const cityName = FUJIAN_CITY_NAMES[ci];
    const cityShort = cityName.replace("市", "");
    nodes.push({
      id: l2Id,
      text: cityShort,
      width: NODE_BOX_T2,
      height: NODE_BOX_T2,
      expanded: false,
      data: {
        tier: 2,
      },
    });
    lines.push({
      from: GRAPH_PAGE_REPLICA_ROOT_ID,
      to: l2Id,
      text: "下辖",
      ...EDGE_T1_TO_T2,
    });

    for (let s = 0; s < 3; s++) {
      const l3Id = `l3-${ci}-${s}`;
      const item = HOME_GALLERY_ITEMS[ci * 3 + s] ?? galleryItem(ci * 3 + s);

      nodes.push({
        id: l3Id,
        text: clipTitle(item.title, 14),
        width: NODE_BOX_T3,
        height: NODE_BOX_T3,
        expanded: false,
        data: {
          tier: 3,
          cityImage: item.image,
        },
      });
      lines.push({ from: l2Id, to: l3Id, text: "非遗项目", ...EDGE_T2_TO_T3 });

      for (let k = 0; k < 4; k++) {
        const l4Id = `l4-${ci}-${s}-${k}`;
        const kind = LEAF_KINDS[k];
        nodes.push({
          id: l4Id,
          text: kind,
          width: NODE_BOX_T4,
          height: NODE_BOX_T4,
          data: {
            tier: 4,
          },
        });
        lines.push({ from: l3Id, to: l4Id, text: kind, ...EDGE_T3_TO_T4 });
      }
    }
  }

  return {
    rootId: GRAPH_PAGE_REPLICA_ROOT_ID,
    nodes,
    lines,
  };
}
