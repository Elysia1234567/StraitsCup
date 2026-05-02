package com.omnisource.controller;

import com.omnisource.entity.Agent;
import com.omnisource.service.AgentService;
import com.omnisource.utils.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public Result<List<Agent>> getAllAgents() {
        return Result.success(agentService.getAllActiveAgents());
    }

    @GetMapping("/{code}")
    public Result<Agent> getAgentByCode(@PathVariable String code) {
        Agent agent = agentService.getAgentByCode(code);
        return agent != null ? Result.success(agent) : Result.notFound("Agent不存在");
    }
}
