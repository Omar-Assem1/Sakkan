package com.sakan;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/** Keep in sync with the `db` service image in docker-compose.yml. */
	static final String POSTGIS_IMAGE = "postgis/postgis:16-3.4";

	@Bean
	@ServiceConnection
	public PostgreSQLContainer postgresContainer() {
		DockerImageName image = DockerImageName.parse(POSTGIS_IMAGE).asCompatibleSubstituteFor("postgres");
		return new PostgreSQLContainer(image);
	}

}
