package org.base.api.service.mobile_services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.base.api.service.mobile_services.dto.MobileRatingDTO;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MobileRatingResponse {
    private List<MobileRatingDTO> result;
    private Integer currentPage;
    private Integer totalPages;
    private String totalResults;
}
