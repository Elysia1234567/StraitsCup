# Home Gallery Local Images

把首页底部弯曲卡片要用的本地图片放到这个目录：

- `public/home-gallery/`

## 命名建议

建议统一命名为：

- `省份名-编号-独立名称.jpg`

例如：

- `福建-01-鼓浪屿日落.jpg`
- `福建-02-武夷山云海.jpg`
- `浙江-01-西湖夜景.jpg`

## 在代码里如何使用

当前首页底部卡片配置在：

- `src/data/homeGalleryConfig.ts`

图片路径写法使用 public 目录的绝对路径：

- `/home-gallery/福建-01-鼓浪屿日落.jpg`

示例（只演示写法）：

```ts
image: '/home-gallery/福建-01-鼓浪屿日落.jpg'
```

## 注意

- 修改图片文件后，开发环境一般会自动生效；若未生效，重启 `npm run dev`
- 文件名尽量不要重复
- 推荐使用 `.jpg` 或 `.png`
