package org.base.mobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.base.mobile.PublicMobileFacade;
import org.base.mobile.RequestContext;
import org.base.mobile.dto.*;
import org.base.mobile.params.FullRaitingParams;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public REST API for mobile/automobile statistics portal.
 * <p>
 * Thin routing shell — all business logic resides in
 * {@link PublicMobileFacade} and the domain services behind it.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/mobile")
@Api
public class PublicMobileController {

    private final PublicMobileFacade facade;

    @GetMapping("/sliders-data")
    public ResponseEntity<?> getSlidersData(HttpServletRequest request) {
        return facade.getSlidersData(RequestContext.from(request));
    }

    @GetMapping("/filters")
    public ResponseEntity<?> getFilters(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "") String filter,
            @RequestParam(required = false) String transport) {
        return facade.getFilters(table, year, quarter, filter, transport, RequestContext.from(request));
    }

    @GetMapping("/sankey")
    public ResponseEntity<?> getSankey(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "") String filter) {
        return facade.getSankey(table, year, quarter, filter, RequestContext.from(request));
    }

    @GetMapping("/full-raiting")
    public ResponseEntity<?> getFullRaiting(
            @RequestParam(defaultValue = "false") Boolean table,
            @ModelAttribute FullRaitingParams params) {
        return facade.getFullRaiting(table, params);
    }

    @GetMapping("/top-five")
    public List<TopFiveDTO> getTopFive(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String transport,
            @RequestParam(required = false) Boolean table) {
        return facade.getTopFive(table, year, quarter, transport);
    }

    @GetMapping("/treemap")
    public ResponseEntity<?> getTreemap(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter) {
        return facade.getTreemap(table, year, quarter, RequestContext.from(request));
    }

    @GetMapping("/colors")
    public List<ColorsDTO> getColors(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getColors(table, year, quarter, RequestContext.from(request));
    }

    @GetMapping("/fuels")
    public List<FuelDTO> getFuels(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getFuels(table, year, quarter, RequestContext.from(request));
    }

    @GetMapping("/engine")
    public List<EngineDTO> getEngines(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getEngines(table, year, quarter, RequestContext.from(request));
    }

    @GetMapping("/body")
    public List<TResponseDTO> getBody(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getBody(table, year, quarter, RequestContext.from(request));
    }

    @GetMapping("/vehicle-age")
    public List<WrappedResponseDTO<VehicleAgeDTO>> getVehicleAge(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String quarter,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getVehicleAge(table, year, quarter, RequestContext.from(request));
    }

    @GetMapping("/race")
    public ResponseEntity<?> getRace(@RequestParam(defaultValue = "false") Boolean table) {
        return facade.getRace(table);
    }

    @GetMapping("/vehicle-dual")
    public ResponseEntity<?> getDual(
            HttpServletRequest request,
            @RequestParam(required = false) String v_type) {
        return facade.getDual(v_type, RequestContext.from(request));
    }

    @GetMapping("/stacked-area")
    public ResponseEntity<?> getStacked(
            HttpServletRequest request,
            @RequestParam(required = false) String filter) {
        return facade.getStacked(filter, RequestContext.from(request));
    }

    @GetMapping("/area-currency")
    public ResponseEntity<?> getAreaCurrency(
            HttpServletRequest request,
            @RequestParam(defaultValue = "[dbo].[vehicles_imp_exp]") String tableName,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) Boolean currency) {
        return facade.getAreaCurrency(tableName, e_i, type, fuel, vehicle, currency, RequestContext.from(request));
    }

    @GetMapping("/area-quantity")
    public ResponseEntity<?> getAreaQuantity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "[dbo].[vehicles_imp_exp]") String tableName,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) Boolean currency) {
        return facade.getAreaQuantity(tableName, e_i, type, fuel, vehicle, currency, RequestContext.from(request));
    }

    @GetMapping("/line-trade")
    public ResponseEntity<?> getTrade(
            HttpServletRequest request,
            @RequestParam(defaultValue = "[dbo].[vehicles_imp_exp]") String tableName,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false) String vehicle,
            @RequestParam(required = false) Boolean currency) {
        return facade.getTrade(tableName, e_i, type, fuel, vehicle, currency, RequestContext.from(request));
    }

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
        return facade.getCompareLine(
                brand1, model1, year_of_production1, fuel1, body1, color1, engine1,
                brand2, model2, year_of_production2, fuel2, body2, color2, engine2,
                RequestContext.from(request));
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
        return facade.getRegionalMap(table, year, quarter, brand, year_of_production, region, RequestContext.from(request));
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
        return facade.getRegionalBar(table, year, quarter, brand, year_of_production, region, RequestContext.from(request));
    }

    @GetMapping("/regional-quantity")
    public ResponseEntity<?> getRegionalQuantity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year_of_production,
            @RequestParam(required = false) String region) {
        return facade.getRegionalQuantity(table, brand, year_of_production, region, RequestContext.from(request));
    }

    @GetMapping("/equity")
    public ResponseEntity<?> getEquity(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String year_of_production,
            @RequestParam(required = false) String region) {
        return facade.getEquity(table, brand, year_of_production, region, RequestContext.from(request));
    }

    @GetMapping("/fuel-currency")
    public ResponseEntity<?> getFuelCurrency(
            HttpServletRequest request,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false") Boolean currency) {
        return facade.getFuelCurrency(e_i, fuel, currency, RequestContext.from(request));
    }

    @GetMapping("/fuel-quantity")
    public ResponseEntity<?> getFuelQuantity(
            HttpServletRequest request,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false") Boolean anualOrMonthly) {
        return facade.getFuelQuantity(e_i, fuel, anualOrMonthly, RequestContext.from(request));
    }

    @GetMapping("/fuel-av-price")
    public ResponseEntity<?> getFuelAvPrice(
            HttpServletRequest request,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false") Boolean currency) {
        return facade.getFuelAvPrice(fuel, currency, RequestContext.from(request));
    }

    @GetMapping("/fuel-column")
    public ResponseEntity<?> getFuelColumn(
            HttpServletRequest request,
            @RequestParam(required = false) String e_i,
            @RequestParam(required = false) String fuel,
            @RequestParam(required = false, defaultValue = "false") Boolean currency) {
        return facade.getFuelColumn(e_i, fuel, currency, RequestContext.from(request));
    }

    @GetMapping("/fuel-line")
    public ResponseEntity<?> getFuelLine(HttpServletRequest request) {
        return facade.getFuelLine(RequestContext.from(request));
    }

    @GetMapping("/road-length")
    public ResponseEntity<?> getRoadLength(
            HttpServletRequest request,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String region) {
        return facade.getRoadLength(year, region, RequestContext.from(request));
    }

    @GetMapping("/accidents-main")
    public ResponseEntity<?> getAccidentsMain(
            HttpServletRequest request,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String accidents) {
        return facade.getAccidentsMain(region, accidents, RequestContext.from(request));
    }

    @GetMapping("/accidents-gender")
    public ResponseEntity<?> getAccidentsGender(
            @RequestParam(defaultValue = "patrul_age") String tableName,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String accidents,
            @Valid @NotBlank @RequestParam String langName) throws Throwable {
        return facade.getAccidentsGender(tableName, accidents, langName);
    }

    @GetMapping("/license-gender")
    public ResponseEntity<?> getLicenseGender(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getLicenseGender(table, RequestContext.from(request));
    }

    @GetMapping("/license-age")
    public ResponseEntity<?> getLicenseAge(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table) {
        return facade.getLicenseAge(table, RequestContext.from(request));
    }

    @GetMapping("/license-dual")
    public ResponseEntity<?> getLicenseDual(HttpServletRequest request) {
        return facade.getLicenseDual(RequestContext.from(request));
    }

    @GetMapping("/license-sankey")
    public ResponseEntity<?> getLicenseSankey(
            HttpServletRequest request,
            @RequestParam(defaultValue = "false") Boolean table,
            @RequestParam(required = false) Integer year) {
        return facade.getLicenseSankey(table, year, RequestContext.from(request));
    }
}

