package org.base.api.service.platform;

import java.util.List;

/** Approval result for the full multi-dataset contract boundary. */
public record ContractApprovalReceipt(long contractId, List<Long> datasetVersionIds, String contractStatus, String versionStatus) {
}
