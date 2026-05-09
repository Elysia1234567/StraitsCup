/**
 * 底部 tabBar 图标：默认使用 DCloud 官方示例 hello-uniapp 的 PNG（见 static/）。
 * 仅当需要纯色占位图时执行：GEN_TAB_PLACEHOLDERS=1 node scripts/gen-tab-icons.js
 */
if (process.env.GEN_TAB_PLACEHOLDERS !== '1') {
  console.log(
    '跳过：当前 tab 图标为官方示例资源（pages.json → static/home*.png、static/tab-*.png）。',
  )
  console.log('若需生成纯色占位 PNG：GEN_TAB_PLACEHOLDERS=1 npm run gen-tab-icons')
  process.exit(0)
}

const fs = require('fs')
const path = require('path')
const zlib = require('zlib')

function crc32(buf) {
  let c = 0xffffffff
  for (let i = 0; i < buf.length; i++) {
    c ^= buf[i]
    for (let k = 0; k < 8; k++) c = (c >>> 1) ^ (0xedb88320 & -(c & 1))
  }
  return (c ^ 0xffffffff) >>> 0
}

function u32(n) {
  return Buffer.from([(n >>> 24) & 255, (n >>> 16) & 255, (n >>> 8) & 255, n & 255])
}

function chunk(typeStr, data) {
  const type = Buffer.from(typeStr)
  return Buffer.concat([u32(data.length), type, data, u32(crc32(Buffer.concat([type, data])))])
}

function png(w, h, r, g, b, outPath) {
  const row = Buffer.alloc(1 + w * 3)
  const rows = []
  for (let y = 0; y < h; y++) {
    row[0] = 0
    for (let x = 0; x < w; x++) {
      row[1 + x * 3] = r
      row[2 + x * 3] = g
      row[3 + x * 3] = b
    }
    rows.push(Buffer.from(row))
  }
  const raw = Buffer.concat(rows)
  const id = zlib.deflateSync(raw)
  const ihdr = Buffer.alloc(13)
  ihdr.writeUInt32BE(w, 0)
  ihdr.writeUInt32BE(h, 4)
  ihdr[8] = 8
  ihdr[9] = 2
  ihdr[10] = 0
  ihdr[11] = 0
  ihdr[12] = 0
  const sig = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])
  const body = Buffer.concat([
    chunk('IHDR', ihdr),
    chunk('IDAT', id),
    chunk('IEND', Buffer.alloc(0)),
  ])
  fs.writeFileSync(outPath, Buffer.concat([sig, body]))
}

const base = path.join(__dirname, '..', 'static')
if (!fs.existsSync(base)) fs.mkdirSync(base, { recursive: true })
const dimR = 124
const dimG = 115
const dimB = 149
const actR = 196
const actG = 181
const actB = 253
png(81, 81, dimR, dimG, dimB, path.join(base, 'home.png'))
png(81, 81, actR, actG, actB, path.join(base, 'home-active.png'))
png(81, 81, dimR, dimG, dimB, path.join(base, 'tab-chat.png'))
png(81, 81, actR, actG, actB, path.join(base, 'tab-chat-active.png'))
png(81, 81, dimR, dimG, dimB, path.join(base, 'tab-agent.png'))
png(81, 81, actR, actG, actB, path.join(base, 'tab-agent-active.png'))
png(81, 81, dimR, dimG, dimB, path.join(base, 'tab-gallery.png'))
png(81, 81, actR, actG, actB, path.join(base, 'tab-gallery-active.png'))
console.log('placeholder tab icons written to', base)
