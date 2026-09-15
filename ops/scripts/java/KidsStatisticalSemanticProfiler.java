import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Read-only evidence report used to author governed statistical semantic bindings. */
public final class KidsStatisticalSemanticProfiler {
  private static final ObjectMapper JSON=new ObjectMapper();
  public static void main(String[] args)throws Exception{
    File source=new File(args.length==0?"samples/kids-children-portal-full-data.accdb":args[0]);
    try(Database db=new DatabaseBuilder(source).setReadOnly(true).open()){
      System.out.println("id\ttitle_ka\ttitle_en\tpath_ka\tperiods\tdimensions\tvalues\tmin\tmax\tintegral\tsample");
      for(Row row:db.getTable("files")){
        String payload=value(row,"chartdata"); JsonNode array=parse(payload); if(array==null||!array.isArray())continue;
        List<String> periods=new ArrayList<>(),dimensions=new ArrayList<>(),sample=new ArrayList<>();
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;int values=0,integral=0;
        for(JsonNode object:array){if(!object.isObject())continue;String period=object.path("year").asText();if(!period.isBlank()&&!periods.contains(period))periods.add(period);
          for(Iterator<Map.Entry<String,JsonNode>> it=object.fields();it.hasNext();){Map.Entry<String,JsonNode> e=it.next();if("year".equals(e.getKey())||e.getValue().isNull())continue;
            if(!dimensions.contains(e.getKey()))dimensions.add(e.getKey());String lexical=e.getValue().asText();if(sample.size()<6)sample.add(period+":"+e.getKey()+"="+lexical);
            try{double number=Double.parseDouble(lexical);min=Math.min(min,number);max=Math.max(max,number);values++;if(Math.rint(number)==number)integral++;}catch(Exception ignored){}
          }}
        System.out.println(clean(value(row,"ID"))+"\t"+clean(value(row,"title_geo"))+"\t"+clean(value(row,"title_eng"))+"\t"+clean(value(row,"path_geo"))+"\t"+clean(String.join(",",periods))+"\t"+clean(String.join("|",dimensions))+"\t"+values+"\t"+(values==0?"":min)+"\t"+(values==0?"":max)+"\t"+integral+"\t"+clean(String.join(";",sample)));
      }
    }
  }
  private static JsonNode parse(String payload){if(payload.isBlank())return null;try{return JSON.readTree(payload);}catch(Exception first){try{return JSON.readTree(JSON.readValue("\""+payload.replace("\"","\\\"")+"\"",String.class));}catch(Exception ignored){return null;}}}
  private static String value(Row row,String column){Object value=row.get(column);return value==null?"":String.valueOf(value);}
  private static String clean(String value){return value.replace('\t',' ').replace('\r',' ').replace('\n',' ');}
}
