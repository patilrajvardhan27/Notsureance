package com.notsureance.claims.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FileSafetyTest {

    @Test
    void detectsRealFileTypesFromContent() {
        assertThat(FileSignature.detect(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00})).contains(FileSignature.JPEG);
        assertThat(FileSignature.detect("%PDF-1.7".getBytes())).contains(FileSignature.PDF);
        assertThat(FileSignature.detect("MZ\u0090\u0000".getBytes())).isEmpty();
    }

    @Test
    void stripsPathsAndOddCharactersFromNames() {
        assertThat(DocumentService.safeName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(DocumentService.safeName("C:\\photos\\hail <1>.jpg")).isEqualTo("hail _1_.jpg");
        assertThat(DocumentService.safeName(".htaccess")).isEqualTo("upload.htaccess");
        assertThat(DocumentService.safeName(null)).isEqualTo("upload");
    }
}
