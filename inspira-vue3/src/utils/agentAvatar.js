const OSS_BASE_URL = 'https://java-ai-fzu.oss-cn-beijing.aliyuncs.com/OmniSource/';
const APPEARANCE_BASE_URL = `${OSS_BASE_URL}appearance/`;

export function normalizeAgentAvatar(avatar) {
  if (!avatar) {
    return '';
  }

  if (/^(https?:)?\/\//i.test(avatar) || avatar.startsWith('data:') || avatar.startsWith('/')) {
    return avatar;
  }

  if (avatar.startsWith('appearance/')) {
    return `${OSS_BASE_URL}${avatar}`;
  }

  if (avatar.startsWith('OmniSource/')) {
    return `${OSS_BASE_URL}${avatar.replace(/^OmniSource\//, '')}`;
  }

  return `${APPEARANCE_BASE_URL}${avatar.replace(/^\/+/, '')}`;
}
