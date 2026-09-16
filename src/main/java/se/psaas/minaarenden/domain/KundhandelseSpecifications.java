package se.psaas.minaarenden.domain;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import se.psaas.minaarenden.api.dto.Arende;
import se.psaas.minaarenden.api.dto.Kund;

/** Databasfilter som motsvarar fälten i fraga. */
public final class KundhandelseSpecifications {

    private KundhandelseSpecifications() {}

    /** Parten matchar om både angiven kund och angivet ärende stämmer (spec: "svaret uppfyller båda kriterierna"). */
    public static Specification<KundhandelseEntity> forPart(Kund kund, Arende arende) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (kund != null) {
                p.add(cb.equal(root.get("kundIdentifierare"), kund.identifierare()));
                p.add(cb.equal(root.get("kundTyp"), kund.typ()));
                if (kund.tillagg() != null && !kund.tillagg().isBlank()) {
                    p.add(cb.equal(root.get("kundTillagg"), kund.tillagg()));
                }
            }
            if (arende != null) {
                p.add(cb.equal(root.get("arendeIdentifierare"), arende.identifierare()));
                p.add(cb.equal(root.get("arendeTyp"), arende.typ()));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
    }

    /** En kundhändelsetyp matchar filtret om den är lika med det eller ligger under det (prefix följt av punkt). */
    public static Specification<KundhandelseEntity> kundhandelseTyper(List<String> typer) {
        if (typer == null || typer.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> cb.or(typer.stream()
                .map(t -> cb.or(cb.equal(root.get("kundhandelseTyp"), t), cb.like(root.get("kundhandelseTyp"), escapeLike(t) + ".%", '!')))
                .toArray(Predicate[]::new));
    }

    public static Specification<KundhandelseEntity> taggar(List<String> taggar) {
        if (taggar == null || taggar.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<KundhandelseEntity, String> join = root.join("taggar");
            return join.in(taggar);
        };
    }

    public static Specification<KundhandelseEntity> tidpunktFrom(Instant from) {
        return from == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("tidpunkt"), from);
    }

    public static Specification<KundhandelseEntity> tidpunktTo(Instant to) {
        return to == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("tidpunkt"), to);
    }

    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
