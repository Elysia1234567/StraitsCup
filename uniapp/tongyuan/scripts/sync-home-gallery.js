/**
 * 从 inspira-vue3/public/home-gallery 复制图片到 static/home-gallery，
 * 并生成 pages/gallery/gallery-data.js（与 src/data/homeGalleryConfig.ts 文案一致）。
 * 目标文件名使用 g001、g002…（仅 ASCII），避免微信开发者工具对中文路径本地资源返回 500。
 * 发布前：npm run shrink-gallery（PNG→压缩 JPG）。
 */
const fs = require('fs')
const path = require('path')

const CITY_ORDER = ['福州', '厦门', '莆田', '三明', '泉州', '漳州', '南平', '龙岩', '宁德']

const DESCRIPTION_MAP = {
  '福州-1':
    '福州软木画以栓皮为材，刀下起伏如山海层叠。细看亭台舟楫与闽都风物，能读到匠人把一座城的温润气质，慢慢刻进木纹里的耐心。',
  '福州-2':
    '寿山石雕讲究因材施艺，一块石头的色脉与肌理，决定了它将成为山川人物还是案头雅器。刀法克制而含蓄，像福州人处世的分寸感。',
  '福州-3':
    '脱胎漆器以麻布与漆层反复塑形，轻而坚，润而雅。器表流转的光泽背后，是日复一日的打磨，映照着闽地工艺的沉静与韧性。',
  '厦门-1':
    '送王船承载着海洋信仰与社区记忆。鼓乐、仪轨与船体装饰共同构成一场面向大海的叙事，讲述人们对平安、丰收与团圆的朴素愿望。',
  '厦门-2':
    '厦门珠绣以针为笔、以珠为墨，在布面上铺陈潮汐般的光点。它既有闽南审美的明丽，也有女性手作中细密而坚韧的生活质感。',
  '厦门-3':
    '漆线雕用盘绕的漆线勾勒纹样，繁而不乱、密而有序。近看是工艺的克己，远看是装饰的华美，凝结着闽南工坊一代代手上功夫。',
  '莆田-1':
    '龙眼木雕保留木材天然纹理，让人物、花鸟与民俗场景在刀痕间生长。温润木色里藏着莆田匠人对“活态传承”的执着理解。',
  '莆田-2':
    '莆仙戏唱腔清丽、身段细腻，舞台上人情冷暖与家国伦理交织。它不只是一门戏，更是地方方言、礼俗和记忆的公共剧场。',
  '莆田-3':
    '莆田银饰讲究錾刻与掐丝，纹样常见瑞禽花卉与海丝意象。银光并不张扬，却在婚嫁节庆中见证着生活的郑重与祝福。',
  '三明-1':
    '明溪微雕在方寸之间开山水、塑人物。放大镜下的一刀一刻，像把时间压缩进细节，也把“精微”二字练成了日常修行。',
  '三明-2':
    '将乐窑器物朴实沉静，釉色含蓄耐看。它不追求喧哗的华丽，而是在柴火与泥土的交汇里，守住一种温厚的生活美学。',
  '三明-3':
    '客家竹编取材山林、因用成形，从农具到家什都见编织智慧。经纬交错之间，是客家人顺应自然、勤俭持家的生存哲学。',
  '泉州-1':
    '提线木偶十指牵百线，角色在抬腕回眸间有了灵魂。泉州戏台上的这一门功夫，把海丝古城的开放与包容演成了活的传统。',
  '泉州-2':
    '德化陶瓷以“白”见长，釉面温润如脂，造型清雅克制。窑火中的不确定，最终化为手中可触的宁静与诗意。',
  '泉州-3':
    '刻纸花灯将剪纸与灯彩融合，点灯即成一幅会发光的民间图卷。节庆夜色里，它让巷陌、人群与传统重新彼此照亮。',
  '漳州-1':
    '布袋木偶戏以小见大，掌中人物却能演尽世态人情。锣鼓一起，诙谐与悲悯并存，是闽南街巷最亲切的文化表情。',
  '漳州-2':
    '木板年画色彩浓烈、构图饱满，寄托着迎新纳福的集体想象。每一次印刷，都是对“年”的重新书写与对家宅的祝愿。',
  '漳州-3':
    '片仔癀承载着闽南药业传统与海上交流史。它不仅是技艺系统，也是一种代代相授的经验伦理与匠心规范。',
  '南平-1':
    '建阳建盏以铁胎厚釉见功力，盏面窑变如夜空流星。握在手中，能感到宋韵茶事与当代审美在同一器物里相遇。',
  '南平-2':
    '邵武傩面以夸张造型与强烈色彩传达古老仪式感。面具背后，是人们对自然、祖先与共同体秩序的敬畏与想象。',
  '南平-3':
    '武夷岩茶生于丹霞岩隙，岩骨花香层次悠长。制茶工序严谨而讲究节气，折射出山地社会与自然共生的时间观。',
  '龙岩-1':
    '客家米酒以粮为本、以曲为魂，发酵中酿出乡土的温度。它常出现在节庆与团聚时刻，连接着记忆中的家与人情。',
  '龙岩-2':
    '龙岩农民画题材来自田野与日常，色彩大胆、情绪直白。它让普通人的劳动经验成为主角，呈现土地最鲜活的叙述。',
  '龙岩-3':
    '客家刺绣针法细密、图案寓意吉祥，常见于服饰与家居器物。每一针都像在缝合迁徙记忆，也缝合家族情感。',
  '宁德-1':
    '霍童线狮以线控狮形，动作灵巧而富节奏感。鼓点与步法交织出热烈现场，展现山海之间民间庆典的生命力。',
  '宁德-2':
    '柘荣剪纸以简驭繁，阴阳虚实间见章法。窗花、礼俗与节令在纸上相逢，留下的是宁德乡土最柔韧的审美传统。',
  '宁德-3':
    '畲族服饰纹样鲜明、配色大胆，银饰与织绣相映成趣。它不仅是穿着艺术，更是族群历史、信仰与身份记忆的可见表达。',
}

