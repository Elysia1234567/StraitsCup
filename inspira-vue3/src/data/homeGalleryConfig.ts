/**
 * 首页底部画廊：9 组 × 3 张 = 27 条（对应福建 9 个地级市，每市 3 张）。
 * 图片文件建议命名：`城市-序号-名称.xxx`，例如 `福州-1-软木画.png`。
 * 本配置会自动从 `public/home-gallery` 扫描本地图片并按“城市+序号”匹配。
 */
export const FUJIAN_CITY_NAMES = [
  '福州市',
  '厦门市',
  '莆田市',
  '三明市',
  '泉州市',
  '漳州市',
  '南平市',
  '龙岩市',
  '宁德市',
] as const;

export function cityNameToGalleryGroup(name: string): number | null {
  const idx = (FUJIAN_CITY_NAMES as readonly string[]).indexOf(name);
  if (idx < 0) return null;
  return idx;
}

export type HomeGalleryItem = {
  image: string;
  text: string;
  title: string;
  description: string;
};

type LocalImageMeta = {
  cityShort: string;
  slot: number;
  imagePath: string;
  title: string;
};

const HOME_GALLERY_LOCAL_FILES = import.meta.glob(
  '/public/home-gallery/*.{png,jpg,jpeg,webp,avif}',
  {
    eager: true,
    query: '?url',
    import: 'default',
  },
) as Record<string, string>;

const DEV_ASSET_CACHE_BUSTER = import.meta.env.DEV ? `?v=${Date.now()}` : '';

function toPublicHomeGalleryUrl(filePath: string): string {
  const fileName = filePath.split('/').pop();
  if (!fileName) return '';
  return `/home-gallery/${encodeURIComponent(fileName)}${DEV_ASSET_CACHE_BUSTER}`;
}

function parseLocalImageMeta(filePath: string, imagePath: string): LocalImageMeta | null {
  const fileName = filePath.split('/').pop();
  if (!fileName) return null;
  const match = fileName.match(/^(.+?)-([1-3])-(.+)\.[a-zA-Z0-9]+$/);
  if (!match) return null;
  return {
    cityShort: match[1],
    slot: Number(match[2]),
    imagePath,
    title: match[3],
  };
}

function buildLocalCityImageMap(): Map<string, LocalImageMeta[]> {
  const parsedList = Object.entries(HOME_GALLERY_LOCAL_FILES)
    .map(([filePath]) => parseLocalImageMeta(filePath, toPublicHomeGalleryUrl(filePath)))
    .filter((x): x is LocalImageMeta => x !== null);

  const map = new Map<string, LocalImageMeta[]>();
  parsedList.forEach((meta) => {
    const { cityShort, slot } = meta;
    const current = map.get(cityShort) ?? [];
    current[slot - 1] = meta;
    map.set(cityShort, current);
  });
  return map;
}

const LOCAL_CITY_IMAGE_MAP = buildLocalCityImageMap();

const FUZHOU_LOCAL_IMAGES = LOCAL_CITY_IMAGE_MAP.get('福州') ?? [];

