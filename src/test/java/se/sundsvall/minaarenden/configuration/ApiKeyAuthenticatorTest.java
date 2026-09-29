package se.sundsvall.minaarenden.configuration;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthenticatorTest {

	private static final String FRAGA_1 = "f".repeat(40);
	private static final String FRAGA_2 = "g".repeat(40);
	private static final String ADMIN = "a".repeat(40);

	private final ApiKeyAuthenticator authenticator = new ApiKeyAuthenticator(new ApiKeyProperties("X-API-Key", List.of(FRAGA_1, FRAGA_2), List.of(ADMIN)));

	@Test
	void frageNyckelGerRollenFraga() {
		assertThat(authenticator.autentisera(FRAGA_2)).hasValueSatisfying(autentisering -> {
			assertThat(autentisering.isAuthenticated()).isTrue();
			assertThat(autentisering.getName()).isEqualTo("fraga-2");
			assertThat(autentisering.getCredentials()).isNull();
			assertThat(autentisering.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_FRAGA");
		});
	}

	@Test
	void adminNyckelGerBadaRollerna() {
		assertThat(authenticator.autentisera(ADMIN)).hasValueSatisfying(autentisering -> {
			assertThat(autentisering.getName()).isEqualTo("admin-1");
			assertThat(autentisering.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_FRAGA");
		});
	}

	@Test
	void nyckelSomFinnsIBadaListornaFarAdmin() {
		final var dubbel = new ApiKeyAuthenticator(new ApiKeyProperties("X-API-Key", List.of(ADMIN), List.of(ADMIN)));

		assertThat(dubbel.autentisera(ADMIN)).hasValueSatisfying(autentisering -> assertThat(autentisering.getAuthorities())
			.extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_FRAGA"));
	}

	@Test
	void felNyckelGerIngenAutentisering() {
		assertThat(authenticator.autentisera("x".repeat(40))).isEmpty();
		assertThat(authenticator.autentisera(FRAGA_1.substring(1))).isEmpty();
		assertThat(authenticator.autentisera("")).isEmpty();
		assertThat(authenticator.autentisera(null)).isEmpty();
	}
}
