package com.notsureance.claims.agent.tools;

import com.notsureance.claims.agent.AgentToolContext;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.ml.scoring.ClaimAssessment;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class GetScoresTool implements AgentTool {

    private final ClaimService claims;

    public GetScoresTool(ClaimService claims) {
        this.claims = claims;
    }

    public record Scores(String claimNumber, ClaimAssessment assessment, String statedCause) {
    }

    @Tool(name = "getScores", description = "Model scores for a claim: calibrated fraud probability with its top reasons, "
            + "expected payout, loss anomaly z-score, the cause read from the description, triage lane and priority.")
    public Scores getScores(@ToolParam(description = "Numeric claim id") long claimId, ToolContext toolContext) {
        AgentToolContext agent = AgentToolContext.from(toolContext);
        ClaimDetail detail = claims.detail(claimId, agent.user());
        agent.allowCitation(detail.summary().claimNumber());
        return new Scores(detail.summary().claimNumber(), detail.assessment(), detail.summary().cause().name());
    }
}
