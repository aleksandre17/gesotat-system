package org.base.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.base.api.service.mobile_services.*;
import org.base.api.service.mobile_services.dto.*;
import org.base.api.service.mobile_services.params.*;
import org.base.core.anotation.Api;
import org.base.core.exeption.extend.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@AllArgsConstructor
@RequiredArgsConstructor
@RestController
@RequestMapping("/mobile")
@Api
public class PublicMobileController {

    @Autowired
    private MobileService mobileService;

    @Autowired
    private LanguageService languageService;

    /**
     * Retrieves sliders data grouped by page.
     *
//     * @param tableName the name of the table to query
//     * @param langName the language key for names (e.g., name_en)
//     * @param period the language key for periods (e.g., period_en)
//     * @param title the language key for titles (e.g., title_en)
     * @return List of SlidersDataDTO grouped by page
     */
    @GetMapping(path = "/sliders-data")
    @ResponseBody
    public ResponseEntity<?> getSlidersData(HttpServletRequest request) {
        try {
            String langName = (String) request.getAttribute("langName");
            String period = (String) request.getAttribute("period");
            String title = (String) request.getAttribute("title");
            List<SlidersDataDTO> result = mobileService.getSlidersData("sliders_data", langName, period, title);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal Server Error");
        }
    }

    @GetMapping("/filters")
    public ResponseEntity<?> getFilters(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(required = false) String transport) {
        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }

