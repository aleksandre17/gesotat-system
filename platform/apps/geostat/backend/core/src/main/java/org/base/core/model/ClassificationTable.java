package org.base.core.model;

public interface ClassificationTable {
    String getTableName();
    String getKeyColumn();
    String getAlias();
    boolean hasHexCode();
}
