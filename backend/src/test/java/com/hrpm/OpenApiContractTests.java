package com.hrpm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class OpenApiContractTests {
    @Test
    @SuppressWarnings("unchecked")
    void contractDeclaresImplementedAuthenticationSessionEndpoints() throws Exception {
        Path contract = Path.of("..", "docs", "hrpm-v1.openapi.yaml");
        try (InputStream input = Files.newInputStream(contract)) {
            Map<String, Object> root = new Yaml().load(input);
            Map<String, Object> paths = (Map<String, Object>) root.get("paths");

            assertEquals("3.1.0", root.get("openapi"));
            assertTrue(paths.containsKey("/auth/login"));
            assertTrue(paths.containsKey("/auth/refresh"));
            assertTrue(paths.containsKey("/auth/logout"));
            assertTrue(paths.containsKey("/me"));
            assertTrue(paths.containsKey("/me/permissions"));
            assertTrue(paths.containsKey("/me/menus"));
            assertTrue(paths.containsKey("/system/users/{id}/roles"));
            assertTrue(paths.containsKey("/workflow/templates"));
            assertTrue(paths.containsKey("/workflow/templates/{id}"));
            assertTrue(paths.containsKey("/workflow/tasks/instances/{id}"));
            assertTrue(paths.containsKey("/workflow/tasks/instances/{id}/withdraw"));
            assertTrue(paths.containsKey("/work-calendars"));
            assertTrue(paths.containsKey("/work-calendars/{id}"));
        }
    }
}
