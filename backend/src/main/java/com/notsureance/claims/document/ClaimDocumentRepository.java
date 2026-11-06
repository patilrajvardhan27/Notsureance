package com.notsureance.claims.document;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, Long> {

    @EntityGraph(attributePaths = "uploadedBy")
    List<ClaimDocument> findByClaimIdOrderByCreatedAtDesc(Long claimId);

    Optional<ClaimDocument> findByIdAndClaimId(Long id, Long claimId);

    boolean existsByClaimIdAndSha256(Long claimId, String sha256);

    long countByClaimId(Long claimId);
}