const HOME_GALLERY_DESCRIPTION_MAP: Record<string, string> = {
  '福州-1': '福州软木画以栓皮为材，刀下起伏如山海层叠。细看亭台舟楫与闽都风物，能读到匠人把一座城的温润气质，慢慢刻进木纹里的耐心。',
  '福州-2': '寿山石雕讲究因材施艺，一块石头的色脉与肌理，决定了它将成为山川人物还是案头雅器。刀法克制而含蓄，像福州人处世的分寸感。',
  '福州-3': '脱胎漆器以麻布与漆层反复塑形，轻而坚，润而雅。器表流转的光泽背后，是日复一日的打磨，映照着闽地工艺的沉静与韧性。',

  '厦门-1': '送王船承载着海洋信仰与社区记忆。鼓乐、仪轨与船体装饰共同构成一场面向大海的叙事，讲述人们对平安、丰收与团圆的朴素愿望。',
  '厦门-2': '厦门珠绣以针为笔、以珠为墨，在布面上铺陈潮汐般的光点。它既有闽南审美的明丽，也有女性手作中细密而坚韧的生活质感。',
  '厦门-3': '漆线雕用盘绕的漆线勾勒纹样，繁而不乱、密而有序。近看是工艺的克己，远看是装饰的华美，凝结着闽南工坊一代代手上功夫。',

  '莆田-1': '龙眼木雕保留木材天然纹理，让人物、花鸟与民俗场景在刀痕间生长。温润木色里藏着莆田匠人对“活态传承”的执着理解。',
  '莆田-2': '莆仙戏唱腔清丽、身段细腻，舞台上人情冷暖与家国伦理交织。它不只是一门戏，更是地方方言、礼俗和记忆的公共剧场。',
  '莆田-3': '莆田银饰讲究錾刻与掐丝，纹样常见瑞禽花卉与海丝意象。银光并不张扬，却在婚嫁节庆中见证着生活的郑重与祝福。',

  '三明-1': '明溪微雕在方寸之间开山水、塑人物。放大镜下的一刀一刻，像把时间压缩进细节，也把“精微”二字练成了日常修行。',
  '三明-2': '将乐窑器物朴实沉静，釉色含蓄耐看。它不追求喧哗的华丽，而是在柴火与泥土的交汇里，守住一种温厚的生活美学。',
  '三明-3': '客家竹编取材山林、因用成形，从农具到家什都见编织智慧。经纬交错之间，是客家人顺应自然、勤俭持家的生存哲学。',

  '泉州-1': '提线木偶十指牵百线，角色在抬腕回眸间有了灵魂。泉州戏台上的这一门功夫，把海丝古城的开放与包容演成了活的传统。',
  '泉州-2': '德化陶瓷以“白”见长，釉面温润如脂，造型清雅克制。窑火中的不确定，最终化为手中可触的宁静与诗意。',
  '泉州-3': '刻纸花灯将剪纸与灯彩融合，点灯即成一幅会发光的民间图卷。节庆夜色里，它让巷陌、人群与传统重新彼此照亮。',

  '漳州-1': '布袋木偶戏以小见大，掌中人物却能演尽世态人情。锣鼓一起，诙谐与悲悯并存，是闽南街巷最亲切的文化表情。',
  '漳州-2': '木板年画色彩浓烈、构图饱满，寄托着迎新纳福的集体想象。每一次印刷，都是对“年”的重新书写与对家宅的祝愿。',
  '漳州-3': '片仔癀承载着闽南药业传统与海上交流史。它不仅是技艺系统，也是一种代代相授的经验伦理与匠心规范。',

  '南平-1': '建阳建盏以铁胎厚釉见功力，盏面窑变如夜空流星。握在手中，能感到宋韵茶事与当代审美在同一器物里相遇。',
  '南平-2': '邵武傩面以夸张造型与强烈色彩传达古老仪式感。面具背后，是人们对自然、祖先与共同体秩序的敬畏与想象。',
  '南平-3': '武夷岩茶生于丹霞岩隙，岩骨花香层次悠长。制茶工序严谨而讲究节气，折射出山地社会与自然共生的时间观。',

  '龙岩-1': '客家米酒以粮为本、以曲为魂，发酵中酿出乡土的温度。它常出现在节庆与团聚时刻，连接着记忆中的家与人情。',
  '龙岩-2': '龙岩农民画题材来自田野与日常，色彩大胆、情绪直白。它让普通人的劳动经验成为主角，呈现土地最鲜活的叙述。',
  '龙岩-3': '客家刺绣针法细密、图案寓意吉祥，常见于服饰与家居器物。每一针都像在缝合迁徙记忆，也缝合家族情感。',

  '宁德-1': '霍童线狮以线控狮形，动作灵巧而富节奏感。鼓点与步法交织出热烈现场，展现山海之间民间庆典的生命力。',
  '宁德-2': '柘荣剪纸以简驭繁，阴阳虚实间见章法。窗花、礼俗与节令在纸上相逢，留下的是宁德乡土最柔韧的审美传统。',
  '宁德-3': '畲族服饰纹样鲜明、配色大胆，银饰与织绣相映成趣。它不仅是穿着艺术，更是族群历史、信仰与身份记忆的可见表达。',
};

function getDescriptionByCityAndSlot(cityShort: string, slot: number, title: string): string {
  const key = `${cityShort}-${slot}`;
  return (
    HOME_GALLERY_DESCRIPTION_MAP[key] ??
    `${title}是${cityShort}具有代表性的非遗项目，展示其历史渊源、工艺特色与当代传承价值。`
  );
}

function cardForGroup(groupIndex: number, slotInGroup: number): HomeGalleryItem {
  const city = FUJIAN_CITY_NAMES[groupIndex];
  const short = city.replace('市', '');
  const localMetas = LOCAL_CITY_IMAGE_MAP.get(short) ?? [];
  const localMeta = localMetas[slotInGroup];

  if (groupIndex === 0 && FUZHOU_LOCAL_IMAGES[slotInGroup]) {
    const fuzhouMeta = FUZHOU_LOCAL_IMAGES[slotInGroup];
    const title = fuzhouMeta.title;
    return {
      image: fuzhouMeta.imagePath,
      text: `${short} · ${slotInGroup + 1}`,
      title,
      description: getDescriptionByCityAndSlot(short, slotInGroup + 1, title),
    };
  }

  if (localMeta) {
    const title = localMeta.title;
    return {
      image: localMeta.imagePath,
      text: `${short} · ${slotInGroup + 1}`,
      title,
      description: getDescriptionByCityAndSlot(short, slotInGroup + 1, title),
    };
  }

  const seed = groupIndex * 10 + slotInGroup + 1;
  const title = `${short} · ${slotInGroup + 1}`;
  return {
    image: `https://picsum.photos/seed/fj${seed}/800/600?grayscale`,
    text: `${short} · ${slotInGroup + 1}`,
    title,
    description: getDescriptionByCityAndSlot(short, slotInGroup + 1, title),
  };
}

/** 按组顺序展平：组0三张、组1三张 … 共 27 条，供 CircularGallery 使用（不再二次 concat） */
export const HOME_GALLERY_ITEMS: HomeGalleryItem[] = Array.from({ length: FUJIAN_CITY_NAMES.length }, (_, g) =>
  [0, 1, 2].map((s) => cardForGroup(g, s)),
).flat();
