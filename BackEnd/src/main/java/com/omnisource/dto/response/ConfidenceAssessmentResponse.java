package com.omnisource.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfidenceAssessmentResponse {

    private double score;
    private double coverageRate;
    private double freshnessRate;
    private double consistencyRate;
    private String level;
}
