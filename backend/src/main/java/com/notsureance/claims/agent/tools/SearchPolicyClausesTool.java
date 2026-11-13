package com.notsureance.claims.agent.tools;

import com.notsureance.claims.agent.AgentToolContext;
import com.notsureance.claims.rag.PolicyClauseRepository.ClauseHit;
import com.notsureance.claims.rag.PolicyClauseSearch;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class SearchPolicyClausesTool implements AgentTool {

    private static final int RESULTS = 4;

    private final PolicyClauseSearch search;

    public SearchPolicyClausesTool(PolicyClauseSearch search) {
        this.search = search;
    }

    @Tool(name = "searchPolicyClauses", description = "Search the farm equipment policy wording. Returns the closest "
            + "clauses with their clause ids, for example FEC-3.4. Ask one specific coverage question per call.")
    public List<ClauseHit> searchPolicyClauses(@ToolParam(description = "A coverage question in plain words") String question,
                                               ToolContext toolContext) {
        AgentToolContext agent = AgentToolContext.from(toolContext);
        List<ClauseHit> hits = search.search(question, RESULTS);
        agent.allowCitations(hits.stream().map(ClauseHit::clauseId).toList());
        return hits;
    }
}
