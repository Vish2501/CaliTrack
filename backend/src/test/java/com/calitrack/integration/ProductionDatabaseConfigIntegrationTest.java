package com.calitrack.integration;

import com.calitrack.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.postgresql.PGConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

// Production goes through Supabase's transaction pooler, where server-side prepared statements
// break once a query has run a few times (see application-prod.yml). These tests guard that fix.
@TestPropertySource(properties = "spring.datasource.hikari.data-source-properties.prepareThreshold=0")
class ProductionDatabaseConfigIntegrationTest extends AbstractIntegrationTest {

    private static final String PREPARE_THRESHOLD = "spring.datasource.hikari.data-source-properties.prepareThreshold";

    @Autowired
    private DataSource dataSource;

    @Test
    void prodProfileTurnsOffServerSidePreparedStatements() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-prod.yml"));
        Properties prod = yaml.getObject();

        assertThat(prod.getProperty(PREPARE_THRESHOLD)).isEqualTo("0");
    }

    @Test
    void hikariPassesThePrepareThresholdToThePostgresDriver() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.unwrap(PGConnection.class).getPrepareThreshold()).isZero();
        }
    }
}
