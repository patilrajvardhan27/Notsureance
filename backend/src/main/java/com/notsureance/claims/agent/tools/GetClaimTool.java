package com.notsureance.claims.agent.tools;

import com.notsureance.claims.agent.AgentToolContext;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.claim.ClaimViews.ClaimDetail;
import com.notsureance.claims.document.DocumentService;
import com.notsureance.claims.document.DocumentView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class GetClaimTool implements AgentTool {

    private final ClaimService claims;
    private final DocumentService documents;

    public GetClaimTool(ClaimService claims, DocumentService documents) {
        this.claims = claims;
        this.documents = documents;
    }

    public record ClaimFacts(String claimNumber, String status, String cause, LocalDate incidentDate,
                             BigDecimal estimatedLoss, String description, String farm, String policyNumber,
                             BigDecimal coverageLimit, BigDecimal deductible, LocalDate policyStart,
                             String equipment, BigDecimal insuredValue, boolean gpsTracked, String repairShop,
                             List<String> timeline, List<String> documents) {
    }

    @Tool(name = "getClaim", description = "Read one claim: status, cause, dates, amounts, equipment, policy terms, "
            + "timeline notes and the names of uploaded documents.")
    public ClaimFacts getClaim(@ToolParam(description = "Numeric claim id") long claimId, ToolContext toolContext) {
        AgentToolContext agent = AgentToolContext.from(toolContext);
        ClaimDetail detail = claims.detail(claimId, agent.user());
        agent.allowCitation(detail.summary().claimNumber());
        List<String> timeline = detail.timeline().stream()
                .map(e -> e.type() + (e.toStatus() == null ? "" : " to " + e.toStatus())
                        + (e.note() == null ? "" : ": " + e.note()))
                .toList();
        List<String> files = documents.list(claimId, agent.user()).stream().map(DocumentView::fileName).toList();
        return new ClaimFacts(detail.summary().claimNumber(), detail.summary().status().name(),
                detail.summary().cause().name(), detail.summary().incidentDate(), detail.summary().estimatedLoss(),
                detail.description(), detail.policy().farmName(), detail.policy().policyNumber(),
                detail.policy().coverageLimit(), detail.policy().deductible(), detail.policy().effectiveDate(),
                detail.equipment().displayName(), detail.equipment().insuredValue(), detail.equipment().gpsTracked(),
                detail.repairDealer(), timeline, files);
    }
}
