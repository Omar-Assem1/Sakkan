package com.sakan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.sakan.support.tags.IntegrationTest;

@IntegrationTest
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SakanApplicationTests {

	@Test
	void contextLoads() {
	}

}
