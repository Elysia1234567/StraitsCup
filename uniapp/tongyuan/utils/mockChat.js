/**
 * 后端不可用时：首页聊天室列表与对话页的离线演示数据。
 * 至少 3 个会话，每段 ≥3 轮问答（用户一句 + 器灵一句为一轮）。
 */

export function isMockRoomId(roomId) {
  return roomId != null && String(roomId).startsWith('mock-')
}

/** 首页列表（roomId 用于 storage / chat） */
export function getMockRoomListForIndex() {
  return [
    {
      id: 'mock-heritage-craft',
      roomId: 'mock-heritage-craft',
      title: '福州软木画 · 纹样问询',
      content: '最后一条：要不要我把亭台的比例再压一点？',
      time: '示例 · 离线',
    },
    {
      id: 'mock-travel-food',
      roomId: 'mock-travel-food',
      title: '泉州Walk · 古城路线',
      content: '最后一条：二楼露台看红砖厝落日很棒。',
      time: '示例 · 离线',
    },
    {
      id: 'mock-multi-agent',
      roomId: 'mock-multi-agent',
      title: '器灵协作 · 非遗短片脚本',
      content: '最后一条：收尾镜头扫过匠人手背特写。',
      time: '示例 · 离线',
    },
  ]
}

function threadHeritage() {
  return [
    { type: 'user', content: '想了解福州软木画适合做什么纹样？家里玄关不大。' },
    {
      type: 'ai',
      content:
        '玄关偏小更适合竖幅山水或亭台剪影一类「取景紧凑」的纹样；层次控制在两到三层，木纹留白多一些会显得通透。',
      senderName: '器灵·榕',
    },
    { type: 'user', content: '配色呢？偏原木还是偏漆色点缀？' },
    {
      type: 'ai',
      content:
        '原木底色最接近栓皮气质；若墙面偏灰白，可在亭台轮廓外侧勾一圈浅漆线，亮度不要超过栓皮本体两度。',
      senderName: '器灵·榕',
    },
    { type: 'user', content: '定制周期一般要多久？我怕赶不上春节前挂上。' },
    {
      type: 'ai',
      content:
        '纹样确认后，常规体量大约三到四周；若只做单片小样可先一周出稿确认，再排全职雕刻档期。',
      senderName: '器灵·榕',
    },
    { type: 'user', content: '如果我给你.room 层高 2.7m，你能帮我估一个竖幅比例吗？' },
    {
      type: 'ai',
      content:
        '2.7m 层高建议主景高度占墙高 0.45～0.5，竖幅约 1.2m 内，上留天、下留地，这样抬头看不会「顶到檐角」。',
      senderName: '器灵·榕',
    },
  ]
}

function threadTravel() {
  return [
    { type: 'user', content: '只有大半天在泉州，想走「半城烟火半城仙」那种路线。' },
    {
      type: 'ai',
      content:
        '可以：上午开元寺—西街早市，中午吃一碗面线糊，下午关帝庙、清净寺一线，傍晚去天后宫听南音或看夕阳。',
      senderName: '器灵·鲤',
    },
    { type: 'user', content: '带老人，不想爬太多坡。' },
    {
      type: 'ai',
      content:
        '把「走巷」放在西街平路部分，关帝庙到清净寺是短平路；若累可跳过远一点的临漳门，把体力留给庙前广场坐一坐。',
      senderName: '器灵·鲤',
    },
    { type: 'user', content: '想顺便看德化白瓷，顺路吗？' },
    {
      type: 'ai',
      content:
        '市区当天往返德化略紧；更稳是另排一天到德化看瓷，泉州老城留在轻松散步的节奏里。',
      senderName: '器灵·鲤',
    },
    { type: 'user', content: '晚饭想吃闽南家常，不要排队两小时那种。' },
    {
      type: 'ai',
      content:
        '避开主街网红铺，拐进后半巷找夫妻小店：姜母鸭、五香卷、卤面任选其一，通常七点前到店更容易有位。',
      senderName: '器灵·鲤',
    },
  ]
}

function threadMultiAgent() {
  return [
    {
      type: 'user',
      content: '我们要拍一支 90 秒的非遗短片，主题是「手里的温度」，帮我列个大纲。',
    },
    {
      type: 'ai',
      content:
        '开篇特写手部粗糙纹理→中段交叉剪辑烧制/雕刻工序→高潮是成品交到参观者手里的停顿一秒→结尾字幕落在工坊门楣。',
      senderName: '编导器灵',
    },
    {
      type: 'user',
      content: '旁白想要克制一点，不要太煽情。',
    },
    {
      type: 'ai',
      content:
        '旁白用陈述句短句：谁在几点起火、谁在几点停刀；情绪留给画面里的蒸汽与粉尘，而不是形容词堆砌。',
      senderName: '编导器灵',
    },
    {
      type: 'user',
      content: '音乐呢？南音会不会太重？',
    },
    {
      type: 'ai',
      content:
        '南音可取一两句打击乐节奏打底，主旋律用极简钢琴或 ambient；高潮才让南音完整一句入场，避免从头铺满。',
      senderName: '声效器灵',
    },
    {
      type: 'user',
      content: '最后一个镜头我想回到匠人洗手的水龙头特写，会不会太文艺？',
    },
    {
      type: 'ai',
      content:
        '不矫情的前提是前后已经有足够工序铺垫；镜头停留不超过 1.5 秒，配一声很轻的环境音收尾即可。',
      senderName: '编导器灵',
    },
  ]
}

const THREADS = {
  'mock-heritage-craft': threadHeritage,
  'mock-travel-food': threadTravel,
  'mock-multi-agent': threadMultiAgent,
}

export function getMockHistoryMessages(roomId) {
  const key = String(roomId)
  const fn = THREADS[key]
  if (!fn) return []
  return fn().map((m) => ({
    type: m.type,
    content: m.content,
    senderName: m.senderName || '',
    imageUrl: '',
    streaming: false,
  }))
}

/** 演示模式下用户发送后的本地回显（不请求后端） */
export function getMockChatReply(roomId, userText) {
  const q = (userText || '').trim()
  const short = q.length > 18 ? `${q.slice(0, 18)}…` : q
  return `【离线演示】未连接服务器。您刚说「${short || '…'}」已记录在本地；连接后端后可继续真实对话。`
}
