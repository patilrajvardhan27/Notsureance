package com.notsureance.claims.claim;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimEventRepository extends JpaRepository<ClaimEvent, Long> {

    @EntityGraph(attributePaths = "actor")
    List<ClaimEvent> findByClaimIdOrderByCreatedAtAscIdAsc(Long claimId);
}
