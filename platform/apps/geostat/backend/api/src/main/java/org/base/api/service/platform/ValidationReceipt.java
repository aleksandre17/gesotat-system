package org.base.api.service.platform;

public record ValidationReceipt(long datasetLoadId, long acceptedRows, long rejectedRows, String status) {
}
