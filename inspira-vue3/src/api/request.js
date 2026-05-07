/**
 * 调用 StraitsCup 后端统一 Result 封装接口。
 * @param {string} path 以 /api 开头的路径
 * @param {RequestInit} [init]
 */
export async function apiFetch(path, init = {}) {
  const res = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers || {}),
    },
  });
  const body = await res.json().catch(() => null);
  if (!body || typeof body.code !== 'number') {
    throw new Error(res.ok ? '接口返回格式异常' : `HTTP ${res.status}`);
  }
  if (body.code !== 200) {
    throw new Error(body.message || `请求失败 (${body.code})`);
  }
  return body.data;
}
