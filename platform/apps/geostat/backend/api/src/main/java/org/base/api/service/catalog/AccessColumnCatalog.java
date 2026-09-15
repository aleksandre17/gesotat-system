package org.base.api.service.catalog;

/** Read-only description of one Access column discovered during cataloging. */
public record AccessColumnCatalog(String name, String type, boolean required) {
}
