package com.hkp.sync_space.config;

import java.io.IOException;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.embedded-postgres", havingValue = "true")
public class EmbeddedPostgresConfig {

	@Bean(destroyMethod = "close")
	EmbeddedPostgres embeddedPostgres() throws IOException {
		return EmbeddedPostgres.builder().start();
	}

	@Bean
	DataSource dataSource(EmbeddedPostgres embeddedPostgres) {
		return embeddedPostgres.getPostgresDatabase();
	}

}
