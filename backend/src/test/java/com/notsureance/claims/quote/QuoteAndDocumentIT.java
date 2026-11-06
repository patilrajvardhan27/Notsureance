package com.notsureance.claims.quote;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.notsureance.claims.support.IntegrationTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class QuoteAndDocumentIT extends IntegrationTest {

    @Test
    void anyoneCanGetAQuoteButLeadsNeedConsent() throws Exception {
        mvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(toJson(quote())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.annualPremium").isNumber())
                .andExpect(jsonPath("$.suggestions").isArray());

        Map<String, Object> lead = new HashMap<>(Map.of("quote", quote(), "fullName", "Jo Miller",
                "email", "jo@millerfarm.com", "consentToContact", false));
        mvc.perform(post("/api/quotes/leads").contentType(MediaType.APPLICATION_JSON).content(toJson(lead)))
                .andExpect(status().isBadRequest());
        lead.put("consentToContact", true);
        mvc.perform(post("/api/quotes/leads").contentType(MediaType.APPLICATION_JSON).content(toJson(lead)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedAgent").isString());
    }

    @Test
    void uploadsAreCheckedByContentNotByName() throws Exception {
        String agent = tokenFor(AGENT);
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
        mvc.perform(multipart("/api/claims/7/documents")
                        .file(new MockMultipartFile("file", "hail.png", "image/png", png))
                        .header("Authorization", agent))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"));

        mvc.perform(multipart("/api/claims/7/documents")
                        .file(new MockMultipartFile("file", "photo.png", "image/png", "MZ fake exe".getBytes()))
                        .header("Authorization", agent))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(get("/api/claims/7/documents").header("Authorization", tokenFor(OTHER_AGENT)))
                .andExpect(status().isNotFound());
    }

    private static Map<String, Object> quote() {
        return Map.of("category", "TRACTOR", "equipmentValue", 250000, "modelYear", 2019, "stateCode", "NE",
                "deductible", 2500, "gpsTracked", false, "safetyTraining", true, "otherInsuredUnits", 2,
                "claimFreeYears", 3, "bundleFarmPolicy", false);
    }
}
