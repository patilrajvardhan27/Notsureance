package com.notsureance.claims.agent.eval;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notsureance.claims.claim.ClaimRequests.FileClaim;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.policy.Equipment;
import com.notsureance.claims.policy.EquipmentRepository;
import com.notsureance.claims.policy.Policy;
import com.notsureance.claims.policy.PolicyRepository;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.AppUser;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Files each case as a normal claim by the policy's own agent, so the agent sees it as it would a real one. */
@Component
public class EvalCaseFiler {

    private final ObjectMapper json;
    private final ClaimService claims;
    private final PolicyRepository policies;
    private final EquipmentRepository equipment;
    private final Clock clock;

    public EvalCaseFiler(ObjectMapper json, ClaimService claims, PolicyRepository policies,
                         EquipmentRepository equipment, Clock clock) {
        this.json = json;
        this.claims = claims;
        this.policies = policies;
        this.equipment = equipment;
        this.clock = clock;
    }

    public List<EvalCase> cases() {
        try (InputStream in = new ClassPathResource("agent-eval/cases.json").getInputStream()) {
            return json.readValue(in, new TypeReference<List<EvalCase>>() {
            });
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Transactional
    public Long file(EvalCase evalCase) {
        Policy policy = policies.findAll().stream().filter(p -> p.getPolicyNumber().equals(evalCase.policy()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Unknown policy " + evalCase.policy()));
        Equipment machine = equipment.findByPolicyIdOrderByModelYearDesc(policy.getId()).stream()
                .filter(e -> e.getSerialNumber().equals(evalCase.serial())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Unknown serial " + evalCase.serial()));
        LocalDate incident = LocalDate.now(clock).minusDays(evalCase.daysAgo());
        FileClaim request = new FileClaim(policy.getId(), machine.getId(), evalCase.cause(), incident,
                evalCase.description(), evalCase.loss(), null, null, false, evalCase.dealer());
        AppUser owner = policy.getAgent();
        var agent = new AuthenticatedUser(owner.getId(), owner.getEmail(), owner.getFullName(), owner.getRole());
        return claims.file(request, agent).summary().id();
    }
}
