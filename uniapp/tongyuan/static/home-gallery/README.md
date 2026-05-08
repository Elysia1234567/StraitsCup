# 首页画廊本地图片

## 复制来源

将 **`StraitsCup/inspira-vue3/public/home-gallery/`** 下的图片文件复制到本目录：

```
StraitsCup/uniapp/tongyuan/static/home-gallery/
```

uni-app 使用 **`static`** 目录下的资源，构建后可通过 **`/static/home-gallery/文件名`** 访问（与 Web 端 `/home-gallery/文件名` 对应）。

## 文件名

须与 `data/gallery.js` 中每条数据的 `image` 字段一致，例如：

- `福州-1-软木画.png`
- `泉州-2-德化陶瓷.png`

命名规则与 Web 端一致：`城市简称-序号1~3-名称.png`（支持 `.png` / `.jpg` 等时，请同步修改 `gallery.js` 中的扩展名）。

若仓库里暂无实体图片，从 inspira 工程拷贝或按上述规则放置文件后即可在小程序/H5 显示。
