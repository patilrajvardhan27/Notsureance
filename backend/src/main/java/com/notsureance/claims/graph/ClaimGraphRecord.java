package com.notsureance.claims.graph;

import com.notsureance.claims.claim.Claim;
import com.notsureance.claims.policy.Policy;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

record ClaimGraphRecord(Map<String, Object> parameters) {

    static ClaimGraphRecord from(Claim claim) {
        Policy policy = claim.getPolicy();
        Map<String, Object> p = new HashMap<>();
        p.put("claimId", claim.getId());
        p.put("claimNumber", claim.getClaimNumber());
        p.put("amount", claim.getEstimatedLoss().doubleValue());
        p.put("status", claim.getStatus().name());
        p.put("flagged", claim.isConfirmedFraud());
        p.put("policyNumber", policy.getPolicyNumber());
        p.put("holderKey", policy.getHolderEmail().toLowerCase(Locale.ROOT));
        p.put("holderName", policy.getHolderName());
        p.put("serial", claim.getEquipment().getSerialNumber());
        p.put("agentId", claim.getFiledBy().getId());
        p.put("agentName", claim.getFiledBy().getFullName());
        p.put("phone", policy.getHolderPhone());
        p.put("phoneDisplay", maskTail(policy.getHolderPhone(), "phone ending "));
        p.put("bankHash", policy.getPayoutAccountHash());
        p.put("bankDisplay", maskTail(policy.getPayoutAccountHash(), "account ref "));
        String dealer = claim.getRepairDealer();
        p.put("dealer", dealer);
        p.put("dealerKey", dealer == null ? null : dealer.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
        return new ClaimGraphRecord(p);
    }

    // Only the last four characters ever reach the graph's display fields or an answer built from them.
    private static String maskTail(String value, String prefix) {
        if (value == null || value.length() < 4) {
            return null;
        }
        return prefix + value.substring(value.length() - 4);
    }
}
