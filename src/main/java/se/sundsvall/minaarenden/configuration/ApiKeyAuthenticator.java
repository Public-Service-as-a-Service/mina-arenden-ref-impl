package se.sundsvall.minaarenden.configuration;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Jämför en inskickad API-nyckel med de konfigurerade. Nycklarna hålls bara som SHA-256-hashar i minnet, och
 * jämförelsen görs i konstant tid mot samtliga nycklar så att svarstiden inte avslöjar hur stor del av en nyckel som
 * stämmer. Den som anropar identifieras med nyckelns position (t.ex. admin-1), aldrig med nyckeln.
 */
@Component
public class ApiKeyAuthenticator {

	private static final Logger LOG = LoggerFactory.getLogger(ApiKeyAuthenticator.class);

	private final List<Nyckel> nycklar;

	public ApiKeyAuthenticator(final ApiKeyProperties properties) {
		final var lista = new ArrayList<Nyckel>();
		for (var i = 0; i < properties.adminKeys().size(); i++) {
			lista.add(new Nyckel("admin-" + (i + 1), sha256(properties.adminKeys().get(i)), Set.of(Roller.ADMIN, Roller.FRAGA)));
		}
		for (var i = 0; i < properties.fragaKeys().size(); i++) {
			lista.add(new Nyckel("fraga-" + (i + 1), sha256(properties.fragaKeys().get(i)), Set.of(Roller.FRAGA)));
		}
		this.nycklar = List.copyOf(lista);
		LOG.info("API-nyckelskydd aktivt med {} admin-nyckel/nycklar och {} fråge-nyckel/nycklar", properties.adminKeys().size(), properties.fragaKeys().size());
	}

	/**
	 * @param  inskickad värdet i API-nyckelheadern
	 * @return           en autentisering med nyckelns roller om nyckeln är giltig
	 */
	public Optional<Authentication> autentisera(final String inskickad) {
		if (inskickad == null || inskickad.isEmpty()) {
			return Optional.empty();
		}
		final var hash = sha256(inskickad);
		String id = null;
		final var roller = new LinkedHashSet<String>();
		// Jämför mot alla nycklar utan att avbryta vid första träff.
		for (final var nyckel : nycklar) {
			if (MessageDigest.isEqual(nyckel.hash(), hash)) {
				id = id == null ? nyckel.id() : id;
				roller.addAll(nyckel.roller());
			}
		}
		if (id == null) {
			return Optional.empty();
		}
		final var authorities = roller.stream().map(roll -> new SimpleGrantedAuthority("ROLE_" + roll)).toList();
		return Optional.of(new PreAuthenticatedAuthenticationToken(id, null, authorities));
	}

	static byte[] sha256(final String varde) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(varde.getBytes(StandardCharsets.UTF_8));
		} catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 saknas i JVM:en", e);
		}
	}

	private record Nyckel(String id, byte[] hash, Set<String> roller) {
	}
}
