package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Resolves the platform default from governance state; no site code is embedded in the engine. */
@Service
public class ApprovedContractResolver {
 private final JdbcTemplate db;
 public ApprovedContractResolver(@Qualifier("primaryJdbcTemplate") JdbcTemplate db){this.db=db;}
 public String resolve(){return db.query("SELECT TOP 1 contract_code FROM platform.site_contract_revision WHERE status='APPROVED' ORDER BY effective_from DESC,revision DESC,site_contract_revision_id DESC",r->r.next()?r.getString(1):null);}
}
