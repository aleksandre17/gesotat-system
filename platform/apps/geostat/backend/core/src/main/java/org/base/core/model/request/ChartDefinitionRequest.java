package org.base.core.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.base.core.entity.data.ChartType;
import org.base.core.entity.data.PublicationStatus;

@Data
public class ChartDefinitionRequest {
    @NotNull private Long profileId;
    @NotBlank private String chartCode;
    @NotNull private ChartType chartType;
    private String xField;
    private String yField;
    private String seriesField;
    private String aggregation;
    private String filtersJson;
    private String displayJson;
    private PublicationStatus publicationStatus = PublicationStatus.DRAFT;
}
