package com.futuretech;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the whole app against the real database: fails if the DB is unreachable,
// a Flyway migration is broken, or an entity does not match its table.
// Skipped when DB_PASSWORD is not set, so the other tests still run without a database.
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class FutureTechApplicationTests {

	@Test
	void contextLoads() {
	}

}
