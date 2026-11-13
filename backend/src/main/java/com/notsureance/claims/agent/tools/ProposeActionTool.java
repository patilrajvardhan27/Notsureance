package com.notsureance.claims.agent.tools;

import com.notsureance.claims.agent.AgentProposalService;
import com.notsureance.claims.agent.AgentToolContext;
import com.notsureance.claims.agent.ProposedAction;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class ProposeActionTool implements AgentTool {

    private final AgentProposalService proposals;

    public ProposeActionTool(AgentProposalService proposals) {
        this.proposals = proposals;
    }

    @Tool(name = "proposeAction", description = "Record your recommendation for this claim. It is saved as a pending "
            + "proposal that an adjuster must approve; it does not change the claim. Call it once.")
    public String proposeAction(@ToolParam(description = "Numeric claim id of the claim under review") long claimId,
                                @ToolParam(description = "FAST_TRACK, REQUEST_INFO or INVESTIGATE") ProposedAction action,
                                @ToolParam(description = "One sentence that cites its sources") String reason,
                                ToolContext toolContext) {
        Long id = proposals.propose(AgentToolContext.from(toolContext), claimId, action, reason);
        return "Saved proposal " + id + " as pending. An adjuster will approve or reject it.";
    }
}
