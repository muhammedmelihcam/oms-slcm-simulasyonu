package com.melih.omsslcm.config;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

import java.io.File;

/**
 * Runs before the DataSource bean is created (registered via spring.factories,
 * fired on ApplicationEnvironmentPreparedEvent). SQLite's JDBC driver creates
 * the .db file itself but not missing parent directories, so without this the
 * app fails on a clean checkout / fresh container volume where ./data doesn't
 * exist yet.
 */
public class DataDirectoryInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        Environment env = event.getEnvironment();
        String dbPath = env.getProperty("DB_PATH", "./data/oms_slcm.db");
        File dbFile = new File(dbPath);
        File parentDir = dbFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
