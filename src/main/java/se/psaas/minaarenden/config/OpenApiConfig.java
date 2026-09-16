package se.psaas.minaarenden.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String API_KEY = "X-Api-Key";

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

                        Autentisering: /kundhandelseFragaSynkron kräver headrarna client_id och client_secret
                        (%s), /kundhandelser kräver X-Api-Key (%s). I skarp drift ersätts detta av OAuth 2 enligt
                        Skatteverkets anslutningsvillkor.
                        """.formatted(properties.producent(), properties.prefix(), properties.standardVersion(),
                        properties.requiresClientCredentials() ? "aktivt" : "inte konfigurerat, öppet",
                        properties.requiresAdminApiKey() ? "aktivt" : "inte konfigurerat, öppet"))
                .contact(new Contact().name("Public Service as a Service").url("https://github.com/Public-Service-as-a-Service/mina-arenden-ref-impl"))
                .license(new License().name("MIT")))
                .components(new Components()
                        .addSecuritySchemes(CLIENT_ID, header(CLIENT_ID, "ID för klienten (Skatteverkets API-definition). Krävs om CLIENT_ID är satt."))
                        .addSecuritySchemes(CLIENT_SECRET, header(CLIENT_SECRET, "Lösenord för klienten. Krävs om CLIENT_SECRET är satt."))
                        .addSecuritySchemes(API_KEY, header(API_KEY, "Nyckel för inläsning till cachen. Krävs om ADMIN_API_KEY är satt.")));
    }

    private static SecurityScheme header(String namn, String beskrivning) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER).name(namn).description(beskrivning);
    }
}
