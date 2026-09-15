package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** SQL-backed lease prevents duplicate scheduler execution across API replicas. */
@Service
public class PlatformJobLeaseService {
    private final JdbcTemplate control;
    private final String owner = UUID.randomUUID().toString();
    public PlatformJobLeaseService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control){this.control=control;}
    public boolean acquire(String jobName,int leaseMinutes){
        int updated=control.update("UPDATE platform.job_lease SET lease_owner=?,lease_until=DATEADD(MINUTE,?,SYSUTCDATETIME()),updated_at=SYSUTCDATETIME() WHERE job_name=? AND (lease_until<SYSUTCDATETIME() OR lease_owner=?)",owner,leaseMinutes,jobName,owner);
        if(updated>0)return true;
        try { return control.update("INSERT INTO platform.job_lease(job_name,lease_owner,lease_until) VALUES(?,?,DATEADD(MINUTE,?,SYSUTCDATETIME()))",jobName,owner,leaseMinutes)==1; }
        catch(Exception duplicateLease){ return false; }
    }
    public void release(String jobName){control.update("UPDATE platform.job_lease SET lease_until=SYSUTCDATETIME(),updated_at=SYSUTCDATETIME() WHERE job_name=? AND lease_owner=?",jobName,owner);}
}
