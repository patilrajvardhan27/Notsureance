package com.notsureance.claims.agent.eval;

import com.notsureance.claims.agent.ProposedAction;
import com.notsureance.claims.claim.LossCause;
import java.math.BigDecimal;

public record EvalCase(String id, String policy, String serial, LossCause cause, int daysAgo, BigDecimal loss,
                       ProposedAction expected, String dealer, String description) {
}
