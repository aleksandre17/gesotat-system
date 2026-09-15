import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.*;
import java.io.File;
import java.util.Map;

/** Adds a lossless normalized statistical-input dataset to the full KIDS v3 package. */
public final class KidsPortalAccessV3StatisticalInputGenerator {
  public static void main(String[] args) throws Exception {
    File out=new File(args.length>0?args[0]:"samples/kids-portal-v1-access-v3-contract-r6-full-data.accdb");
    File source=new File(args.length>1?args[1]:"samples/kids-children-portal-full-data.accdb");
    KidsPortalAccessV3TemplateGenerator.main(new String[]{out.getPath(),source.getPath()});
    ObjectMapper json=new ObjectMapper();
    try(Database d=DatabaseBuilder.open(out)) {
      TableBuilder b=new TableBuilder("kids_statistical_input");
      for(String c:new String[]{"source_file_id","period","age_group_source_key","observation_value","source_encoding"}) b.addColumn(new ColumnBuilder(c,DataType.MEMO));
      Table input=b.toTable(d);
      Table files=d.getTable("files"); long count=0;
      for(Row file:files) {
        Object raw=file.get("chartdata"); if(raw==null)continue; String text=raw.toString(); JsonNode array;
        try {array=json.readTree(text);} catch(Exception direct) { try {array=json.readTree(json.readValue("\""+text.replace("\"","\\\"")+"\"",String.class));}catch(Exception ignored){continue;} }
        if(!array.isArray())continue;
        for(JsonNode item:array) for(java.util.Iterator<Map.Entry<String,JsonNode>> it=item.fields();it.hasNext();) { Map.Entry<String,JsonNode> e=it.next(); if(e.getKey().equals("year")||e.getValue().isNull())continue; input.addRow(String.valueOf(file.get("ID")),item.path("year").asText(),e.getKey(),e.getValue().asText(),text.trim().startsWith("[{")?"DIRECT_JSON":"ESCAPED_JSON_TEXT"); count++; }
      }
      if(count==0)throw new IllegalStateException("No statistical cells extracted");
      d.getTable("__gs_dataset").addRow("KIDS_STATISTICAL_INPUT","kids_statistical_input","STATISTICAL","one source file, period and raw age-band numeric cell");
      Table f=d.getTable("__gs_field"); for(String[] x:new String[][]{{"source_file_id","INTEGER","FOREIGN_KEY"},{"period","TEXT","PERIOD"},{"age_group_source_key","CODE","DIMENSION"},{"observation_value","DECIMAL","MEASURE"},{"source_encoding","CODE","ATTRIBUTE"}})f.addRow("KIDS_STATISTICAL_INPUT",x[0],x[1],x[2],"true");
      Table k=d.getTable("__gs_key"); k.addRow("KIDS_STATISTICAL_INPUT","source_file_id","NATURAL","1");k.addRow("KIDS_STATISTICAL_INPUT","period","NATURAL","2");k.addRow("KIDS_STATISTICAL_INPUT","age_group_source_key","NATURAL","3");
      d.getTable("__gs_relation").addRow("STAT_INPUT_FILE","KIDS_STATISTICAL_INPUT","source_file_id","KIDS_FILE_RESOURCE","ID","MANY_TO_ONE","true");
      d.getTable("__gs_projection").addRow("KIDS_STATS_INPUT","KIDS_STATISTICAL_INPUT","STATISTICAL","{\"approvalState\":\"DRAFT\",\"dataflowCode\":\"KIDS_FILES_STATISTICS\",\"metricBinding\":\"CONTROL_PLANE_REQUIRED\",\"unitBinding\":\"CONTROL_PLANE_REQUIRED\",\"ageGroupAlias\":\"TRIM_FOR_LOOKUP_ONLY\"}","DRAFT");
    }
  }
}
