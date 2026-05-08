import { normalizeAgentAvatar } from './agentAvatar.js';

const OSS_BASE_URL = 'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource';

const CITY_BY_PREFIX = {
  fz: '福州',
  xm: '厦门',
  qz: '泉州',
  zz: '漳州',
  pt: '莆田',
  np: '南平',
  sm: '三明',
  ly: '龙岩',
  nd: '宁德',
};

const APPEARANCE_FILE_BY_AGENT_CODE = {
  fz_shoushan_stone: '寿山石雕',
  fz_cork_scene: '福州软木画',
  fz_lacquerware: '脱胎漆器',
  xm_bead_embroidery: '厦门珠绣',
  xm_lacquer_thread: '漆线雕',
  xm_wangchuan: '送王船',
  qz_dehua_porcelain: '德化瓷',
  qz_paper_lantern: '刻纸花灯',
  qz_string_puppet: '提线木偶',
  zz_glove_puppet: '布袋木偶戏',
  zz_woodblock_print: '木版年画',
  zz_pien_tze_huang: '片仔癀',
  pt_puxian_opera: '莆仙戏',
  pt_silver_ornament: '莆田银饰',
  pt_longan_woodcarving: '龙眼木雕',
  np_jian_ware: '建阳建盏（黑釉瓷）',
  np_wuyi_tea: '武夷岩茶',
  np_nuo_mask: '邵武傩面具',
  sm_hakka_bamboo: '客家竹编',
  sm_danankeng_pottery: '将乐大南坑陶瓷',
  sm_mingxi_microcarving: '明溪微雕',
  ly_hakka_rice_wine: '客家米酒',
  ly_hakka_embroidery: '龙岩客家刺绣',
  ly_farmer_painting: '农民画',
  nd_zherong_papercut: '柘荣剪纸',
  nd_she_costume: '畲族服饰',
  nd_line_lion: '霍童线狮',
};

const SOURCE_FILE_BY_AGENT_CODE = {
  ...APPEARANCE_FILE_BY_AGENT_CODE,
};

function cityName(agentCode) {
  if (!agentCode || !agentCode.includes('_')) return '';
  return CITY_BY_PREFIX[agentCode.slice(0, agentCode.indexOf('_'))] || '';
}

function encodePathSegment(value) {
  return encodeURIComponent(value).replace(/%2F/gi, '/');
}

function ossImageUrl(folder, city, fileName) {
  return `${OSS_BASE_URL}/${folder}/${encodePathSegment(city)}/${encodePathSegment(fileName)}.png`;
}

export function buildAgentAppearanceUrl(agent) {
  if (!agent) return '';

  const city = cityName(agent.agentCode);
  const fileName = APPEARANCE_FILE_BY_AGENT_CODE[agent.agentCode];
  if (city && fileName) {
    return ossImageUrl('appearance', city, fileName);
  }

  const normalized = normalizeAgentAvatar(agent.avatar);
  if (normalized) return normalized;

  if (!city) return '';
  const name = agent.name || agent.agentCode;
  return ossImageUrl('appearance', city, name);
}

export function buildAgentSourceImageUrl(agent) {
  if (!agent?.agentCode) return '';
  const city = cityName(agent.agentCode);
  const fileName = SOURCE_FILE_BY_AGENT_CODE[agent.agentCode];
  if (!city || !fileName) return '';
  return ossImageUrl('source', city, fileName);
}

export function toAgentViewModel(agent) {
  const appearanceUrl = buildAgentAppearanceUrl(agent);
  const sourceImageUrl = buildAgentSourceImageUrl(agent);
  return {
    ...agent,
    avatar: appearanceUrl,
    appearanceUrl,
    sourceImageUrl,
    displayName: agent.name,
  };
}
