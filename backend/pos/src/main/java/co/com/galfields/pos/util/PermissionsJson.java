package co.com.galfields.pos.util;

import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

// Shared by EmployeeRoleService (CRUD over employee_roles.permissions) and
// AuthService (reads it to embed in the JWT) - both need the exact same
// JSONB-string -> Map<String, Boolean> conversion for employee_roles.permissions.
//
// Uses Jackson 3 (tools.jackson.*), not the classic com.fasterxml.jackson.* -
// Spring Boot 4.1's JacksonAutoConfiguration only registers a
// tools.jackson.databind.json.JsonMapper bean (see
// org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration),
// no com.fasterxml.jackson.databind.ObjectMapper bean exists to inject at
// all, even though that groupId is still present transitively (pulled in by
// springdoc/swagger-core, which hasn't moved to Jackson 3 yet) - confirmed
// by @Autowired failing at boot with "No qualifying bean of type
// com.fasterxml.jackson.databind.ObjectMapper" before this was caught.
public final class PermissionsJson {

    private PermissionsJson() {
    }

    public static Map<String, Boolean> read(ObjectMapper objectMapper, String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Boolean>>() {
            });
        } catch (JacksonException e) {
            throw new IllegalStateException("Corrupt permissions JSON for employee_roles row", e);
        }
    }
}
