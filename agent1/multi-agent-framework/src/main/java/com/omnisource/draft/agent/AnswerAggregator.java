package com.omnisource.draft.agent;

import com.omnisource.draft.model.AgentReply;
import java.util.List;

public class AnswerAggregator {

    public String aggregate(List<AgentReply> replies) {
        StringBuilder builder = new StringBuilder();
        builder.append("综合结论：\n");
        for (AgentReply reply : replies) {
            builder.append("[").append(reply.getTitle()).append("] ");
            builder.append(reply.getContent()).append("\n");
        }
        return builder.toString().trim();
    }
}
