package com.notsureance.claims.agent.tools;

import com.notsureance.claims.agent.AgentToolContext;
import com.notsureance.claims.graph.FraudGraphService;
import com.notsureance.claims.graph.GraphUnavailableException;
import com.notsureance.claims.graph.GraphViews.LinkedParties;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class FindLinkedPartiesTool implements AgentTool {

    private final FraudGraphService graph;

    public FindLinkedPartiesTool(FraudGraphService graph) {
        this.graph = graph;
    }

    @Tool(name = "findLinkedParties", description = "Other claimants within two hops of this claim's policyholder through "
            + "a shared phone, payout account or repair shop, with their claim and confirmed fraud counts. "
            + "Identifiers are masked.")
    public LinkedParties findLinkedParties(@ToolParam(description = "Numeric claim id") long claimId,
                                           ToolContext toolContext) {
        AgentToolContext agent = AgentToolContext.from(toolContext);
        try {
            return graph.linkedParties(claimId, agent.user());
        } catch (GraphUnavailableException ex) {
            return new LinkedParties("unavailable", List.of(), 0);
        }
    }
}
