package org.base.api.service.platform.access;
public record SemanticAccessMetadataAssertion(String subjectType,String subjectCode,int subjectRevision,String namespaceCode,String propertyCode,String languageTag,String valueType,String valueText,String valueJson,int ordinal,String lifecycleStatus,String sourceReference) {}
