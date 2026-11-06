package com.notsureance.claims.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ClaimRepository extends JpaRepository<Claim, Long>, JpaSpecificationExecutor<Claim> {

    @Override
    @EntityGraph(attributePaths = {"policy", "equipment", "filedBy", "assignedAdjuster"})
    Page<Claim> findAll(Specification<Claim> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"policy", "equipment", "filedBy", "assignedAdjuster"})
    List<Claim> findByStatusIn(Collection<ClaimStatus> statuses);

    @Query("select count(c) from Claim c where c.policy.id = :policyId and c.incidentDate >= :since and c.id <> :excludeId")
    int countPriorClaims(Long policyId, LocalDate since, Long excludeId);

    @Query("select c.status as status, count(c) as total from Claim c where (:agentId is null or c.filedBy.id = :agentId) "
            + "group by c.status")
    List<StatusCount> countByStatus(Long agentId);

    @Query("select c.score.lane as lane, count(c) as total from Claim c where c.status in :open "
            + "and (:agentId is null or c.filedBy.id = :agentId) group by c.score.lane")
    List<LaneCount> countOpenByLane(Collection<ClaimStatus> open, Long agentId);

    @Query("select coalesce(sum(c.approvedAmount), 0) from Claim c where c.status in :paidOrApproved "
            + "and (:agentId is null or c.filedBy.id = :agentId)")
    BigDecimal sumApproved(Collection<ClaimStatus> paidOrApproved, Long agentId);

    @Query("select c.policy.id as policyId, count(c) as claimCount, coalesce(avg(c.score.fraudScore), 0) as averageFraud "
            + "from Claim c group by c.policy.id")
    List<PolicyClaimStats> claimStatsByPolicy();

    interface PolicyClaimStats {
        Long getPolicyId();

        long getClaimCount();

        double getAverageFraud();
    }

    interface StatusCount {
        ClaimStatus getStatus();

        long getTotal();
    }

    interface LaneCount {
        TriageLane getLane();

        long getTotal();
    }
}