        String langName = (String) request.getAttribute("langName");
        FiltersParams params = new FiltersParams(year, quarter, filter, transport, langName);
        FiltersDTO result = mobileService.getFilters(tableName, params);
        return ResponseEntity.ok(result);

//        try {
//            FiltersParams params = new FiltersParams(year, quarter, filter, transport, langName);
//            FiltersDTO result = mobileService.getFilters(tableName, params);
//            return ResponseEntity.ok(result);
//        } catch (TopModelNotFoundException e) {
//            //return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"message\": \"" + e.getClientMessage() + "\"}");
//        } catch (ApiException e) {
//            //return ResponseEntity.status(HttpStatus.NOT_FOUND).body("\"" + e.getClientMessage() + "\"");
//        }
    }

    @GetMapping("/sankey")
    public ResponseEntity<?> getSankey(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "") String filter
            ) {
        try {
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }
            String langName = (String) request.getAttribute("lang");
            SankeyParams params = new SankeyParams(year, quarter, filter, langName);
            List<SankeyDTO> result = mobileService.getSankey(tableName, params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/full-raiting")
    public ResponseEntity<?> getFullRaiting(
            @RequestParam(defaultValue = "false") Boolean table,
            @ModelAttribute FullRaitingParams params) {
        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        PaginatedResponse<FullRaitingDTO.Item> result = mobileService.getFullRaiting(tableName, params);
        return ResponseEntity.ok(result);//catch (InvalidPageException e) {
            //return ResponseEntity.ok(e.getClientMessage());
//        } catch (ApiException e) {
//            //return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
//        }
    }

    @GetMapping("/top-five")
    public List<TopFiveDTO> getTopFive(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String transport,
            @RequestParam(required = false) Boolean table) { // Dynamic table name

        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        return mobileService.getTopFive(tableName, year, quarter, transport);
    }

    @GetMapping("/treemap")
    public ResponseEntity<?> getTreemap(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter
           ) {
        try {
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }

            String langName = (String) request.getAttribute("langName");
            TreemapParams params = new TreemapParams(year, quarter, langName);
            TreemapDTO result = mobileService.getTreemap(tableName, params);
            return ResponseEntity.ok(result.results());
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

//    @GetMapping("/treemap")
//    public Map<String, Map<String, Integer>> getTreemap(
//            HttpServletRequest request,
//            @RequestParam(required = false) Integer year,
//            @RequestParam(required = false) String quarter,
//            @RequestParam @NotBlank String tableName) {
//        String langKey = request.getParameter("lang") != null && request.getParameter("lang").equals("en") ? "en" : "ka";
//        return mobileService.getTreemap(year, quarter, langKey, tableName);
//    }

//    @GetMapping("/sankey")
//    public List<SankeyLinkDTO> getSankey(
//            HttpServletRequest request,
//            @RequestParam(required = false) Integer year,
//            @RequestParam(required = false) String quarter,
//            @RequestParam(required = false) String filter,
//            @RequestParam @NotBlank String tableName) {
//        String langName = (String) request.getAttribute("langName");
//        return mobileService.getSankey(year, quarter, filter, langName, tableName);
//    }

    @GetMapping("/colors")
    public List<ColorsDTO> getColors(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        String langName = (String) request.getAttribute("langName");
        String othersEn = languageService.getColumnNames(langName).get("other");
        ColorsParams params = new ColorsParams(year, quarter, langName, othersEn);
        return mobileService.getColors(tableName, params);
    }

    @GetMapping("/fuels")
    public List<FuelDTO> getFuels(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {

        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        String langName = (String) request.getAttribute("langName");
        return mobileService.getFuels(year, quarter, langName, tableName);
    }

    @GetMapping("/engine")
    public List<EngineDTO> getEngines(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        String langName = (String) request.getAttribute("langName");
        return mobileService.getEngines(year, quarter, langName, tableName);
    }

    @GetMapping("/body")
    public List<TResponseDTO> getBody(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        String langName = (String) request.getAttribute("langName");
        return mobileService.getBody(year, quarter, langName, tableName);
    }

    @GetMapping("/vehicle-age")
    public List<WrappedResponseDTO<VehicleAgeDTO>> getVehicleAge(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        String tableName;
        if (Objects.equals(table, false)) {
            tableName = "[dbo].[eoyes]";
        } else {
            tableName = "[dbo].[auto_main]";
        }
        String langName = (String) request.getAttribute("langName");
        return mobileService.getVehicleAge(year, quarter, langName, tableName);
    }

//    @GetMapping("/race")
//    public Map<String, List<BrandDTO>> getRace(
//            @RequestParam @NotBlank String tableName) {
//        return mobileService.getRace(tableName);
//    }

    @GetMapping("/race")
    public ResponseEntity<?> getRace(@RequestParam(defaultValue = "false") Boolean table) {
        try {
            RaceParams params = new RaceParams();
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }
            Map<String, List<Map<String, Object>>> result = mobileService.getRace(tableName, params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

//    @GetMapping("/dual")
//    public List<VehicleDualDTO> getDual(
//            HttpServletRequest request,
//            @RequestParam(required = false) String vType) {
//        String langName = (String) request.getAttribute("langName");
//        return mobileService.getDual(vType, langName);
//    }
    @GetMapping("/vehicle-dual")
    public ResponseEntity<?> getDual(
            HttpServletRequest request,
            @RequestParam(required = false) String v_type
            ) {
        try {
            String langName = (String) request.getAttribute("langName");
            DualParams params = new DualParams(v_type, langName);
            List<DualDTO> result = mobileService.getDual("[dbo].[vehicles1000]", params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        }
    }

//    @GetMapping("/stacked")
//    public StackedDataDTO getStacked(
//            HttpServletRequest request,
//            @RequestParam(required = false) String filter) {
//        String langName = (String) request.getAttribute("langName");
//        return mobileService.getStacked(filter, langName);
//    }

    @GetMapping("/stacked-area")
    public ResponseEntity<?> getStacked(
            HttpServletRequest request,
            @RequestParam(required = false) String filter
            ) {
        try {
            String langName = (String) request.getAttribute("langName");
            StackedParams params = new StackedParams(filter, langName,  languageService.getColumnNames(langName).get("other"));
            StackedDTO result = mobileService.getStacked("[dbo].[auto_main]", params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(404).body("\"" + e.getClientMessage() + "\"");
        }
    }


    @GetMapping("/area-currency")
    public ResponseEntity<?> getAreaCurrency(
            HttpServletRequest request,
            @RequestParam(defaultValue = "[dbo].[vehicles_imp_exp]") String tableName,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) Boolean currency
            ) {
        try {
            String langName = (String) request.getAttribute("langName");
            AreaQuantityOrCurrencyParams params = new AreaQuantityOrCurrencyParams(e_i, type, fuel, vehicle, currency, langName, "currency");
            QuantityOrCurrencyDTO result = mobileService.getAreaCurrencyOrQuantity(tableName, params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.ok("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/area-quantity")
    public ResponseEntity<?> getQuantity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "[dbo].[vehicles_imp_exp]") String tableName,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) Boolean currency) {
        try {
            String langName = (String) request.getAttribute("langName");
            AreaQuantityOrCurrencyParams params = new AreaQuantityOrCurrencyParams(e_i, type, fuel, vehicle, currency, langName, "quantity");
            QuantityOrCurrencyDTO result = mobileService.getAreaCurrencyOrQuantity(tableName, params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.ok("\"" + e.getClientMessage() + "\"");
        }
    }

    @GetMapping("/line-trade")
    public ResponseEntity<?> getTrade(
            HttpServletRequest request,
            @RequestParam(defaultValue = "[dbo].[vehicles_imp_exp]") String tableName,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) Boolean currency
           ) {
        try {
            String langName = (String) request.getAttribute("langName");
            AreaQuantityOrCurrencyParams params = new AreaQuantityOrCurrencyParams(e_i, type, fuel, vehicle, currency, langName, "currency");

            TradeDTO result = mobileService.getTrade(tableName, params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.ok("\"" + e.getClientMessage() + "\"");
        }
    }

//    @GetMapping("/area-currency")
//    public AreaCurrencyDTO getAreaQurrency(
//            HttpServletRequest request,
//            @RequestParam(required = false) String e_i,
//            @RequestParam(required = false) String type,
//            @RequestParam(required = false) String vehicle,
//            @RequestParam(required = false) String fuel,
//            @RequestParam(required = false) String currency) {
//        String langName = (String) request.getAttribute("langName");
//        return mobileService.getAreaQurrency(e_i, type, vehicle, fuel, currency, langName);
//    }

    @GetMapping("/compare-line")
    public CompareLineDTO getCompareLine(
            HttpServletRequest request,
            @RequestParam(required = false) String brand1,
            @RequestParam(required = false) String model1,
            @RequestParam(required = false) String year_of_production1,
            @RequestParam(required = false) String fuel1,
            @RequestParam(required = false) String body1,
            @RequestParam(required = false) String color1,
            @RequestParam(required = false) String engine1,
            @RequestParam(required = false) String brand2,
            @RequestParam(required = false) String model2,
            @RequestParam(required = false) String year_of_production2,
            @RequestParam(required = false) String fuel2,
            @RequestParam(required = false) String body2,
            @RequestParam(required = false) String color2,
            @RequestParam(required = false) String engine2) {
        String langName = (String) request.getAttribute("langName");
        return mobileService.getCompareLine(
                brand1, model1, year_of_production1, fuel1, body1, color1, engine1,
                brand2, model2, year_of_production2, fuel2, body2, color2, engine2, langName);
    }

    @GetMapping("/regional-map")
    public ResponseEntity<?> getRegionalMap(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year_of_production,
            @RequestParam(required = false) String region) {
        String langName = (String) request.getAttribute("langName");
        try {
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }

            List<RegionalMapDTO> result = mobileService.getRegionalMap(tableName, year, quarter, brand, year_of_production, region, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    @GetMapping("/regional-bar")
    public ResponseEntity<?> getRegionalBar(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year_of_production,
            @RequestParam(required = false) String region) {
        String langName = (String) request.getAttribute("langName");
        try {
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }

            List<RegionalBarDTO> result = mobileService.getRegionalBar(
                    tableName, year, quarter, brand, year_of_production, region, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    /**
     * Retrieves regional quantity data.
     */
    @GetMapping("/regional-quantity")
    public ResponseEntity<?> getRegionalQuantity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year_of_production,
            @RequestParam(required = false) String region) {
        String langName = (String) request.getAttribute("langName");
        try {
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }
            List<RegionalBarDTO> result = mobileService.getRegionalQuantity(tableName, brand, year_of_production, region, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    /**
     * Retrieves equity data.
     */
    @GetMapping("/equity")
    public ResponseEntity<?> getEquity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year_of_production,
            @RequestParam(required = false) String region) {
        String langName = (String) request.getAttribute("lang");
        try {
            String tableName;
            if (Objects.equals(table, false)) {
                tableName = "[dbo].[eoyes]";
            } else {
                tableName = "[dbo].[auto_main]";
            }
            EquityDTO result = mobileService.getEquity(tableName, brand, year_of_production, region, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    /**
     * Retrieves fuel currency data.
     */
    @GetMapping("/fuel-currency")
    public ResponseEntity<?> getFuelCurrency(
            HttpServletRequest request,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false") Boolean currency) {
        try {
            FuelCurrencyParams params = new FuelCurrencyParams(e_i, fuel, currency);
            FuelCurrencyDTO result = mobileService.getFuelCurrency(params, "[dbo].[fuel_imp_exp]", (String) request.getAttribute("lang"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    /**
     * Retrieves fuel quantity data.
     */
    @GetMapping("/fuel-quantity")
    public ResponseEntity<?> getFuelQuantity(
            HttpServletRequest request,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false") Boolean anualOrMonthly) {
        String langName = (String) request.getAttribute("lang");
        try {
            FuelQuantityDTO result = mobileService.getFuelQuantity(new FuelQuantityParams(e_i, fuel, anualOrMonthly),"[dbo].[fuel_imp_exp]", langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    /**
     * Retrieves fuel average price data.
     */
    @GetMapping("/fuel-av-price")
    public ResponseEntity<?> getFuelAvPrice(
            HttpServletRequest request,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false")
            Boolean currency
    ) {
        String langName = (String) request.getAttribute("langName");
        try {
            FuelAvPriceParams params = new FuelAvPriceParams(fuel, currency);
            FuelAvPriceDTO result = mobileService.getFuelAvPrice(params, "[dbo].[fuel_imp_exp]", langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve data from the database"+e.getCause());
        }
    }

    /**
     * Retrieves fuel column data by year and country.
     */
    @GetMapping("/fuel-column")
    public ResponseEntity<?> getFuelColumn(
            HttpServletRequest request,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false")
            Boolean currency
    ) {
        String langName = (String) request.getAttribute("lang");
        try {

            FuelColumnDTO result = mobileService.getFuelColumn(new FuelColumnParams(e_i, fuel, currency), "[dbo].[fuel_imp_exp]", langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database"+e.getCause());
        }
    }


    /**
     * Retrieves fuel line data by fuel type and year.
     *
     * @return FuelLineDTO containing pointStart and series data
     */
    @GetMapping("/fuel-line")
    public ResponseEntity<?> getFuelLine(HttpServletRequest request) {
        try {
            String langName = (String) request.getAttribute("lang");
            FuelLineDTO result = mobileService.getFuelLine("[dbo].[fuel_prices]", langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("\"Failed to retrieve data from the database\"");
        }
    }

    /**
     * Retrieves road length data by year and region.
     *
     * @param year      the year filter (optional)
     * @param region    the region filter (optional)
     * @return List of RoadLengthDTO containing category names and lengths
     */
    @GetMapping("/road-length")
    public ResponseEntity<?> getRoadLength(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String region) {
        try {
            String langName = (String) request.getAttribute("lang");
            List<RoadLengthDTO> result = mobileService.getRoadLength("[dbo].[road_length]", year, region, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"Failed to retrieve data from the database\"");
        }
    }

    /**
     * Retrieves accident quantity data by region and accident type.
     *
     @RequestParam @RequestParam(defaultValue = "patrul_ssk_ask") String tableName
      * @param region    the region filter (optional)
     * @param accidents the accident type filter (optional)

     * @return AccidentsMainDTO containing pointStart and series data
     */
    @GetMapping("/accidents-main")
    public ResponseEntity<?> getAccidentsMain(
            HttpServletRequest request,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String accidents) {
        try {
            String langName = (String) request.getAttribute("lang");
            AccidentsMainDTO result = mobileService.getAccidentsMain("[dbo].[patrul_ssk_ask]", region, accidents, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to retrieve data from the database"+e.getCause()+e.getCause());
        }
    }

    /**
     * Retrieves accident quantity data by gender and accident type.
     *
     * @param tableName the name of the table to query
     * @param region    the region filter (optional, ignored)
     * @param accidents the accident type filter (required, must not be '3')
     * @param langName  the language key for translations (e.g., name_en)
     * @return AccidentsGenderDTO containing pointStart and series data
     * @throws ResponseStatusException if the accidents parameter is '3' or if an error occurs
     * **Response Match**
     *  ```json
     *   {
     *     "pointStart": 2020,
     *     "data": [
     *       { "name": "Male", "data": [100, 150, 200] },
     *       { "name": "Female", "data": [50, 75, 100] }
     *     ]
     *   }
     *   ```
     */
    @GetMapping("/accidents-gender")
    public ResponseEntity<?> getAccidentsGender(
            @RequestParam(defaultValue = "patrul_age") String tableName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String accidents,
            @Valid @NotBlank @RequestParam String langName) throws Throwable {
        try {
            AccidentsGenderDTO result = mobileService.getAccidentsGender(tableName, accidents, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            if (e.getCause() instanceof ResponseStatusException) {
                throw e.getCause();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("\"Failed to retrieve data from the database\"");
        }
    }

    /**
     * Retrieves license data by gender and year.
     *
     * @param table the name of the table to query
     * //@param lang the language key for translations (e.g., name_en)
     * @return List of LicenseGenderDTO representing gender data by year
     */
    @GetMapping("/license-gender")
    public ResponseEntity<?> getLicenseGender(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table) {
        try {
            String langName = (String) request.getAttribute("lang");
            LicenseGenderParams params = new LicenseGenderParams(table);
            List<LicenseGenderDTO> result = mobileService.getLicenseGender(params, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"Failed to retrieve data from the database\"");
        }
    }

    /**
     * Retrieves license data by age and year.
     *
     * @param table the name of the table to query
     * //@param lang the language key for translations (e.g., name_en)
     * @return LicenseAgeDTO with categories (years) and series (age groups)
     */
    @GetMapping("/license-age")
    public ResponseEntity<?> getLicenseAge(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table
    ) {
        try {
            String langName = (String) request.getAttribute("lang");
            LicenseAgeDTO result = mobileService.getLicenseAge(table, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"Failed to retrieve data from the database\"");
        }
    }

    /**
     * Retrieves license data from licenses_main and licenses_eoy by year.
     *
     * //@param tableName the name of the table to query
     * @return List of LicenseDualDTO with quantities from both tables by year
     */
    @GetMapping("/license-dual")
    public ResponseEntity<?> getLicenseDual(HttpServletRequest request) {
        try {
            List<LicenseDualDTO> result = mobileService.getLicenseDual("[dbo].[licenses]", new QueryParams<>((String) request.getAttribute("lang"), new LicenseDualParams()));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"Failed to retrieve data from the database\"");
        }
    }

    @GetMapping("/license-sankey")
    public ResponseEntity<?> getLicenseSunKey(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year
    ) {
        try {

            List<LicenseSankeyDTO> result = mobileService.getLicenseSankey(new QueryParams<>((String) request.getAttribute("lang"), new LicenseSankeyParams(year,  table)), "[dbo].[licenses]");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("\"Failed to retrieve data from the database\"");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidInput(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to retrieve data from the database"+e.getCause());
    }

}
