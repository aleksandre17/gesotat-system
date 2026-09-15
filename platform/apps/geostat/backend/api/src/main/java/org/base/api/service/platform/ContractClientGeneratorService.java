package org.base.api.service.platform;

import org.springframework.stereotype.Service;
import java.util.*;

/** Generates deterministic JSON Schema and TypeScript types from the approved contract view. */
@Service
public class ContractClientGeneratorService {
    private final ContractIntrospectionService discovery;
    public ContractClientGeneratorService(ContractIntrospectionService discovery){this.discovery=discovery;}
    public Map<String,Object> jsonSchema(String code,int revision){
        Map<String,Object> c=discovery.contract(code,revision); Map<String,Object> defs=new LinkedHashMap<>();
        for(Object p:(List<?>)c.getOrDefault("pages",List.of())){Map<?,?> page=(Map<?,?>)p;Map<?,?> caps=(Map<?,?>)page.get("capabilities");Object rawFields=caps.get("fields");List<Map<String,Object>> fs=rawFields instanceof List<?> ? (List<Map<String,Object>>)rawFields : List.of();Map<String,Object> props=new LinkedHashMap<>();for(Map<String,Object> f:fs)props.put(String.valueOf(f.get("name")),Map.of("type",jsonType(String.valueOf(f.get("logicalType")))));defs.put(String.valueOf(page.get("datasetCode")),Map.of("type","object","properties",props));}
        Map<String,Object> out=new LinkedHashMap<>();out.put("$schema","https://json-schema.org/draft/2020-12/schema");out.put("$id","urn:geostat:"+code.toLowerCase(Locale.ROOT)+":v"+revision);out.put("type","object");out.put("properties",Map.of("data",Map.of("type","array","items",Map.of("oneOf",defs.keySet().stream().map(x->Map.of("$ref","#/$defs/"+x)).toList()))));out.put("$defs",defs);return out;
    }
    public String typescript(String code,int revision){Map<String,Object> c=discovery.contract(code,revision);StringBuilder s=new StringBuilder("// Generated from ").append(code).append(" revision ").append(revision).append("\n\n");for(Object p:(List<?>)c.getOrDefault("pages",List.of())){Map<?,?> page=(Map<?,?>)p;Map<?,?> caps=(Map<?,?>)page.get("capabilities");Object rawFields=caps.get("fields");List<Map<String,Object>> fs=rawFields instanceof List<?> ? (List<Map<String,Object>>)rawFields : List.of();s.append("export interface ").append(typeName(String.valueOf(page.get("datasetCode")))).append(" {\n");for(Map<String,Object> f:fs)s.append("  ").append(safeTsIdentifier(String.valueOf(f.get("name")))).append("?: ").append(tsType(String.valueOf(f.get("logicalType")))).append(";\n");s.append("}\n\n");}s.append("export const query = (baseUrl:string, contract:string, pageId:number, body:unknown) => fetch(`${baseUrl}/api/v1/platform/contracts/${contract}/pages/${pageId}/query`, {method:'POST', headers:{'content-type':'application/json'}, body:JSON.stringify(body)});\n");return s.toString();}
    /** Generates deterministic model declarations for supported SDK languages. */
    public String sdk(String code, int revision, String language) {
        String lang = language == null ? "" : language.toLowerCase(Locale.ROOT);
        if (!Set.of("java", "kotlin", "dart").contains(lang)) throw new IllegalArgumentException("Unsupported SDK language");
        Map<String,Object> schema = jsonSchema(code, revision);
        StringBuilder out = new StringBuilder("// Generated from ").append(code).append(" revision ").append(revision).append("\n\n");
        Map<?,?> defs = (Map<?,?>) schema.get("$defs");
        for (var entry : defs.entrySet()) {
            String name = typeName(String.valueOf(entry.getKey()));
            Map<?,?> model = (Map<?,?>) entry.getValue(); Map<?,?> props = (Map<?,?>) model.get("properties");
            if (lang.equals("java")) { out.append("public record ").append(name).append("("); int i=0; for(var p:props.entrySet()){if(i++>0)out.append(", ");out.append(javaType(((Map<?,?>)p.getValue()).get("type"))).append(" ").append(safeTsIdentifier(String.valueOf(p.getKey())));} out.append(") {}\n"); }
            else if (lang.equals("kotlin")) { out.append("data class ").append(name).append("(\n"); int i=0; for(var p:props.entrySet()){if(i++>0)out.append(",\n");out.append("  val ").append(safeTsIdentifier(String.valueOf(p.getKey()))).append(": ").append(kotlinType(((Map<?,?>)p.getValue()).get("type"))).append("? = null");} out.append("\n)\n"); }
            else { out.append("class ").append(name).append(" {\n"); for(var p:props.entrySet())out.append("  ").append(dartType(((Map<?,?>)p.getValue()).get("type"))).append("? ").append(safeTsIdentifier(String.valueOf(p.getKey()))).append(";\n"); out.append("}\n"); }
        }
        return out.toString();
    }
    private static String javaType(Object t){return "number".equals(t)?"java.math.BigDecimal":"String";}
    private static String kotlinType(Object t){return "number".equals(t)?"java.math.BigDecimal":"String";}
    private static String dartType(Object t){return "number".equals(t)?"num":"String";}
    private static String typeName(String x){StringBuilder b=new StringBuilder();for(String p:x.split("[^A-Za-z0-9]+")){if(!p.isBlank())b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());}return b.toString();}
    private static String jsonType(String x){return switch(x.toUpperCase(Locale.ROOT)){case "INTEGER","DECIMAL","NUMBER"->"number";case "BOOLEAN"->"boolean";default->"string";};}
    private static String tsType(String x){return jsonType(x).equals("string")?"string":jsonType(x);}
    static String safeTsIdentifier(String value){String normalized=value==null?"field":value.replaceAll("[^A-Za-z0-9_$]","_");if(normalized.isBlank())normalized="field";if(!Character.isJavaIdentifierStart(normalized.charAt(0)))normalized="_"+normalized;StringBuilder out=new StringBuilder();for(int i=0;i<normalized.length();i++){char c=normalized.charAt(i);out.append(Character.isJavaIdentifierPart(c)?c:'_');}String result=out.toString();return Set.of("class","function","var","let","const","interface","type","return","export","import","default","extends","implements","new","delete","in","instanceof","typeof","void","yield","async","await","this","true","false","null","undefined").contains(result)?"_"+result:result;}
}
