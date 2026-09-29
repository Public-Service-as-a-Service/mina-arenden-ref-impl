package se.sundsvall.minaarenden.configuration;

import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/**
 * Säkerhetskritiska inställningar som måste gå före dept44:s standardvärden.
 *
 * <p>
 * dept44-starter levererar sina standardvärden i classpath:/config/application.properties, som Spring Boot läser med
 * högre prioritet än tjänstens egen application.yml. Där slås bland annat loggning av anropens innehåll på (med
 * headers, alltså API-nyckeln, och personnummer i klartext), alla actuator-endpoints exponeras och hälsodetaljer visas
 * för alla. Den här klassen lägger tjänstens värden ovanför alla konfigurationsfiler men under miljövariabler,
 * systemegenskaper och kommandoradsargument, så att de fortfarande kan ändras uttryckligen vid felsökning.
 */
public class SakerhetsinstallningarEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

	static final String PROPERTY_SOURCE_NAME = "minaArendenSakerhetsinstallningar";

	static final Map<String, Object> INSTALLNINGAR = Map.of(
		// Anropens innehåll (inklusive headern med API-nyckeln) loggas inte.
		"logging.level.se.sundsvall.dept44.payload", "OFF",
		// Bara hälsa, info och Flyway-status exponeras; övrigt kräver dessutom admin-nyckel (ApiKeySecurityConfiguration).
		"management.endpoints.web.exposure.include", "health,info,flyway",
		// Hälsodetaljer (databas, disk m.m.) bara med admin-nyckel.
		"management.endpoint.health.show-details", "when-authorized",
		"management.endpoint.health.show-components", "when-authorized",
		"management.endpoint.health.roles", Roller.ADMIN);

	@Override
	public void postProcessEnvironment(final ConfigurableEnvironment environment, final SpringApplication application) {
		final var propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, INSTALLNINGAR);
		final var sources = environment.getPropertySources();
		if (sources.contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
			sources.addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
		} else {
			sources.addFirst(propertySource);
		}
	}

	/**
	 * Efter att konfigurationsfilerna har lästs in.
	 */
	@Override
	public int getOrder() {
		return ConfigDataEnvironmentPostProcessor.ORDER + 1;
	}
}