const root = path.join(__dirname, '..')
const srcDir = path.join(root, '..', '..', 'inspira-vue3', 'public', 'home-gallery')
const destDir = path.join(root, 'static', 'home-gallery')
const outData = path.join(root, 'pages', 'gallery', 'gallery-data.js')

function parseName(file) {
  const m = file.match(/^(.+?)-([1-3])-(.+)\.(png|jpg|jpeg|webp)$/i)
  if (!m) return null
  return { cityShort: m[1], slot: Number(m[2]), title: m[3], file }
}

function sortKey(meta) {
  const ci = CITY_ORDER.indexOf(meta.cityShort)
  const city = ci < 0 ? 99 : ci
  return city * 10 + meta.slot
}

function main() {
  if (!fs.existsSync(srcDir)) {
    console.error('源目录不存在:', srcDir)
    process.exit(1)
  }
  fs.mkdirSync(destDir, { recursive: true })
  if (fs.existsSync(destDir)) {
    for (const f of fs.readdirSync(destDir)) {
      fs.unlinkSync(path.join(destDir, f))
    }
  }

  const files = fs.readdirSync(srcDir).filter((f) => /\.(png|jpg|jpeg|webp)$/i.test(f))
  const metas = []
  for (const file of files) {
    const meta = parseName(file)
    if (!meta) {
      console.warn('跳过（命名不符）:', file)
      continue
    }
    metas.push(meta)
  }
  metas.sort((a, b) => sortKey(a) - sortKey(b))

  const items = metas.map((m, index) => {
    const destPng = `g${String(index + 1).padStart(3, '0')}.png`
    fs.copyFileSync(path.join(srcDir, m.file), path.join(destDir, destPng))
    const key = `${m.cityShort}-${m.slot}`
    const description =
      DESCRIPTION_MAP[key] ??
      `${m.title}是${m.cityShort}具有代表性的非遗项目，展示其历史渊源、工艺特色与当代传承价值。`
    return {
      image: `/static/home-gallery/${destPng}`,
      label: `${m.cityShort} · ${m.slot}`,
      title: m.title,
      description,
    }
  })

  const banner =
    '/**\n * 非遗图鉴数据：由 scripts/sync-home-gallery.js 根据 inspira-vue3/public/home-gallery 生成。\n * 更新图片后请执行：npm run sync-gallery\n */\n'
  const body = `${banner}export const GALLERY_ITEMS = ${JSON.stringify(items, null, 2)}\n`
  fs.mkdirSync(path.dirname(outData), { recursive: true })
  fs.writeFileSync(outData, body, 'utf8')
  console.log('已复制', metas.length, '个文件到', destDir)
  console.log('已写入', outData)
}

main()
