package org.base.api.service.platform.access;

/** Portable logical page declaration; numeric runtime IDs are assigned by Control Plane. */
public record SemanticAccessPage(String pageCode, String nodeCode, String contractCode, int contractRevision,
                                 String parentPageCode, String nodeKind, String datasetCode,
                                 String pathSegment, String responseProjectionCode, int sortOrder,
                                 boolean required, String approvalState) { }
