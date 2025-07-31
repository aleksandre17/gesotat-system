package org.base.api.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.base.core.entity.Migration;
import org.base.core.model.request.NodeRequest;
import org.base.core.repository.JsonMigrationRepository;
import org.base.core.runner.TreeBasePagesMigrationRunner;
import org.base.core.service.PageStructureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@AllArgsConstructor
public class CarsMigrationRunner implements ApplicationRunner  {

    private static final Logger log = LoggerFactory.getLogger(CarsMigrationRunner.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
//            ClassPathResource resource = new ClassPathResource("migration/eoy_2017.sql");
//            log.info("Executing SQL script: {}", resource.getFilename());
//
//            try (Connection conn = jdbcTemplate.getDataSource().getConnection()) {
//                log.info("start:");
//                ScriptUtils.executeSqlScript(conn, resource);
//            }
//            log.info("end:");
        } catch (Exception e) {
            log.error("Failed to run migrations: {}", e.getMessage(), e);
        }
    }

}
