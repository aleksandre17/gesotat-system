package org.base.api.service.platform.statistical;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.platform.statistical.access.AccessAuthoringAdapter;
import org.base.api.service.platform.statistical.compiler.ContractDraftParser;
import org.base.api.service.platform.statistical.compiler.StatisticalContractCompiler;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.registry.InMemoryStatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Codelist;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.Lifecycle;
import org.base.api.service.platform.statistical.registry.StatisticalRegistry.MeasureDefinition;

import java.util.Set;

/**
 * Two unrelated subject areas share one registry and one compiler: a labour dataset (time, coded dimensions,
 * two count measures with a per-measure status) and a non-time land-use dataset with different units. Nothing
 * in the engine names either of them.
 */
final class StatisticalContractFixtures {
    static final StatisticalRegistry.Scope LABOUR = new StatisticalRegistry.Scope("LABOUR_PRODUCT");
    static final StatisticalRegistry.Scope LAND = new StatisticalRegistry.Scope("LAND_PRODUCT");

    static final Ref CL_AREA = Ref.parse("codelist:SHARED:CL_AREA(1.0.0)");
    static final Ref CL_SEX = Ref.parse("codelist:SHARED:CL_SEX(1.0.0)");
    static final Ref CL_OBS_STATUS = Ref.parse("codelist:SHARED:CL_OBS_STATUS(1.0.0)");
    static final Ref CL_LAND_USE = Ref.parse("codelist:LAND:CL_LAND_USE(2.1.0)");

    private StatisticalContractFixtures() { }

    static InMemoryStatisticalRegistry registry() {
        InMemoryStatisticalRegistry r = new InMemoryStatisticalRegistry();
        for (String wire : new String[]{"profile:SHARED:STAT_AGGREGATE(1.0.0)", "profile:SHARED:STAT_MICRODATA(1.0.0)",
                "concept:SHARED:TIME_PERIOD(1.0.0)", "concept:SHARED:REF_AREA(1.0.0)", "concept:SHARED:SEX(1.0.0)",
                "concept:SHARED:OBS_STATUS(1.0.0)", "concept:SHARED:EMPLOYED(1.0.0)", "concept:SHARED:UNEMPLOYED(1.0.0)",
                "concept:SHARED:SOURCE_NOTE(1.0.0)", "unit:SHARED:PERSONS(1.0.0)", "unit:SHARED:HECTARE(1.0.0)",
                "unit:SHARED:PERCENT(1.0.0)", "policy:SHARED:QUALITY_BASELINE(1.0.0)"})
            r.approved(Ref.parse(wire));
        r.approved(CL_AREA, new Codelist(CL_AREA, Set.of("GE", "GE_TB", "GE_KA")));
        r.approved(CL_SEX, new Codelist(CL_SEX, Set.of("F", "M", "_T")));
        r.approved(CL_OBS_STATUS, new Codelist(CL_OBS_STATUS, Set.of("A", "E", "M", "O", "P")));
        measure(r, "measure:SHARED:EMPLOYED(1.0.0)", "concept:SHARED:EMPLOYED(1.0.0)", 18, 0, "unit:SHARED:PERSONS(1.0.0)", Lifecycle.APPROVED);
        measure(r, "measure:SHARED:UNEMPLOYED(1.0.0)", "concept:SHARED:UNEMPLOYED(1.0.0)", 18, 0, "unit:SHARED:PERSONS(1.0.0)", Lifecycle.APPROVED);

        // Product-scoped entries: visible to their owner only.
        r.put(Ref.parse("concept:LAND:LAND_USE(1.0.0)"), "concept", Lifecycle.APPROVED, LAND.productCode());
        r.put(Ref.parse("concept:LAND:AREA_SIZE(1.0.0)"), "concept", Lifecycle.APPROVED, LAND.productCode());
        r.put(Ref.parse("concept:LAND:SHARE(1.0.0)"), "concept", Lifecycle.APPROVED, LAND.productCode());
        r.put(CL_LAND_USE, new Codelist(CL_LAND_USE, Set.of("ARABLE", "FOREST", "URBAN")), Lifecycle.APPROVED, LAND.productCode());
        Ref areaSize = Ref.parse("measure:LAND:AREA_SIZE(1.0.0)");
        r.put(areaSize, new MeasureDefinition(areaSize, Ref.parse("concept:LAND:AREA_SIZE(1.0.0)"), new Representation.Numeric(28, 10, false),
                Ref.parse("unit:SHARED:HECTARE(1.0.0)")), Lifecycle.APPROVED, LAND.productCode());
        Ref share = Ref.parse("measure:LAND:SHARE(1.0.0)");
        r.put(share, new MeasureDefinition(share, Ref.parse("concept:LAND:SHARE(1.0.0)"), new Representation.Numeric(7, 4, false),
                Ref.parse("unit:SHARED:PERCENT(1.0.0)")), Lifecycle.PROPOSED, LAND.productCode());
        return r;
    }

    private static void measure(InMemoryStatisticalRegistry r, String ref, String concept, int precision, int scale, String unit, Lifecycle lifecycle) {
        Ref measureRef = Ref.parse(ref);
        r.put(measureRef, new MeasureDefinition(measureRef, Ref.parse(concept), new Representation.Numeric(precision, scale, false), Ref.parse(unit)), lifecycle);
    }

