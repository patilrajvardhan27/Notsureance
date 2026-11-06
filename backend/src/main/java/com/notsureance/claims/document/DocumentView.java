package com.notsureance.claims.document;

import java.time.Instant;

public record DocumentView(Long id, String fileName, String contentType, long sizeBytes, String uploadedBy,
                           Instant uploadedAt) {

    static DocumentView from(ClaimDocument d) {
        return new DocumentView(d.getId(), d.getFileName(), d.getContentType(), d.getSizeBytes(),
                d.getUploadedBy().getFullName(), d.getCreatedAt());
    }
}
