package tn.ithelpdesk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class HelpdeskApplicationTests {
    @Test
    void applicationContextLoads() {
        // Vérifie que Spring démarre correctement avec la configuration de test.
    }
}
