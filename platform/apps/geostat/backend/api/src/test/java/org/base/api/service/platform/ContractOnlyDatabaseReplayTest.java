package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real database-backed replay for a second site: no KIDS identifiers or controller branch are used. */
class ContractOnlyDatabaseReplayTest {
 @Test void secondSiteContractIsLoadedQueriedJoinedAndReplayIsIdempotent(){
  var ds=new DriverManagerDataSource("jdbc:h2:mem:other_site;MODE=MSSQLServer;DB_CLOSE_DELAY=-1","sa",""); var db=new JdbcTemplate(ds);
  db.execute("create table other_entity (record_id int primary key, label varchar(100), classifier_id int)");
  db.execute("create table other_classifier (classifier_id int primary key, code varchar(50))");
  db.update("insert into other_classifier values (10,'A')"); db.update("insert into other_entity values (1,'Example',10)");
  Set<String> declared=Set.of("record_id","label","classifier_id"); var plan=ContractQueryCompiler.compile(Map.of("label","Example"),declared,"label",false);
  String sql="select record_id,label,classifier_id from other_entity"+plan.whereSql()+plan.orderSql();
  // The compiler emits SQL Server delimiters; the contract itself remains portable.
  List<Map<String,Object>> roots=db.queryForList(sql.replace("[","").replace("]","").replace("select record_id,label,classifier_id","select record_id as \"record_id\",label as \"label\",classifier_id as \"classifier_id\""),plan.parameters().toArray());
  List<Map<String,Object>> child=db.queryForList("select classifier_id as \"classifier_id\",code as \"code\" from other_classifier");
  List<Map<String,Object>> joined=ContractRelationGraphExecutor.attach(roots,"classifier",child,"classifier_id","classifier_id",false);
  assertEquals("Example",joined.get(0).get("label")); assertEquals("A",((Map<?,?>)joined.get(0).get("classifier")).get("code"));
  assertEquals(joined.toString(),ContractRelationGraphExecutor.attach(db.queryForList(sql.replace("[","").replace("]","").replace("select record_id,label,classifier_id","select record_id as \"record_id\",label as \"label\",classifier_id as \"classifier_id\""),plan.parameters().toArray()),"classifier",child,"classifier_id","classifier_id",false).toString());
 }
}
