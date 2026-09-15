package org.base.core.model.request;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * NodeRequest extended with children — used only for JSON migration files.
 * The HTTP API uses plain NodeRequest (no children).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MigrationNodeRequest extends NodeRequest {
    private List<MigrationNodeRequest> children;
}