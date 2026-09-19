package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.controller.StatisticalContractController;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.access.AuthoringFileService;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow.Authority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.base.api.service.platform.statistical.StatisticalContractFixtures.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP semantics of the authoring surface: RFC 9110 preconditions, idempotent create, RFC 9457 problems. */
class StatisticalContractHttpTest {
    private static final String BASE = "/platform/products/LABOUR_PRODUCT/statistical-contracts";
    private static final TestingAuthenticationToken AUTHOR = user("author"), APPROVER = user("approver"), STRANGER = user("stranger");
    private final ObjectMapper mapper = new ObjectMapper();
    private MockMvc mvc;

    private static TestingAuthenticationToken user(String name) {
        TestingAuthenticationToken token = new TestingAuthenticationToken(name, "n/a");
        token.setAuthenticated(true);
        return token;
    }

    @BeforeEach void wire() {
        Map<String, Set<Authority>> grants = Map.of("author|LABOUR_PRODUCT", Set.of(Authority.AUTHOR), "approver|LABOUR_PRODUCT", Set.of(Authority.APPROVE),
                "stranger|LAND_PRODUCT", Set.of(Authority.AUTHOR));
        ContractWorkflow workflow = new ContractWorkflow(new ContractWorkflowTest.InMemoryStore(), compiler(registry()),
                (actor, product, authority) -> grants.getOrDefault(actor.subject() + "|" + product, Set.of()).contains(authority),
                new ContractWorkflow.Policy(false), () -> UUID.randomUUID().toString());
        var registry = registry();
        AuthoringFileService files = new AuthoringFileService(workflow, registry,
                (ref, language) -> registry.codelist(ref, LABOUR).orElseThrow().codes().stream().sorted().map(c -> new AccessAuthoringAdapter.CodeItem(c, c)).toList(),
                "OBS_STATUS", 3_000_000);
        mvc = MockMvcBuilders.standaloneSetup(new StatisticalContractController(workflow, files, null)).build();
    }

    private JsonNode body(MvcResult result) throws Exception { return mapper.readTree(result.getResponse().getContentAsString()); }

    private MvcResult create(String key) throws Exception {
        return mvc.perform(post(BASE).principal(AUTHOR).header("Idempotency-Key", key).contentType(MediaType.APPLICATION_JSON).content(labourDraft())).andReturn();
    }

    @Test void fullAuthoringFlowOverHttp() throws Exception {
        MvcResult created = create("k1");
        assertEquals(201, created.getResponse().getStatus());
        assertEquals("\"1\"", created.getResponse().getHeader("ETag"));
        assertEquals("no-store", created.getResponse().getHeader("Cache-Control"));
        String id = body(created).get("id").asText();

        mvc.perform(get(BASE + "/" + id + "/preview").principal(AUTHOR)).andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(true)).andExpect(jsonPath("$.plan.semanticDigest").isNotEmpty());

