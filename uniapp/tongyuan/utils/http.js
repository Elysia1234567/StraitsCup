/**
 * OmniSource 后端 HTTP 封装（除 GET /api/aigc/chat 纯文本、SSE stream 外均为 JSON Result）
 */

export class ApiError extends Error {
  /**
   * @param {string} message
   * @param {number} [code]
   * @param {*} [data]
   */
  constructor(message, code, data) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.data = data
  }
}

export function getApiBase() {
  const raw = import.meta.env.VITE_API_BASE_URL
  const s = raw != null && String(raw).trim() ? String(raw).trim() : 'http://127.0.0.1:8081'
  return s.replace(/\/$/, '')
}

/** WebSocket 聊天室：/ws/chat?roomId= */
export function getWsChatUrl(roomId) {
  const b = getApiBase()
  const ws = b.startsWith('https://')
    ? `wss://${b.slice(8)}`
    : b.startsWith('http://')
      ? `ws://${b.slice(7)}`
      : b.replace(/^http/i, 'ws')
  return `${ws}/ws/chat?roomId=${encodeURIComponent(String(roomId))}`
}

export function getWsVoiceUrl() {
  const b = getApiBase()
  const ws = b.startsWith('https://')
    ? `wss://${b.slice(8)}`
    : b.startsWith('http://')
      ? `ws://${b.slice(7)}`
      : b.replace(/^http/i, 'ws')
  return `${ws}/ws/voice`
}

function buildQuery(query) {
  if (!query || typeof query !== 'object') return ''
  const parts = []
  Object.keys(query).forEach((k) => {
    const v = query[k]
    if (v === undefined || v === null) return
    parts.push(`${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
  })
  return parts.length ? `?${parts.join('&')}` : ''
}

/**
 * @param {{ method?: string, path: string, data?: any, query?: Record<string, string|number|boolean>, header?: Record<string, string>, timeout?: number }} opts
 * @returns {Promise<any>} 原始 response.data
 */
export function requestRaw(opts) {
  const { method = 'GET', path, data, query, header = {}, timeout = 120000 } = opts
  let url = `${getApiBase()}${path.startsWith('/') ? path : `/${path}`}`
  url += buildQuery(query)

  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data: method === 'GET' ? undefined : data,
      header: {
        ...(!['GET', 'DELETE'].includes(String(method).toUpperCase()) ? { 'Content-Type': 'application/json' } : {}),
        ...header,
      },
      timeout,
      success(res) {
        resolve(res)
      },
      fail(err) {
        const m =
          err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : '网络请求失败'
        reject(new ApiError(m))
      },
    })
  })
}

/**
 * 解析为 Result：code===200 时返回整包 body（含 data）
 * @returns {Promise<{ code: number, message: string, data: any }>}
 */
export async function requestResult(opts) {
  const res = await requestRaw(opts)
  const body = res.data
  const status = res.statusCode
  if (status !== 200) {
    throw new ApiError(`HTTP ${status}`, status, body)
  }
  if (!body || typeof body !== 'object' || typeof body.code !== 'number') {
    throw new ApiError('响应不是 JSON Result', status, body)
  }
  return body
}

/**
 * @returns {Promise<any>} 仅返回 data
 */
export async function requestData(opts) {
  const body = await requestResult(opts)
  if (body.code !== 200) {
    throw new ApiError(body.message || `业务错误 ${body.code}`, body.code, body.data)
  }
  return body.data
}

/**
 * GET /api/aigc/chat 纯文本
 * @param {string} message
 */
export async function getAigcChatPlain(message) {
  const res = await requestRaw({
    method: 'GET',
    path: '/api/aigc/chat',
    query: { message },
    timeout: 120000,
  })
  if (res.statusCode !== 200) {
    throw new ApiError(`HTTP ${res.statusCode}`, res.statusCode, res.data)
  }
  const d = res.data
  if (typeof d === 'string') return d
  if (d && typeof d === 'object' && typeof d.content === 'string') return d.content
  return d == null ? '' : String(d)
}

/**
 * SSE 流式地址（小程序无法原生消费 SSE，仅 H5/调试可用或自行分片请求）
 * @param {string} message
 */
export function getAigcStreamUrl(message) {
  return `${getApiBase()}/api/aigc/stream${buildQuery({ message })}`
}

/**
 * 上传图片 multipart，字段名 file（与 UploadController 一致）
 * @param {string} filePath 本地临时路径
 */
export function uploadImage(filePath) {
  const url = `${getApiBase()}/api/upload/image`
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url,
      filePath,
      name: 'file',
      timeout: 120000,
      success(res) {
        try {
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (body && body.code === 200) {
            resolve(body.data)
            return
          }
          if (body && body.message) {
            reject(new ApiError(body.message, body.code, body.data))
            return
          }
          reject(new ApiError('上传失败', res.statusCode, res.data))
        } catch (e) {
          reject(new ApiError(e.message || '解析上传响应失败'))
        }
      },
      fail(err) {
        const m =
          err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : '上传失败'
        reject(new ApiError(m))
      },
    })
  })
}
