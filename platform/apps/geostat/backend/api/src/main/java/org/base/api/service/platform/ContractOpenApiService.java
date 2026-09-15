package org.base.api.service.platform;

import org.springframework.stereotype.Service;
import java.util.*;

/** Generates a minimal OpenAPI 3.1 document from the approved contract registry. */
@Service
public class ContractOpenApiService {
    private final ContractIntrospectionService introspection;
    public ContractOpenApiService(ContractIntrospectionService introspection){this.introspection=introspection;}
    public Map<String,Object> document(String code, int revision){
        Map<String,Object> contract=introspection.contract(code,revision);
        Map<String,Object> paths=new LinkedHashMap<>();
        for(Object raw:(List<?>)contract.getOrDefault("pages",List.of())){
            Map<?,?> page=(Map<?,?>)raw; int id=((Number)page.get("pageId")).intValue();
            paths.put("/api/v1/platform/contracts/"+code+"/pages/"+id+"/query",Map.of("post",Map.of("operationId","queryPage"+id,"responses",Map.of("200",Map.of("description","Published contract response"),"400",Map.of("description","Invalid contract query")))));
        }
        Map<String,Object> out=new LinkedHashMap<>(); out.put("openapi","3.1.0"); out.put("info",Map.of("title",code,"version",String.valueOf(contract.get("revision")))); out.put("paths",paths); out.put("x-contract",contract);
        ContractOpenApiValidator.validate(out);
        return out;
    }
}
