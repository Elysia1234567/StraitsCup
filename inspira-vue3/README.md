# OmniSource Web Frontend

Vue 3 + Vite 的 OmniSource 展示端，承载福建地图入口、非遗画廊、文化 Agent 聊天、关系图谱、无限画廊和 3D 模型展示。

## 运行

```bash
npm install
npm run dev
```

默认通过当前域名访问后端；如需指定地址，可设置 `VITE_API_BASE_URL`，例如：

```bash
VITE_API_BASE_URL=http://127.0.0.1:8081
```

## 页面

- `/`：首页，福建地图 + 底部非遗画廊。
- `/agent`：聊天室、Agent 选择、多智能体流式对话、生图面板。
- `/graph`：`relation-graph/vue3` 关系图谱页，自定义节点展示城市和画廊图。
- `/infinite-grid`：OGL 无限网格画廊。
- `/data`：Three.js 独立 3D 展示页。

## 关键依赖

- `relation-graph`：关系图谱和自定义节点。
- `echarts`：福建省地图与区域交互。
- `three` / `ogl` / `postprocessing`：3D 与 WebGL 展示。
- `@heroicons/vue`：按钮和功能图标。
- `tailwindcss`：整体布局和视觉系统。

## 代码入口

- `src/pages/HomePage.vue`：首页主视觉与画廊联动。
- `src/pages/AgentPage.vue`：聊天室和 AIGC 面板。
- `src/pages/GraphPage.vue`：关系图谱复刻页面。
- `src/pages/InfiniteGridPage.vue`：无限画廊。
- `src/pages/DataPage.vue`：3D 模型展示。
- `src/api/chatApi.js`：WebSocket、聊天室和生图接口封装。
- `src/data/homeGalleryConfig.ts`：首页 27 条画廊数据与城市分组映射。

## 本地图片

首页画廊图片放在 `public/home-gallery/`，命名建议见 [public/home-gallery/README.md](public/home-gallery/README.md)。

## 说明

- `relation-graph/vue3` 已写入 `package.json` 和锁文件，首次拉取后请先执行 `npm install`。
- 本前端更适合作品展示与答辩，不是模板页。
