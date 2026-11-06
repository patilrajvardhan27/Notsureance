package com.notsureance.claims.document;

import java.util.Arrays;
import java.util.Optional;

enum FileSignature {
    JPEG("image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}),
    PNG("image/png", new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    PDF("application/pdf", new byte[] {0x25, 0x50, 0x44, 0x46, 0x2D});

    private final String contentType;
    private final byte[] magic;

    FileSignature(String contentType, byte[] magic) {
        this.contentType = contentType;
        this.magic = magic;
    }

    String contentType() {
        return contentType;
    }

    static Optional<FileSignature> detect(byte[] content) {
        return Arrays.stream(values())
                .filter(sig -> content.length >= sig.magic.length
                        && Arrays.equals(Arrays.copyOf(content, sig.magic.length), sig.magic))
                .findFirst();
    }
}
