package com.notsureance.claims.document;

import com.notsureance.claims.security.AuthenticatedUser;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/claims/{claimId}/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentView upload(@PathVariable Long claimId, @RequestParam("file") MultipartFile file,
                               @AuthenticationPrincipal AuthenticatedUser user) throws IOException {
        return documentService.upload(claimId, file, user);
    }

    @GetMapping
    public List<DocumentView> list(@PathVariable Long claimId, @AuthenticationPrincipal AuthenticatedUser user) {
        return documentService.list(claimId, user);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<byte[]> download(@PathVariable Long claimId, @PathVariable Long documentId,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        ClaimDocument document = documentService.download(claimId, documentId, user);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFileName()).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(document.getContent());
    }
}
