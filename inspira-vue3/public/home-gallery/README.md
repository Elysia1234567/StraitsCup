# Home Gallery 图片目录

这个目录存放 Web 首页底部 OGL/CircularGallery 使用的本地图片。代码入口是 `src/data/homeGalleryConfig.ts`。

## 命名规则

当前代码会自动扫描：

```text
public/home-gallery/*.{png,jpg,jpeg,webp,avif}
```

并按下面格式匹配城市和序号：

```text
城市短名-序号-名称.扩展名
```

示例：

```text
福州-1-软木画.png
福州-2-寿山石雕.jpg
泉州-1-提线木偶.webp
```

## 数据对应

- 城市顺序来自 `FUJIAN_CITY_NAMES`，共 9 个地级市。
- 每个城市建议放 3 张，对应首页 27 条非遗画廊项。
- 文件名中的 `名称` 会作为卡片标题。
- 卡片说明文案来自 `HOME_GALLERY_DESCRIPTION_MAP`，缺省时使用兜底描述。

## 使用路径

图片最终通过 public 目录绝对路径访问：

```ts
image: "/home-gallery/福州-1-软木画.png"
```

开发环境会自动加缓存破坏参数；替换图片后若未刷新，重启 `npm run dev`。

## 注意

- 文件名不要重复。
- 城市名建议用短名，如 `福州`、`厦门`、`泉州`。
- 序号只匹配 `1`、`2`、`3`。