        MvcResult submitted = mvc.perform(post(BASE + "/" + id + "/submit").principal(AUTHOR).header("If-Match", "\"1\"")).andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"2\"")).andExpect(jsonPath("$.state").value("REVIEW_REQUIRED")).andReturn();
        String digest = body(submitted).get("revisionDigest").asText();

        mvc.perform(post(BASE + "/" + id + "/approve").principal(APPROVER).header("If-Match", "\"2\"").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"revisionDigest\":\"" + digest + "\"}"))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"3\"")).andExpect(jsonPath("$.contract.state").value("APPROVED"))
                .andExpect(jsonPath("$.contract.decidedBy").value("approver"));
    }

    @Test void createIsIdempotentAndRequiresAKey() throws Exception {
        String first = body(create("same")).get("id").asText();
        assertEquals(first, body(create("same")).get("id").asText());
        mvc.perform(post(BASE).principal(AUTHOR).header("Idempotency-Key", "same").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("statistical-contract-idempotency-conflict"));
        mvc.perform(post(BASE).principal(AUTHOR).contentType(MediaType.APPLICATION_JSON).content(labourDraft()))
                .andExpect(status().isPreconditionRequired());
    }

    @Test void mutationsNeedIfMatchAndStaleOrWeakValidatorsFail() throws Exception {
        String id = body(create("k")).get("id").asText();
        mvc.perform(put(BASE + "/" + id).principal(AUTHOR).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isPreconditionRequired()).andExpect(header().string("Content-Type", "application/problem+json"));
        mvc.perform(put(BASE + "/" + id).principal(AUTHOR).header("If-Match", "\"1\"").contentType(MediaType.APPLICATION_JSON).content(labourDraft()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\""));
        for (String stale : new String[]{"\"1\"", "W/\"2\"", "*", "2"})
            mvc.perform(put(BASE + "/" + id).principal(AUTHOR).header("If-Match", stale).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isPreconditionFailed());
    }

    @Test void problemsAreMachineReadableAndCarryCompilerFindings() throws Exception {
        MvcResult created = mvc.perform(post(BASE).principal(AUTHOR).header("Idempotency-Key", "bad").contentType(MediaType.APPLICATION_JSON)
                .content(labourDraft().replace("\"sourceProfile\"", "\"sql\":\"x\",\"sourceProfile\""))).andReturn();
        String id = body(created).get("id").asText();
        MvcResult rejected = mvc.perform(post(BASE + "/" + id + "/submit").principal(AUTHOR).header("If-Match", "\"1\""))
                .andExpect(status().isUnprocessableEntity()).andReturn();
        JsonNode problem = body(rejected);
        assertEquals("statistical-contract-not-compilable", problem.get("code").asText());
        assertEquals("UNKNOWN_FIELD", problem.get("issues").get(0).get("code").asText());
        assertTrue(problem.get("type").asText().startsWith("https://"));
    }

    @Test void authorityAndProductBoundariesHoldAtTheHttpEdge() throws Exception {
        String id = body(create("k")).get("id").asText();
        mvc.perform(get(BASE + "/" + id).principal(STRANGER)).andExpect(status().isNotFound());
        mvc.perform(get("/platform/products/LAND_PRODUCT/statistical-contracts/" + id).principal(AUTHOR)).andExpect(status().isNotFound());
        mvc.perform(post(BASE).principal(STRANGER).header("Idempotency-Key", "x").contentType(MediaType.APPLICATION_JSON).content(labourDraft()))
                .andExpect(status().isForbidden());
        mvc.perform(post(BASE + "/" + id + "/submit").principal(AUTHOR).header("If-Match", "\"1\"")).andExpect(status().isOk());
        mvc.perform(post(BASE + "/" + id + "/approve").principal(AUTHOR).header("If-Match", "\"2\"").contentType(MediaType.APPLICATION_JSON).content("{\"revisionDigest\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    private String approved() throws Exception {
        String id = body(create("file")).get("id").asText();
        String digest = body(mvc.perform(post(BASE + "/" + id + "/submit").principal(AUTHOR).header("If-Match", "\"1\"")).andReturn()).get("revisionDigest").asText();
        mvc.perform(post(BASE + "/" + id + "/approve").principal(APPROVER).header("If-Match", "\"2\"").contentType(MediaType.APPLICATION_JSON)
                .content("{\"revisionDigest\":\"" + digest + "\"}")).andExpect(status().isOk());
        return id;
    }

    @Test void authoringFileIsServedOnlyForAnApprovedContractAndAFilledCopyValidates(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        String pending = body(create("pending")).get("id").asText();
        mvc.perform(get(BASE + "/" + pending + "/authoring-file").principal(AUTHOR)).andExpect(status().isConflict());

        String id = approved();
        MvcResult download = mvc.perform(get(BASE + "/" + id + "/authoring-file").principal(AUTHOR)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(header().string("X-Content-Type-Options", "nosniff")).andReturn();
        byte[] bytes = download.getResponse().getContentAsByteArray();
        String digest = java.util.Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        assertEquals("sha-256=" + digest, download.getResponse().getHeader("Digest"), "the integrity header describes the bytes that were sent");
        assertTrue(download.getResponse().getHeader("Content-Disposition").contains("LABOUR_LABOUR_FORCE_"));
        mvc.perform(get(BASE + "/" + id + "/authoring-file").param("language", "ka'; DROP").principal(AUTHOR)).andExpect(status().isBadRequest());

        java.nio.file.Path file = dir.resolve("filled.accdb");
        java.nio.file.Files.write(file, bytes);
        try (var db = com.healthmarketscience.jackcess.DatabaseBuilder.open(file.toFile())) {
            var table = db.getTable("stat_DSD_LABOUR");
            table.addRow("2025", "GE_TB", "F", new java.math.BigDecimal("12000"), "P", new java.math.BigDecimal("1800"), null);
            table.addRow("2025-Q9", "GE_TB", "M", new java.math.BigDecimal("1"), null, null, null);
        }
        var upload = new org.springframework.mock.web.MockMultipartFile("file", "filled.accdb", "application/msaccess", java.nio.file.Files.readAllBytes(file));
        JsonNode report = body(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(BASE + "/" + id + "/authoring-file/validation").file(upload).principal(AUTHOR))
                .andExpect(status().isOk()).andReturn());
        assertEquals(false, report.get("accepted").asBoolean());
        assertEquals(2, report.get("rows").asInt());
        java.util.Set<String> codes = new java.util.HashSet<>();
        report.get("rowIssues").forEach(i -> codes.add(i.get("row").asInt() + ":" + i.get("code").asText()));
        assertEquals(java.util.Set.of("2:INVALID_PERIOD", "2:VALUE_WITHOUT_STATUS_MISSING"), codes, "every finding names its row; the valid first row raises none");

        var garbage = new org.springframework.mock.web.MockMultipartFile("file", "x.accdb", "application/msaccess", "not an access file".getBytes());
        assertEquals("NOT_A_READABLE_AUTHORING_FILE", body(mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(BASE + "/" + id + "/authoring-file/validation").file(garbage).principal(AUTHOR))
                .andReturn()).get("fileIssues").get(0).asText());
        var huge = new org.springframework.mock.web.MockMultipartFile("file", "big.accdb", "application/msaccess", new byte[3_000_001]);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart(BASE + "/" + id + "/authoring-file/validation").file(huge).principal(AUTHOR))
                .andExpect(status().isPayloadTooLarge());
    }
}
