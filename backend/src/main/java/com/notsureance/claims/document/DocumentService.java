package com.notsureance.claims.document;

import com.notsureance.claims.claim.Claim;
import com.notsureance.claims.claim.ClaimEvent;
import com.notsureance.claims.claim.ClaimEventRepository;
import com.notsureance.claims.claim.ClaimEventType;
import com.notsureance.claims.claim.ClaimService;
import com.notsureance.claims.common.BusinessRuleException;
import com.notsureance.claims.common.NotFoundException;
import com.notsureance.claims.security.AuthenticatedUser;
import com.notsureance.claims.user.AppUser;
import com.notsureance.claims.user.UserRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class DocumentService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final int MAX_DOCUMENTS_PER_CLAIM = 20;

    private final ClaimDocumentRepository documents;
    private final ClaimService claimService;
    private final ClaimEventRepository events;
    private final UserRepository users;

    public DocumentService(ClaimDocumentRepository documents, ClaimService claimService, ClaimEventRepository events,
                           UserRepository users) {
        this.documents = documents;
        this.claimService = claimService;
        this.events = events;
        this.users = users;
    }

    public DocumentView upload(Long claimId, MultipartFile file, AuthenticatedUser user) throws IOException {
        Claim claim = claimService.visibleClaim(claimId, user);
        if (file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BusinessRuleException("Files must be between 1 byte and 5 MB");
        }
        if (documents.countByClaimId(claimId) >= MAX_DOCUMENTS_PER_CLAIM) {
            throw new BusinessRuleException("This claim already has the maximum number of documents");
        }
        byte[] content = file.getBytes();
        FileSignature signature = FileSignature.detect(content)
                .orElseThrow(() -> new BusinessRuleException("Only JPEG, PNG and PDF files are accepted"));
        String digest = sha256(content);
        if (documents.existsByClaimIdAndSha256(claimId, digest)) {
            throw new BusinessRuleException("This file is already attached to the claim");
        }
        AppUser uploader = users.getReferenceById(user.id());
        ClaimDocument saved = documents.save(new ClaimDocument(claim, uploader, safeName(file.getOriginalFilename()),
                signature.contentType(), content, digest));
        events.save(new ClaimEvent(claim, uploader, ClaimEventType.DOCUMENT_ADDED, null, null, saved.getFileName()));
        return DocumentView.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DocumentView> list(Long claimId, AuthenticatedUser user) {
        claimService.visibleClaim(claimId, user);
        return documents.findByClaimIdOrderByCreatedAtDesc(claimId).stream().map(DocumentView::from).toList();
    }

    @Transactional(readOnly = true)
    public ClaimDocument download(Long claimId, Long documentId, AuthenticatedUser user) {
        claimService.visibleClaim(claimId, user);
        ClaimDocument document = documents.findByIdAndClaimId(documentId, claimId)
                .orElseThrow(() -> new NotFoundException("Document", documentId));
        document.getContent();
        return document;
    }

    static String safeName(String original) {
        String name = original == null ? "upload" : original.replaceAll(".*[/\\\\]", "");
        name = name.replaceAll("[^A-Za-z0-9._ -]", "_").strip();
        if (name.isEmpty() || name.startsWith(".")) {
            name = "upload" + name;
        }
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
