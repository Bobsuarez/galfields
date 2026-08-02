package co.com.galfields.pos_transactions.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mirrors backend/pos's OpenApiConfig — same API, re-architected, not a new one. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI posOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Galfields POS API")
                        .description("Point of sale backend: products, inventory, sales, purchase orders and related domain endpoints.")
                        .version("v1"));
    }
}
