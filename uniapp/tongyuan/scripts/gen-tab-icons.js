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
/* 未选中 #5c6b8a，选中 #00f5ff（与 tabBar 赛博主题一致） */
png(81, 81, 92, 107, 138, path.join(base, 'home.png'))
png(81, 81, 0, 245, 255, path.join(base, 'home-active.png'))
png(81, 81, 92, 107, 138, path.join(base, 'chat.png'))
png(81, 81, 0, 245, 255, path.join(base, 'chat-active.png'))
png(81, 81, 92, 107, 138, path.join(base, 'agent.png'))
png(81, 81, 0, 245, 255, path.join(base, 'agent-active.png'))
/* 图鉴：未选中灰，选中青绿 */
png(81, 81, 92, 107, 138, path.join(base, 'gallery.png'))
png(81, 81, 0, 245, 255, path.join(base, 'gallery-active.png'))
console.log('tab icons written to', base)
