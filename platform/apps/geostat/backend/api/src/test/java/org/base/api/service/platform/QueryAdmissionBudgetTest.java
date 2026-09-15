package org.base.api.service.platform;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class QueryAdmissionBudgetTest {
    @Test
    void costIsDeterministicAndRejectsExpensiveShape() {
        var request = new ContractQueryRequest(Map.of("a", 1), "id", false,
                List.of("age"), "SUM", null, 1000,
                List.of("id", "value"), List.of("raw", "statistics"), null,
                Map.of("and", List.of(Map.of("x", 1))), List.of(), true,
                Map.of("statistics", 10));
        var budget = new QueryAdmissionBudget(100);
        assertEquals(budget.cost(request, 1000), budget.cost(request, 1000));
        var ex = assertThrows(QueryAdmissionBudget.QueryAdmissionRejectedException.class,
                () -> budget.admit(request, 1000));
        assertTrue(ex.cost() > ex.maximum());
    }

    @Test
    void smallShapeIsAccepted() {
        var budget = new QueryAdmissionBudget(5000);
        var request = new ContractQueryRequest(Map.of(), "id", false,
                List.of(), null, null, 10);
        assertDoesNotThrow(() -> budget.admit(request, 10));
    }

    @Test
    void nestedPredicateNodesContributeToCost() {
        var shallow = new ContractQueryRequest(Map.of(), "id", false,
                List.of(), null, null, 1, List.of(), List.of(), null,
                Map.of("and", List.of(Map.of("field", "x"))), List.of(), false, Map.of());
        var deep = new ContractQueryRequest(Map.of(), "id", false,
                List.of(), null, null, 1, List.of(), List.of(), null,
                Map.of("and", List.of(Map.of("and", List.of(Map.of("and", List.of(Map.of("field", "x"))))))),
                List.of(), false, Map.of());
        assertTrue(new QueryAdmissionBudget(5000).cost(deep, 1)
                > new QueryAdmissionBudget(5000).cost(shallow, 1));
    }

    @Test
    void admissionDecisionsAreObservable() {
        var registry = new SimpleMeterRegistry();
        var budget = new QueryAdmissionBudget(20, registry);
        var request = new ContractQueryRequest(Map.of(), "id", false,
                List.of(), null, null, 1);
        budget.admit(request, 1);
        assertEquals(1.0, registry.get("geostat.query.admission.accepted").counter().count());
        assertThrows(QueryAdmissionBudget.QueryAdmissionRejectedException.class,
                () -> budget.admit(request, 100));
        assertEquals(1.0, registry.get("geostat.query.admission.rejected").counter().count());
    }
}
