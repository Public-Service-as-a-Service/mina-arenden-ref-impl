package se.psaas.minaarenden.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi(MinaArendenProperties properties) {
        return new OpenAPI().info(new Info()
                .title("Mina ärenden – referensimplementation (producent)")
                .version("0.1.0")
                .description("""
                        Enklast möjliga producent enligt Mina ärendens standard för samlad ärendeåterkoppling.
                        Kundhändelser lagras i en ärendecache (MariaDB) och lämnas ut via samma operation som
                        Skatteverkets vidareförmedlingstjänst använder: POST /kundhandelseFragaSynkron.
                        Producent: %s (prefix %s, standardversion %s).
                        """.formatted(properties.producent(), properties.prefix(), properties.standardVersion()))
                .contact(new Contact().name("Public Service as a Service").url("https://github.com/Public-Service-as-a-Service/mina-arenden-ref-impl"))
                .license(new License().name("MIT")));
    }
}
