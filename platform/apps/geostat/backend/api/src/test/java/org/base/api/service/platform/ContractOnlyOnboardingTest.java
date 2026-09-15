package org.base.api.service.platform;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
/** Proves a second site's contract can be compiled without KIDS identifiers or code branches. */
class ContractOnlyOnboardingTest {
 @Test void secondSiteUsesOnlyDeclaredContract(){
   Set<String> fields=Set.of("record_id","label","period");
   var plan=ContractQueryCompiler.compile(Map.of("period","2026"),fields,"label",false);
   assertTrue(plan.whereSql().contains("[period]")); assertEquals(" ORDER BY [label] ASC",plan.orderSql());
   var graph=new LinkedHashMap<String,List<String>>(); graph.put("OTHER_ENTITY",List.of("OTHER_CLASSIFIER")); graph.put("OTHER_CLASSIFIER",List.of()); ContractRelationGraphExecutor.assertAcyclic(graph);
 }
}
