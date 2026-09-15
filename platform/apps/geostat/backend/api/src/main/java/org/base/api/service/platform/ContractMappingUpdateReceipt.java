package org.base.api.service.platform;

/** Result of replacing a reviewed but not-yet-active semantic mapping. */
public record ContractMappingUpdateReceipt(long contractId, long contractSourceId, String approvalState, String contractStatus) {
}
