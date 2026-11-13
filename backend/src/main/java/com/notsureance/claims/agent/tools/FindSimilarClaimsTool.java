package com.notsureance.claims.agent.tools;

import com.notsureance.claims.agent.AgentToolContext;
import com.notsureance.claims.ml.insight.SimilarClaimsService;
import com.notsureance.claims.ml.insight.SimilarClaimsService.Neighborhood;
import com.notsureance.claims.ml.insight.SimilarClaimsService.SimilarClaim;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FindSimilarClaimsTool implements AgentTool {

    private final SimilarClaimsService similar;

    public FindSimilarClaimsTool(SimilarClaimsService similar) {
        this.similar = similar;
    }

    @Tool(name = "findSimilarClaims", description = "The k most similar already decided claims (k-nearest neighbours on "
            + "loss size, loss-to-value, equipment age, cause and equipment type) with their outcomes.")
    public Neighborhood findSimilarClaims(@ToolParam(description = "Numeric claim id") long claimId,
                                          @ToolParam(description = "How many neighbours, 1 to 5") int k,
                                          ToolContext toolContext) {
        AgentToolContext agent = AgentToolContext.from(toolContext);
        Neighborhood near = similar.nearest(claimId, agent.user());
        var neighbors = near.neighbors().stream().limit(Math.max(1, Math.min(k, 5))).toList();
        agent.allowCitations(neighbors.stream().map(SimilarClaim::claimNumber).toList());
        return new Neighborhood(neighbors, near.approvalRate(), near.medianPayoutRatio());
    }
}
