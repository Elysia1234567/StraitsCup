/**
 * 同源小程序对接 OmniSource 后端（StraitsCup/BackEnd）。
 * 默认 AI 单轮对话：POST /api/aigc/chat
 */

/**
 * @returns {string} 去掉末尾斜杠的 Base URL
 */
export function getApiBase() {
  const raw = import.meta.env.VITE_API_BASE_URL
  const s = raw != null && String(raw).trim() ? String(raw).trim() : 'http://127.0.0.1:8081'
  return s.replace(/\/$/, '')
}

/**
 * 普通 AI 问答（统一 Result，data 为字符串）
 * @param {string} message
 * @returns {Promise<string>}
 */
export function postAigcChat(message) {
  const url = `${getApiBase()}/api/aigc/chat`
  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method: 'POST',
      data: { message },
      header: {
        'Content-Type': 'application/json',
      },
      timeout: 120000,
      success(res) {
        const body = res.data
        const status = res.statusCode
        if (status !== 200) {
          reject(new Error(`服务返回 HTTP ${status}`))
          return
        }
        if (body && typeof body === 'object' && typeof body.code === 'number') {
          if (body.code === 200) {
            resolve(body.data == null ? '' : String(body.data))
          } else {
            reject(new Error(body.message || `业务错误 ${body.code}`))
          }
          return
        }
        reject(new Error('响应格式异常，请确认后端为 POST /api/aigc/chat（JSON）'))
      },
      fail(err) {
        const m =
          err && (err.errMsg || err.message) ? String(err.errMsg || err.message) : '网络请求失败'
        reject(new Error(m))
      },
    })
  })
}
