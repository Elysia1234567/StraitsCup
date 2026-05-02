package com.omnisource.service;

import com.omnisource.entity.Agent;
import com.omnisource.entity.ChatRoomMember;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final StringRedisTemplate redisTemplate;
    private final ChatRoomService chatRoomService;
    private final AgentService agentService;

    private static final String COOLDOWN_KEY_PREFIX = "chat:agent:cooldown:";

    public List<Agent> selectRespondingAgents(Long roomId, String userMessage, List<Agent> allAgents) {
        List<Agent> available = allAgents.stream()
                .filter(a -> canAgentSpeak(roomId, a.getId()))
                .collect(Collectors.toList());

        if (available.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Agent, Double> scores = available.stream()
                .collect(Collectors.toMap(
                        a -> a,
                        a -> calculateRelevance(a, userMessage)
                ));

        List<Agent> selected = scores.entrySet().stream()
                .sorted(Map.Entry.<Agent, Double>comparingByValue().reversed())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (selected.size() < 2 && available.size() > selected.size()) {
            List<Agent> remaining = available.stream()
                    .filter(a -> !selected.contains(a))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining);
            selected.addAll(remaining.stream().limit(2 - selected.size()).collect(Collectors.toList()));
        }

        return selected.stream().limit(3).collect(Collectors.toList());
    }

    public boolean canAgentSpeak(Long roomId, Long agentId) {
        String key = COOLDOWN_KEY_PREFIX + roomId + ":" + agentId;
        String lastSpeak = redisTemplate.opsForValue().get(key);
        if (lastSpeak == null) return true;

        try {
            long lastTime = Long.parseLong(lastSpeak);
            return System.currentTimeMillis() - lastTime > 30000;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public void recordAgentSpeak(Long roomId, Long agentId) {
        String key = COOLDOWN_KEY_PREFIX + roomId + ":" + agentId;
        redisTemplate.opsForValue().set(key, String.valueOf(System.currentTimeMillis()), 60, TimeUnit.SECONDS);
    }

    private double calculateRelevance(Agent agent, String message) {
        if (agent.getKnowledgeScope() == null) return 0.5;
        String scope = agent.getKnowledgeScope().toLowerCase();
        String msg = message.toLowerCase();

        int matchCount = 0;
        for (String word : msg.split("[，。！？、\\s]+")) {
            if (word.length() > 1 && scope.contains(word)) {
                matchCount++;
            }
        }
        return Math.min(0.3 + matchCount * 0.2, 1.0);
    }
}
