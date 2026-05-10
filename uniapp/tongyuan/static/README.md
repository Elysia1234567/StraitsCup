# UniApp 静态资源

`static/` 目录存放 UniApp 端的底部 Tab 图标、Logo 和首页资源。

## 当前资源

- `home.png` / `home-active.png`
- `tab-chat.png` / `tab-chat-active.png`
- `tab-agent.png` / `tab-agent-active.png`
- `tab-gallery.png` / `tab-gallery-active.png`
- `logo.png` / `logo.svg`

这些资源与 `pages.json` 的 `tabBar` 配置一一对应。

## 约定

- 图标应保持 81x81 或接近正方形，避免边缘锯齿。
- 选中态与未选中态尽量使用同一图形体系，便于统一视觉。
- 若替换图片，保持文件名不变可以避免同步修改 `pages.json`。

## 备注

- `static/home-gallery/` 也会被首页画廊脚本读取。
- 图标生成脚本位于 `scripts/gen-tab-icons.js`。
