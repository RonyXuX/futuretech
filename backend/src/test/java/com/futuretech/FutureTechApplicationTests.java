package com.futuretech;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Starts the whole app against the real database: fails if the DB is unreachable,
// a Flyway migration is broken, or an entity does not match its table.
@SpringBootTest
class FutureTechApplicationTests {

	@Test
	void contextLoads() {
	}

}
