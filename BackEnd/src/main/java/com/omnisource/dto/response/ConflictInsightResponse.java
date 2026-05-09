package com.omnisource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictInsightResponse {

    private boolean conflictDetected;
    private List<String> divergencePoints;
    private List<String> verificationPoints;
    private List<String> conflictingAgents;
}