    static StatisticalContractCompiler compiler(StatisticalRegistry registry) { return compiler(registry, AccessAuthoringAdapter.catalog()); }

    static StatisticalContractCompiler compiler(StatisticalRegistry registry, ProviderCapabilities.Catalog providers) {
        return new StatisticalContractCompiler(new ContractDraftParser(new ObjectMapper()), registry, providers,
                new StatisticalContractCompiler.Settings(Set.of("STAT_AGGREGATE"), new StatisticalContractCompiler.NumericEnvelope(28, 10), Set.of("ka")));
    }

    static String labourDraft() {
        return """
            {"profileRef":"profile:SHARED:STAT_AGGREGATE(1.0.0)",
             "dataset":{"namespace":"LABOUR","code":"LABOUR_FORCE"},
             "structure":{"inline":{"code":"DSD_LABOUR","version":"1.0.0",
               "dimensions":[
                 {"code":"TIME_PERIOD","conceptRef":"concept:SHARED:TIME_PERIOD(1.0.0)","representation":{"type":"TIME_PERIOD","formats":["YEAR","QUARTER"]}},
                 {"code":"REF_AREA","conceptRef":"concept:SHARED:REF_AREA(1.0.0)","representation":{"type":"CODED","codelistRef":"codelist:SHARED:CL_AREA(1.0.0)"}},
                 {"code":"SEX","conceptRef":"concept:SHARED:SEX(1.0.0)","representation":{"type":"CODED","codelistRef":"codelist:SHARED:CL_SEX(1.0.0)"}}],
               "measures":[
                 {"code":"EMPLOYED","measureRef":"measure:SHARED:EMPLOYED(1.0.0)"},
                 {"code":"UNEMPLOYED","measureRef":"measure:SHARED:UNEMPLOYED(1.0.0)"}],
               "attributes":[
                 {"code":"EMPLOYED_STATUS","conceptRef":"concept:SHARED:OBS_STATUS(1.0.0)","representation":{"type":"CODED","codelistRef":"codelist:SHARED:CL_OBS_STATUS(1.0.0)"},"attachment":{"level":"MEASURE","measure":"EMPLOYED"}},
                 {"code":"UNEMPLOYED_STATUS","conceptRef":"concept:SHARED:OBS_STATUS(1.0.0)","representation":{"type":"CODED","codelistRef":"codelist:SHARED:CL_OBS_STATUS(1.0.0)"},"attachment":{"level":"MEASURE","measure":"UNEMPLOYED"}},
                 {"code":"SOURCE_NOTE","conceptRef":"concept:SHARED:SOURCE_NOTE(1.0.0)","representation":{"type":"TEXT","maxLength":200},"attachment":{"level":"DATASET"}}]}},
             "policyRefs":["policy:SHARED:QUALITY_BASELINE(1.0.0)"],
             "sourceProfile":"ACCESS_ACCDB",
             "presentation":{"captions":{
               "TIME_PERIOD":{"ka":"პერიოდი"},"REF_AREA":{"ka":"რეგიონი"},"SEX":{"ka":"სქესი"},
               "EMPLOYED":{"ka":"დასაქმებულები"},"UNEMPLOYED":{"ka":"უმუშევრები"},
               "EMPLOYED_STATUS":{"ka":"დასაქმებულების სტატუსი"},"UNEMPLOYED_STATUS":{"ka":"უმუშევრების სტატუსი"},
               "SOURCE_NOTE":{"ka":"წყაროს შენიშვნა"}}}}
            """;
    }

    /** Non-time, different units, scale-10 decimal, a constant dimension and quarantine mode. */
    static String landDraft(boolean withProposedShare) {
        return """
            {"profileRef":"profile:SHARED:STAT_AGGREGATE(1.0.0)",
             "dataset":{"namespace":"LAND","code":"LAND_USE_AREA"},
             "structure":{"inline":{"code":"DSD_LAND_USE","version":"1.0.0",
               "dimensions":[
                 {"code":"REF_AREA","conceptRef":"concept:SHARED:REF_AREA(1.0.0)","representation":{"type":"CODED","codelistRef":"codelist:SHARED:CL_AREA(1.0.0)"}},
                 {"code":"LAND_USE","conceptRef":"concept:LAND:LAND_USE(1.0.0)","representation":{"type":"CODED","codelistRef":"codelist:LAND:CL_LAND_USE(2.1.0)"}}],
               "measures":[{"code":"AREA_SIZE","measureRef":"measure:LAND:AREA_SIZE(1.0.0)"}%s]}},
             "constantBindings":[{"component":"REF_AREA","value":"GE","overridable":true}],
             "sourceProfile":"ACCESS_ACCDB","errorMode":"QUARANTINE_ROWS",
             "presentation":{"captions":{"REF_AREA":{"ka":"რეგიონი"},"LAND_USE":{"ka":"მიწის გამოყენება"},"AREA_SIZE":{"ka":"ფართობი"}%s}}}
            """.formatted(withProposedShare ? ",{\"code\":\"SHARE\",\"measureRef\":\"measure:LAND:SHARE(1.0.0)\"}" : "",
                withProposedShare ? ",\"SHARE\":{\"ka\":\"წილი\"}" : "");
    }
}
