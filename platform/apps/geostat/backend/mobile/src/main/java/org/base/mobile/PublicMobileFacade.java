package org.base.mobile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.base.mobile.dto.*;
import org.base.mobile.params.*;
import org.base.core.exeption.extend.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Facade that bridges the thin REST controller with domain services.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Table name resolution via {@link TableResolver}</li>
 *   <li>Parameter object construction from raw request params + {@link RequestContext}</li>
 *   <li>Uniform error handling (maps domain exceptions → HTTP responses)</li>
 * </ul>
 * <p>
 * The controller delegates here with a one-liner; this class calls the appropriate
 * {@link MobileService} method and wraps the result in a {@link ResponseEntity}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PublicMobileFacade {

    private final MobileService mobileService;
    private final LanguageService languageService;
    private final TableResolver tableResolver;

    // ─── Sliders ─────────────────────────────────────────────────────────────

    public ResponseEntity<?> getSlidersData(RequestContext ctx) {
        return execute(() -> {
            List<SlidersDataDTO> result = mobileService.getSlidersData(
                    "sliders_data", ctx.langName(), ctx.period(), ctx.title());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Filters ─────────────────────────────────────────────────────────────

    public ResponseEntity<?> getFilters(Boolean table, Integer year, String quarter,
                                        String filter, String transport, RequestContext ctx) {
        String tableName = tableResolver.resolve(table);
        FiltersParams params = new FiltersParams(year, quarter, filter, transport, ctx.langName());
        FiltersDTO result = mobileService.getFilters(tableName, params);
        return ResponseEntity.ok(result);
    }

    // ─── Sankey ──────────────────────────────────────────────────────────────

    public ResponseEntity<?> getSankey(Boolean table, Integer year, String quarter,
                                       String filter, RequestContext ctx) {
        return executeApi(() -> {
            String tableName = tableResolver.resolve(table);
            SankeyParams params = new SankeyParams(year, quarter, filter, ctx.lang());
            List<SankeyDTO> result = mobileService.getSankey(tableName, params);
            return ResponseEntity.ok(result);
        });
    }

    // ─── Full Rating ─────────────────────────────────────────────────────────

    public ResponseEntity<?> getFullRaiting(Boolean table, FullRaitingParams params) {
        String tableName = tableResolver.resolve(table);
        PaginatedResponse<FullRaitingDTO.Item> result = mobileService.getFullRaiting(tableName, params);
        return ResponseEntity.ok(result);
    }

    // ─── Top Five ────────────────────────────────────────────────────────────

    public List<TopFiveDTO> getTopFive(Boolean table, Integer year, String quarter, String transport) {
        String tableName = tableResolver.resolve(table);
        return mobileService.getTopFive(tableName, year, quarter, transport);
    }

    // ─── Treemap ─────────────────────────────────────────────────────────────

    public ResponseEntity<?> getTreemap(Boolean table, Integer year, String quarter, RequestContext ctx) {
        return executeApi(() -> {
            String tableName = tableResolver.resolve(table);
            TreemapParams params = new TreemapParams(year, quarter, ctx.langName());
            TreemapDTO result = mobileService.getTreemap(tableName, params);
            return ResponseEntity.ok(result.results());
        });
    }

    // ─── Colors ──────────────────────────────────────────────────────────────

    public List<ColorsDTO> getColors(Boolean table, Integer year, String quarter, RequestContext ctx) {
        String tableName = tableResolver.resolve(table);
        String othersEn = languageService.getColumnNames(ctx.langName()).get("other");
        ColorsParams params = new ColorsParams(year, quarter, ctx.langName(), othersEn);
        return mobileService.getColors(tableName, params);
    }

    // ─── Fuels (vehicle fuels) ───────────────────────────────────────────────

    public List<FuelDTO> getFuels(Boolean table, Integer year, String quarter, RequestContext ctx) {
        String tableName = tableResolver.resolve(table);
        return mobileService.getFuels(year, quarter, ctx.langName(), tableName);
    }

    // ─── Engine ──────────────────────────────────────────────────────────────

    public List<EngineDTO> getEngines(Boolean table, Integer year, String quarter, RequestContext ctx) {
        String tableName = tableResolver.resolve(table);
        return mobileService.getEngines(year, quarter, ctx.langName(), tableName);
    }

    // ─── Body ────────────────────────────────────────────────────────────────

    public List<TResponseDTO> getBody(Boolean table, Integer year, String quarter, RequestContext ctx) {
        String tableName = tableResolver.resolve(table);
        return mobileService.getBody(year, quarter, ctx.langName(), tableName);
    }

    // ─── Vehicle Age ─────────────────────────────────────────────────────────

    public List<WrappedResponseDTO<VehicleAgeDTO>> getVehicleAge(Boolean table, Integer year,
                                                                  String quarter, RequestContext ctx) {
        String tableName = tableResolver.resolve(table);
        return mobileService.getVehicleAge(year, quarter, ctx.langName(), tableName);
    }

    // ─── Race ────────────────────────────────────────────────────────────────

    public ResponseEntity<?> getRace(Boolean table) {
        return executeApi(() -> {
            String tableName = tableResolver.resolve(table);
            Map<String, List<Map<String, Object>>> result = mobileService.getRace(tableName, new RaceParams());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Vehicle Dual ────────────────────────────────────────────────────────

    public ResponseEntity<?> getDual(String vType, RequestContext ctx) {
        return executeApi(() -> {
            DualParams params = new DualParams(vType, ctx.langName());
            List<DualDTO> result = mobileService.getDual("[dbo].[vehicles1000]", params);
            return ResponseEntity.ok(result);
        });
    }

    // ─── Stacked Area ────────────────────────────────────────────────────────

    public ResponseEntity<?> getStacked(String filter, RequestContext ctx) {
        try {
            StackedParams params = new StackedParams(
                    filter, ctx.langName(), languageService.getColumnNames(ctx.langName()).get("other"));
            StackedDTO result = mobileService.getStacked("[dbo].[auto_main]", params);
            return ResponseEntity.ok(result);
        } catch (ApiException e) {
            return ResponseEntity.status(404).body("\"" + e.getClientMessage() + "\"");
        }
    }

    // ─── Area Currency ───────────────────────────────────────────────────────

    public ResponseEntity<?> getAreaCurrency(String tableName, String eI, String type,
                                             String fuel, String vehicle, Boolean currency,
                                             RequestContext ctx) {
        return executeApiOk(() -> {
            AreaQuantityOrCurrencyParams params = new AreaQuantityOrCurrencyParams(
                    eI, type, fuel, vehicle, currency, ctx.langName(), "currency");
            return mobileService.getAreaCurrencyOrQuantity(tableName, params);
        });
    }

    // ─── Area Quantity ───────────────────────────────────────────────────────

    public ResponseEntity<?> getAreaQuantity(String tableName, String eI, String type,
                                             String fuel, String vehicle, Boolean currency,
                                             RequestContext ctx) {
        return executeApiOk(() -> {
            AreaQuantityOrCurrencyParams params = new AreaQuantityOrCurrencyParams(
                    eI, type, fuel, vehicle, currency, ctx.langName(), "quantity");
            return mobileService.getAreaCurrencyOrQuantity(tableName, params);
        });
    }

    // ─── Line Trade ──────────────────────────────────────────────────────────

    public ResponseEntity<?> getTrade(String tableName, String eI, String type,
                                      String fuel, String vehicle, Boolean currency,
                                      RequestContext ctx) {
        return executeApiOk(() -> {
            AreaQuantityOrCurrencyParams params = new AreaQuantityOrCurrencyParams(
                    eI, type, fuel, vehicle, currency, ctx.langName(), "currency");
            return mobileService.getTrade(tableName, params);
        });
    }

    // ─── Compare Line ────────────────────────────────────────────────────────

    public CompareLineDTO getCompareLine(
            String brand1, String model1, String yearOfProduction1, String fuel1,
            String body1, String color1, String engine1,
            String brand2, String model2, String yearOfProduction2, String fuel2,
            String body2, String color2, String engine2,
            RequestContext ctx) {
        return mobileService.getCompareLine(
                brand1, model1, yearOfProduction1, fuel1, body1, color1, engine1,
                brand2, model2, yearOfProduction2, fuel2, body2, color2, engine2, ctx.langName());
    }

    // ─── Regional Map ────────────────────────────────────────────────────────

    public ResponseEntity<?> getRegionalMap(Boolean table, Integer year, String quarter,
                                            String brand, String yearOfProduction,
                                            String region, RequestContext ctx) {
        return execute(() -> {
            String tableName = tableResolver.resolve(table);
            List<RegionalMapDTO> result = mobileService.getRegionalMap(
                    tableName, year, quarter, brand, yearOfProduction, region, ctx.langName());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Regional Bar ────────────────────────────────────────────────────────

    public ResponseEntity<?> getRegionalBar(Boolean table, Integer year, String quarter,
                                            String brand, String yearOfProduction,
                                            String region, RequestContext ctx) {
        return execute(() -> {
            String tableName = tableResolver.resolve(table);
            List<RegionalBarDTO> result = mobileService.getRegionalBar(
                    tableName, year, quarter, brand, yearOfProduction, region, ctx.langName());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Regional Quantity ───────────────────────────────────────────────────

    public ResponseEntity<?> getRegionalQuantity(Boolean table, String brand,
                                                  String yearOfProduction, String region,
                                                  RequestContext ctx) {
        return execute(() -> {
            String tableName = tableResolver.resolve(table);
            List<RegionalBarDTO> result = mobileService.getRegionalQuantity(
                    tableName, brand, yearOfProduction, region, ctx.langName());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Equity ──────────────────────────────────────────────────────────────

    public ResponseEntity<?> getEquity(Boolean table, String brand,
                                       String yearOfProduction, String region,
                                       RequestContext ctx) {
        return execute(() -> {
            String tableName = tableResolver.resolve(table);
            EquityDTO result = mobileService.getEquity(
                    tableName, brand, yearOfProduction, region, ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Fuel Currency ───────────────────────────────────────────────────────

    public ResponseEntity<?> getFuelCurrency(String eI, String fuel, Boolean currency, RequestContext ctx) {
        return execute(() -> {
            FuelCurrencyParams params = new FuelCurrencyParams(eI, fuel, currency);
            FuelCurrencyDTO result = mobileService.getFuelCurrency(params, "[dbo].[fuel_imp_exp]", ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Fuel Quantity ───────────────────────────────────────────────────────

    public ResponseEntity<?> getFuelQuantity(String eI, String fuel, Boolean anualOrMonthly, RequestContext ctx) {
        return execute(() -> {
            FuelQuantityDTO result = mobileService.getFuelQuantity(
                    new FuelQuantityParams(eI, fuel, anualOrMonthly), "[dbo].[fuel_imp_exp]", ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Fuel Average Price ──────────────────────────────────────────────────

    public ResponseEntity<?> getFuelAvPrice(String fuel, Boolean currency, RequestContext ctx) {
        return execute(() -> {
            FuelAvPriceParams params = new FuelAvPriceParams(fuel, currency);
            FuelAvPriceDTO result = mobileService.getFuelAvPrice(params, "[dbo].[fuel_imp_exp]", ctx.langName());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Fuel Column ─────────────────────────────────────────────────────────

    public ResponseEntity<?> getFuelColumn(String eI, String fuel, Boolean currency, RequestContext ctx) {
        return execute(() -> {
            FuelColumnDTO result = mobileService.getFuelColumn(
                    new FuelColumnParams(eI, fuel, currency), "[dbo].[fuel_imp_exp]", ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Fuel Line ───────────────────────────────────────────────────────────

    public ResponseEntity<?> getFuelLine(RequestContext ctx) {
        return execute(() -> {
            FuelLineDTO result = mobileService.getFuelLine("[dbo].[fuel_prices]", ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Road Length ─────────────────────────────────────────────────────────

    public ResponseEntity<?> getRoadLength(Integer year, String region, RequestContext ctx) {
        return execute(() -> {
            List<RoadLengthDTO> result = mobileService.getRoadLength(
                    "[dbo].[road_length]", year, region, ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Accidents Main ──────────────────────────────────────────────────────

    public ResponseEntity<?> getAccidentsMain(String region, String accidents, RequestContext ctx) {
        return execute(() -> {
            AccidentsMainDTO result = mobileService.getAccidentsMain(
                    "[dbo].[patrul_ssk_ask]", region, accidents, ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── Accidents Gender ────────────────────────────────────────────────────

    public ResponseEntity<?> getAccidentsGender(String tableName, String accidents,
                                                 String langName) throws Throwable {
        try {
            AccidentsGenderDTO result = mobileService.getAccidentsGender(tableName, accidents, langName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            if (e.getCause() instanceof org.springframework.web.server.ResponseStatusException) {
                throw e.getCause();
            }
            log.error("Failed to retrieve accidents gender data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("\"Failed to retrieve data from the database\"");
        }
    }

    // ─── License Gender ──────────────────────────────────────────────────────

    public ResponseEntity<?> getLicenseGender(Boolean table, RequestContext ctx) {
        return execute(() -> {
            LicenseGenderParams params = new LicenseGenderParams(table);
            List<LicenseGenderDTO> result = mobileService.getLicenseGender(params, ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── License Age ─────────────────────────────────────────────────────────

    public ResponseEntity<?> getLicenseAge(Boolean table, RequestContext ctx) {
        return execute(() -> {
            LicenseAgeDTO result = mobileService.getLicenseAge(table, ctx.lang());
            return ResponseEntity.ok(result);
        });
    }

    // ─── License Dual ────────────────────────────────────────────────────────

    public ResponseEntity<?> getLicenseDual(RequestContext ctx) {
        return execute(() -> {
            List<LicenseDualDTO> result = mobileService.getLicenseDual(
                    "[dbo].[licenses]", new QueryParams<>(ctx.lang(), new LicenseDualParams()));
            return ResponseEntity.ok(result);
        });
    }

    // ─── License Sankey ──────────────────────────────────────────────────────

    public ResponseEntity<?> getLicenseSankey(Boolean table, Integer year, RequestContext ctx) {
        return execute(() -> {
            List<LicenseSankeyDTO> result = mobileService.getLicenseSankey(
                    new QueryParams<>(ctx.lang(), new LicenseSankeyParams(year, table)),
                    "[dbo].[licenses]");
            return ResponseEntity.ok(result);
        });
    }

    // ─── Error handling helpers ──────────────────────────────────────────────

    /**
     * Wraps execution with generic error handling (500 on any exception).
     */
    private ResponseEntity<?> execute(ResponseSupplier supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database");
        }
    }

    /**
     * Wraps execution with ApiException → 500 error handling.
     */
    private ResponseEntity<?> executeApi(ResponseSupplier supplier) {
        try {
            return supplier.get();
        } catch (ApiException e) {
            return ResponseEntity.status(500).body("\"" + e.getClientMessage() + "\"");
        } catch (Exception e) {
            log.error("Request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database");
        }
    }

    /**
     * Wraps execution — returns OK(error message) on ApiException (preserves legacy behavior).
     */
    private <T> ResponseEntity<?> executeApiOk(DataSupplier<T> supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (ApiException e) {
            return ResponseEntity.ok("\"" + e.getClientMessage() + "\"");
        } catch (Exception e) {
            log.error("Request failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve data from the database");
        }
    }

    @FunctionalInterface
    private interface ResponseSupplier {
        ResponseEntity<?> get() throws Exception;
    }

    @FunctionalInterface
    private interface DataSupplier<T> {
        T get() throws Exception;
    }
}

