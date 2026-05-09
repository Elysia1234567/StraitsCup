/**
 * 将 static/home-gallery 下 PNG 转为压缩 JPEG，并同步修改 pages/gallery/gallery-data.js 中的后缀。
 *
 * 用法：npm run shrink-gallery（需先 npm run sync-gallery）
 */
const fs = require('fs')
const path = require('path')

const root = path.join(__dirname, '..')
const dir = path.join(root, 'static', 'home-gallery')
const dataPath = path.join(root, 'pages', 'gallery', 'gallery-data.js')

async function main() {
  let sharp
  try {
    sharp = require('sharp')
  } catch {
    console.error('请先安装 sharp：npm install sharp --save-dev')
    process.exit(1)
  }

  if (!fs.existsSync(dir)) {
    console.error('目录不存在，请先执行 npm run sync-gallery：', dir)
    process.exit(1)
  }

  const files = fs.readdirSync(dir).filter((f) => /\.png$/i.test(f))
  if (!files.length) {
    console.log('无 PNG 需压缩（若已是 JPG，可跳过）。')
    return
  }

  for (const f of files) {
    const input = path.join(dir, f)
    const out = path.join(dir, f.replace(/\.png$/i, '.jpg'))
    await sharp(input)
      .resize({ width: 520, withoutEnlargement: true })
      .jpeg({ quality: 72, mozjpeg: true })
      .toFile(out)
    fs.unlinkSync(input)
  }

  if (fs.existsSync(dataPath)) {
    let s = fs.readFileSync(dataPath, 'utf8')
    s = s.replace(/\.png"/g, '.jpg"')
    fs.writeFileSync(dataPath, s, 'utf8')
  }

  const bytes = fs.readdirSync(dir).reduce((sum, name) => {
    const p = path.join(dir, name)
    const st = fs.statSync(p)
    return sum + (st.isFile() ? st.size : 0)
  }, 0)
  console.log('已压缩', files.length, '张图 → JPEG；目录合计约', (bytes / 1024 / 1024).toFixed(2), 'MB')
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
