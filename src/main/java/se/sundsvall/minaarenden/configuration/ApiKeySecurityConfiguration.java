package se.sundsvall.minaarenden.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

/**
 * Alla anrop kräver API-nyckel utom dokumentationen (/, /api-docs, Swagger UI) och hälsokontrollen
 * (/actuator/health, /actuator/info).
 *
 * <ul>
 * <li>/{municipalityId}/kundhandelseFragaSynkron: fråge- eller admin-nyckel</li>
 * <li>/{municipalityId}/kundhandelser/**: admin-nyckel</li>
 * <li>allt annat (t.ex. övriga actuator-endpoints): admin-nyckel</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class ApiKeySecurityConfiguration {

	@Bean
	SecurityFilterChain apiKeyFilterChain(final HttpSecurity http, final ApiKeyAuthenticator authenticator, final ApiKeyProperties properties,
		final ApiKeyFailureHandler failureHandler) {

		return http
			.securityMatcher("/**")
			.csrf(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.logout(AbstractHttpConfigurer::disable)
			.requestCache(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
			.addFilterBefore(new ApiKeyAuthenticationFilter(authenticator, properties.headerName()), AnonymousAuthenticationFilter.class)
			.authorizeHttpRequests(authorize -> authorize
				.requestMatchers(GET, "/", "/api-docs", "/api-docs.yaml", "/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll()
				.requestMatchers(GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
				.requestMatchers("/*/kundhandelseFragaSynkron").hasAnyRole(Roller.FRAGA, Roller.ADMIN)
				.requestMatchers("/*/kundhandelser", "/*/kundhandelser/**").hasRole(Roller.ADMIN)
				.anyRequest().hasRole(Roller.ADMIN))
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(failureHandler)
				.accessDeniedHandler(failureHandler))
			.build();
	}
}
