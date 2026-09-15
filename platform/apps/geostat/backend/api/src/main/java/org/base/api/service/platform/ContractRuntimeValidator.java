package org.base.api.service.platform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
/** Fail-closed validation immediately before query planning. */
@Service
public class ContractRuntimeValidator {
 private final JdbcTemplate db;
 public ContractRuntimeValidator(@Qualifier("primaryJdbcTemplate") JdbcTemplate db){this.db=db;}
 public void validate(long revisionId,String dataset){
  Integer fields=db.query("SELECT COUNT(*) FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id WHERE d.site_contract_revision_id=? AND d.dataset_code=?",r->{r.next();return r.getInt(1);},revisionId,dataset);
  if(fields==null||fields==0) throw new IllegalStateException("Contract dataset has no declared fields: "+dataset);
  Integer tables=db.query("SELECT COUNT(*) FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE (t.logical_table_code=? OR t.access_table_name=?) AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name))",r->{r.next();return r.getInt(1);},dataset,dataset,revisionId);
  if(tables==null||tables==0) throw new IllegalStateException("No approved physical mapping for contract dataset: "+dataset);
 }
}
