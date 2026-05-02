package com.omnisource.Agents;

import com.omnisource.Agents.fuzhou.FuzhouCulturalAgents;
import com.omnisource.Agents.longyan.LongyanCulturalAgents;
import com.omnisource.Agents.nanping.NanpingCulturalAgents;
import com.omnisource.Agents.ningde.NingdeCulturalAgents;
import com.omnisource.Agents.putian.PutianCulturalAgents;
import com.omnisource.Agents.quanzhou.QuanzhouCulturalAgents;
import com.omnisource.Agents.sanming.SanmingCulturalAgents;
import com.omnisource.Agents.xiamen.XiamenCulturalAgents;
import com.omnisource.Agents.zhangzhou.ZhangzhouCulturalAgents;

import java.util.List;
import java.util.stream.Stream;

/**
 * 福建九市特色文化物品器灵注册表。
 */
public final class FujianCulturalAgentRegistry {

    public static final List<AgentDefinition> DEFINITIONS = Stream.of(
                    FuzhouCulturalAgents.DEFINITIONS,
                    XiamenCulturalAgents.DEFINITIONS,
                    QuanzhouCulturalAgents.DEFINITIONS,
                    ZhangzhouCulturalAgents.DEFINITIONS,
                    PutianCulturalAgents.DEFINITIONS,
                    NanpingCulturalAgents.DEFINITIONS,
                    SanmingCulturalAgents.DEFINITIONS,
                    LongyanCulturalAgents.DEFINITIONS,
                    NingdeCulturalAgents.DEFINITIONS
            )
            .flatMap(List::stream)
            .toList();

    private FujianCulturalAgentRegistry() {
    }
}
