package com.notsureance.claims.rag;

import com.notsureance.claims.rag.PolicyClauseRepository.ClauseHit;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class PolicyClauseController {

    private final PolicyClauseSearch search;

    public PolicyClauseController(PolicyClauseSearch search) {
        this.search = search;
    }

    @GetMapping("/api/policy-clauses/search")
    public List<ClauseHit> search(@RequestParam @NotBlank @Size(max = 300) String q,
                                  @RequestParam(defaultValue = "5") @Min(1) @Max(8) int k) {
        return search.search(q, k);
    }
}
