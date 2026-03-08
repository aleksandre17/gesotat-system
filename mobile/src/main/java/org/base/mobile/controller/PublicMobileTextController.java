package org.base.mobile.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.base.mobile.MobileServiceText;
import org.base.mobile.TableConfigText;
import org.base.mobile.TableResolver;
import org.base.mobile.dto.text.SelectorDTO;
import org.base.mobile.params.text.RatingsParams;
import org.base.core.anotation.Api;
import org.base.core.exeption.extend.ApiException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mobile-text")
@Api
public class PublicMobileTextController {

    private final MobileServiceText mobileServiceText;
    private final TableResolver tableResolver;

    @GetMapping("/ratings")
    public ResponseEntity<?> getRatings(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String year) {
        try {
            RatingsParams params = new RatingsParams();
            if (year != null && !year.matches("\\d+")) {
                return ResponseEntity.status(400).body("\"Invalid year format\"");
            }
            params.setYear(year != null ? Integer.parseInt(year) : null);
            String tableName = tableResolver.resolve(table);
            params.setLang((String) request.getAttribute("lang"));
            List<SelectorDTO> result = mobileServiceText.getRatings(tableName, params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/top-five")
    public ResponseEntity<?> getTopFive(HttpServletRequest request) {
        try {
            JsonNode result = mobileServiceText.getTopFive((String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/full-ratings")
    public ResponseEntity<?> getFullRatings(HttpServletRequest request, @RequestParam(defaultValue = "false") Boolean table) {
        try {
            String tableName = tableResolver.resolve(table);
            JsonNode result = mobileServiceText.getFullRatings(tableName, (String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/v-quantity")
    public ResponseEntity<?> getVehicleQuantity(HttpServletRequest request, @RequestParam(defaultValue = "false") Boolean table) {
        try {
            String tableName = table ? "[dbo].[eoyes]" : "[dbo].[Vehicles1000]";
            JsonNode result = mobileServiceText.getVehicleQuantity(tableName, (String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/stacked-area")
    public ResponseEntity<?> getStackedArea(HttpServletRequest request) {
        try {
            JsonNode result = mobileServiceText.getStackedArea((String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/export-import")
    public ResponseEntity<?> getExportImport(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String type) {
        try {
            String tableName = table ? "[dbo].[eoyes]" : "[CL].[cl_fuel]";
            JsonNode result = mobileServiceText.getExportImport(tableName, (String) request.getAttribute("lang"), type);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/regional-analysis")
    public ResponseEntity<?> getRegionalAnalysis(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year) {
        try {
            String tableName = tableResolver.resolve(table);
            JsonNode result = mobileServiceText.getRegionalAnalysis(tableName, (String) request.getAttribute("lang"), brand, year);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/compare")
    public ResponseEntity<?> getCompare(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String brand1,
            @RequestParam(required = false) String brand2,
            @RequestParam(required = false) String model1,
            @RequestParam(required = false) String model2,
            @RequestParam(required = false) String year_of_prod1,
            @RequestParam(required = false) String year_of_prod2) {
        try {
            String tableName = table ? "[dbo].[eoyes]" : "[dbo].[auto_main]";
            JsonNode result = mobileServiceText.getCompare(tableName, (String) request.getAttribute("lang"), brand1, brand2, model1, model2, year_of_prod1, year_of_prod2);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/fuel")
    public ResponseEntity<?> getFuel(HttpServletRequest request, @RequestParam(defaultValue = "false") Boolean table) {
        try {
            String tableName = table ? "[dbo].[eoyes]" : "[CL].[cl_fuel]";
            JsonNode result = mobileServiceText.getFuel(tableName, (String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/road")
    public ResponseEntity<?> getRoad(HttpServletRequest request, @RequestParam(defaultValue = "false") Boolean table) {
        try {
            String tableName = table ? "[dbo].[eoyes]" : "[dbo].[road_length]";
            JsonNode result = mobileServiceText.getRoad(tableName, (String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/accidents")
    public ResponseEntity<?> getAccidents(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table) {
        try {
            String tableName = table ? "[dbo].[eoyes]" : "[CL].[cl_accidents]";
            TableConfigText.getTableMetadata(tableName);
            JsonNode result = mobileServiceText.getAccidents(tableName, (String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("\"" + e.getMessage() + "\"");
        }
    }

    @GetMapping("/licenses")
    public ResponseEntity<?> getLicenses(
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false, defaultValue = "en") String lang,
            @RequestParam(required = false) Integer year) {
        try {
            if (lang != null && !lang.matches("^[a-zA-Z]{2}$")) {
                return ResponseEntity.status(400).body("\"Invalid language code\"");
            }
            String tableName = table ? "[dbo].[eoyes]" : TableConfigText.DEFAULT_LICENSES_TABLE_NAME;
            TableConfigText.getTableMetadata(tableName);
            JsonNode result = mobileServiceText.getLicenses(tableName, lang, year);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("\"" + e.getMessage() + "\"");
        }
    }
}

